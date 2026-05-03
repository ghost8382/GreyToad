import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, HostListener } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, Subscription } from 'rxjs';
import { ChatWsService } from '../../core/services/chat-ws.service';
import { ChannelService, TeamService, UserService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ProjectContextService } from '../../core/services/project-context.service';
import { Channel, ChannelScope, Team, Message, User } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'],
  imports: [CommonModule, FormsModule, RouterLink]
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('msgList') msgList!: ElementRef;
  @ViewChild('msgInput') msgInput!: ElementRef<HTMLTextAreaElement>;
  @ViewChild('postInput') postInput!: ElementRef<HTMLTextAreaElement>;

  teams: Team[] = [];
  channels: Channel[] = [];
  messages: Message[] = [];
  users: User[] = [];
  me: User | null = null;
  wsConnected = false;

  // Sidebar mode
  sidebarMode: 'teams' | 'project' = 'teams';
  projectChannels: Channel[] = [];
  projectChannelsLoading = false;

  selectedTeam = '';
  selectedChannel: Channel | null = null;
  showChannelSidebar = true;
  messageText = '';
  loading = false;
  shouldScroll = false;

  mentionQuery: string | null = null;
  mentionIndex = 0;
  emojiPickerOpen = false;

  // Thread state
  threadMessage: Message | null = null;
  threadReplies: Message[] = [];
  threadInput = '';
  threadLoading = false;

  // Channel tabs
  activeTab: 'messages' | 'posts' = 'messages';

  // Posts (standalone thread posts, Teams/Slack style)
  posts: Message[] = [];
  postsLoading = false;
  postText = '';
  postEmojiPickerOpen = false;
  postMentionQuery: string | null = null;
  postMentionIndex = 0;

  // Reaction quick-pick
  reactPickerOpenId: string | null = null;
  readonly QUICK_EMOJIS = ['👍','👎','❤️','😂','🔥','✅','🎉','😮','😢','👀'];

  readonly EMOJIS = [
    '😀','😂','🥲','😍','🤔','😎','🥳','😅','🤣','😭',
    '👍','👎','👋','🙌','🔥','❤️','💯','✅','❌','⚡',
    '🎉','🎯','🚀','💡','🐛','⭐','🌟','💪','🤝','👀',
    '📋','📌','📎','🔗','💬','📢','🔔','⏰','📅','🗓️',
    '😤','😡','🥺','😴','🤯','🤫','🤭','🙃','😬','🫡',
    '🫂','👏','🤌','✌️','🫠','🤡','💀','🙏','🫶','🤞'
  ];

  // Channel creation modal
  showCreateModal = false;
  createName = '';
  createScope: ChannelScope = 'TEAM';
  createTeamId = '';
  creating = false;

  private destroy$ = new Subject<void>();
  private wsSub?: Subscription;
  private connSub?: Subscription;
  private optimisticIds = new Set<string>();

  constructor(
    private ws: ChatWsService,
    private channelService: ChannelService,
    private teamService: TeamService,
    private userService: UserService,
    private auth: AuthService,
    private route: ActivatedRoute,
    private projectContext: ProjectContextService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => this.me = u);
    this.userService.getAll().subscribe(users => this.users = users);
    this.teamService.getMyTeams().subscribe(t => {
      const projectId = this.projectContext.selected?.id;
      this.teams = projectId ? t.filter(team => team.projectId === projectId) : t;
      this.route.queryParams.subscribe(p => {
        const teamId = p['teamId'] || this.teams[0]?.id;
        if (teamId) this.selectTeam(teamId, p['channelId']);
      });
    });

    this.ws.connect();

    this.connSub = this.ws.connected$.subscribe(connected => {
      this.wsConnected = connected;
      if (connected && this.selectedChannel) {
        this.ws.subscribeToChannel(this.selectedChannel.id);
        this.fetchMissedMessages(this.selectedChannel.id);
      }
    });

    this.wsSub = this.ws.channelMessage$.subscribe(msg => {
      if (msg.channelId !== this.selectedChannel?.id) return;
      if (msg.type === 'POST') {
        this.posts = [msg, ...this.posts.filter(p => p.id !== msg.id)];
      } else {
        this.messages = this.mergeMessages(this.messages, [msg]);
        this.shouldScroll = true;
      }
    });
  }

  ngAfterViewChecked() {
    if (this.shouldScroll) { this.scrollToBottom(); this.shouldScroll = false; }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.wsSub?.unsubscribe();
    this.connSub?.unsubscribe();
    this.ws.unsubscribeFromChannel();
  }

  switchSidebarMode(mode: 'teams' | 'project') {
    this.sidebarMode = mode;
    this.selectedChannel = null;
    this.messages = [];
    this.posts = [];
    this.closeThread();

    if (mode === 'project') {
      const projectId = this.projectContext.selected?.id;
      if (projectId) {
        this.projectChannelsLoading = true;
        this.channelService.getByProject(projectId).subscribe({
          next: ch => { this.projectChannels = ch; this.projectChannelsLoading = false; },
          error: () => { this.projectChannelsLoading = false; }
        });
      }
    }
  }

  selectTeam(teamId: string, preselectChannelId?: string) {
    this.selectedTeam = teamId;
    this.selectedChannel = null;
    this.messages = [];
    this.posts = [];
    this.closeThread();
    this.channelService.getByTeam(teamId).subscribe(channels => {
      this.channels = channels.filter(c => c.scope !== 'PROJECT');
      if (preselectChannelId) {
        const ch = this.channels.find(c => c.id === preselectChannelId);
        if (ch) this.openChannel(ch);
      }
    });
  }

  switchTab(tab: 'messages' | 'posts') {
    this.activeTab = tab;
    if (tab === 'posts' && this.selectedChannel) {
      this.loadPosts(this.selectedChannel.id);
    }
  }

  loadPosts(channelId: string) {
    this.postsLoading = true;
    this.channelService.getPosts(channelId).subscribe({
      next: p => { this.posts = p; this.postsLoading = false; },
      error: () => { this.postsLoading = false; }
    });
  }

  openChannel(ch: Channel) {
    this.selectedChannel = ch;
    if (window.innerWidth <= 768) this.showChannelSidebar = false;
    this.messages = [];
    this.posts = [];
    this.optimisticIds.clear();
    this.closeThread();

    if (ch.scope === 'PROJECT') {
      this.activeTab = 'posts';
      this.loading = false;
      this.loadPosts(ch.id);
    } else {
      this.activeTab = 'messages';
      this.loading = true;
      this.channelService.getMessages(ch.id).subscribe({
        next: msgs => { this.messages = msgs; this.loading = false; this.shouldScroll = true; },
        error: () => { this.loading = false; }
      });
    }

    this.ws.subscribeToChannel(ch.id);
  }

  send() {
    const text = this.messageText.trim();
    if (!text || !this.selectedChannel || !this.me) return;

    const tmpId = `tmp-${crypto.randomUUID()}`;
    const optimistic: Message = {
      id: tmpId, content: text, senderId: this.me.id,
      channelId: this.selectedChannel.id, createdAt: new Date().toISOString(), type: 'CHAT'
    };
    this.optimisticIds.add(tmpId);
    this.messages = [...this.messages, optimistic];
    this.shouldScroll = true;

    this.ws.sendChannelMessage(this.selectedChannel.id, text);
    this.messageText = '';
  }

  submitPost() {
    const text = this.postText.trim();
    if (!text || !this.selectedChannel) return;
    this.channelService.createPost(this.selectedChannel.id, text).subscribe({
      next: post => {
        this.posts = [post, ...this.posts];
        this.postText = '';
      }
    });
  }

  // ---- Thread ----

  openThread(msg: Message) {
    if (this.threadMessage?.id === msg.id) { this.closeThread(); return; }
    this.threadMessage = msg;
    this.threadReplies = [];
    this.threadInput = '';
    this.threadLoading = true;
    this.channelService.getReplies(msg.channelId, msg.id).subscribe({
      next: replies => { this.threadReplies = replies; this.threadLoading = false; },
      error: () => { this.threadLoading = false; }
    });
  }

  closeThread() {
    this.threadMessage = null;
    this.threadReplies = [];
    this.threadInput = '';
  }

  sendReply() {
    const text = this.threadInput.trim();
    if (!text || !this.threadMessage) return;
    this.channelService.sendReply(this.threadMessage.channelId, this.threadMessage.id, text).subscribe({
      next: reply => {
        this.threadReplies.push(reply);
        this.threadInput = '';
      }
    });
  }

  // ---- Channel creation ----

  deleteChannel(ch: Channel, event: MouseEvent) {
    event.stopPropagation();
    if (!confirm(`Delete channel #${ch.name}? This cannot be undone.`)) return;
    this.channelService.deleteChannel(ch.id).subscribe(() => {
      this.channels = this.channels.filter(c => c.id !== ch.id);
      this.projectChannels = this.projectChannels.filter(c => c.id !== ch.id);
      if (this.selectedChannel?.id === ch.id) {
        this.selectedChannel = null;
        this.messages = [];
        this.posts = [];
        this.closeThread();
      }
    });
  }

  openCreateModal() {
    this.showCreateModal = true;
    this.createName = '';
    this.createScope = 'TEAM';
    this.createTeamId = this.selectedTeam || (this.teams[0]?.id ?? '');
  }

  closeCreateModal() { this.showCreateModal = false; }

  createChannel() {
    if (!this.createName.trim() || !this.createTeamId) return;
    this.creating = true;
    this.channelService.create({ name: this.createName.trim(), teamId: this.createTeamId, scope: this.createScope }).subscribe({
      next: ch => {
        this.creating = false;
        this.showCreateModal = false;
        if (this.createScope === 'TEAM') {
          if (ch.teamId === this.selectedTeam) this.channels = [...this.channels, ch];
        } else {
          this.projectChannels = [...this.projectChannels, ch];
        }
        this.openChannel(ch as Channel);
      },
      error: () => { this.creating = false; }
    });
  }

  // ---- Reactions ----

  toggleReaction(msgId: string, emoji: string) {
    this.channelService.toggleReaction(msgId, emoji).subscribe(reactions => {
      const msg = this.messages.find(m => m.id === msgId);
      if (msg) msg.reactions = reactions;
      const post = this.posts.find(p => p.id === msgId);
      if (post) post.reactions = reactions;
      if (this.threadMessage?.id === msgId) this.threadMessage.reactions = reactions;
    });
    this.reactPickerOpenId = null;
  }

  openReactPicker(msgId: string, event: MouseEvent) {
    event.stopPropagation();
    this.reactPickerOpenId = this.reactPickerOpenId === msgId ? null : msgId;
  }

  // ---- Input ----

  onKeydown(e: KeyboardEvent) {
    if (this.mentionQuery !== null && this.mentionUsers.length > 0) {
      if (e.key === 'ArrowDown')  { e.preventDefault(); this.mentionIndex = Math.min(this.mentionIndex + 1, this.mentionUsers.length - 1); return; }
      if (e.key === 'ArrowUp')    { e.preventDefault(); this.mentionIndex = Math.max(this.mentionIndex - 1, 0); return; }
      if (e.key === 'Tab' || (e.key === 'Enter' && this.mentionUsers.length > 0)) {
        e.preventDefault(); this.insertMention(this.mentionUsers[this.mentionIndex]); return;
      }
      if (e.key === 'Escape') { this.mentionQuery = null; return; }
    }
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.send(); }
  }

  onPostKeydown(e: KeyboardEvent) {
    if (this.postMentionQuery !== null && this.postMentionUsers.length > 0) {
      if (e.key === 'ArrowDown')  { e.preventDefault(); this.postMentionIndex = Math.min(this.postMentionIndex + 1, this.postMentionUsers.length - 1); return; }
      if (e.key === 'ArrowUp')    { e.preventDefault(); this.postMentionIndex = Math.max(this.postMentionIndex - 1, 0); return; }
      if (e.key === 'Tab' || (e.key === 'Enter' && this.postMentionUsers.length > 0)) {
        e.preventDefault(); this.insertPostMention(this.postMentionUsers[this.postMentionIndex]); return;
      }
      if (e.key === 'Escape') { this.postMentionQuery = null; return; }
    }
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.submitPost(); }
  }

  onInput(event: Event) {
    this.autoResize(event);
    const el = event.target as HTMLTextAreaElement;
    const before = el.value.slice(0, el.selectionStart ?? 0);
    const match = before.match(/@(\w*)$/);
    if (match) { this.mentionQuery = match[1]; this.mentionIndex = 0; }
    else        { this.mentionQuery = null; }
  }

  onPostInput(event: Event) {
    this.autoResize(event);
    const el = event.target as HTMLTextAreaElement;
    const before = el.value.slice(0, el.selectionStart ?? 0);
    const match = before.match(/@(\w*)$/);
    if (match) { this.postMentionQuery = match[1]; this.postMentionIndex = 0; }
    else        { this.postMentionQuery = null; }
  }

  autoResize(event: Event) {
    const el = event.target as HTMLTextAreaElement;
    el.style.height = 'auto';
    el.style.height = el.scrollHeight + 'px';
  }

  get mentionUsers(): User[] {
    if (this.mentionQuery === null) return [];
    const q = this.mentionQuery.toLowerCase();
    return this.users.filter(u => u.username.toLowerCase().startsWith(q)).slice(0, 6);
  }

  get postMentionUsers(): User[] {
    if (this.postMentionQuery === null) return [];
    const q = this.postMentionQuery.toLowerCase();
    return this.users.filter(u => u.username.toLowerCase().startsWith(q)).slice(0, 6);
  }

  insertMention(user: User) {
    const el = this.msgInput.nativeElement;
    const cursor = el.selectionStart ?? 0;
    const before = this.messageText.slice(0, cursor);
    const after  = this.messageText.slice(cursor);
    const match  = before.match(/@(\w*)$/);
    if (!match) return;
    const replaced = before.slice(0, before.length - match[0].length) + `@${user.username} `;
    this.messageText = replaced + after;
    this.mentionQuery = null;
    setTimeout(() => { el.focus(); el.setSelectionRange(replaced.length, replaced.length); }, 0);
  }

  insertPostMention(user: User) {
    const el = this.postInput?.nativeElement;
    if (!el) return;
    const cursor = el.selectionStart ?? 0;
    const before = this.postText.slice(0, cursor);
    const after  = this.postText.slice(cursor);
    const match  = before.match(/@(\w*)$/);
    if (!match) return;
    const replaced = before.slice(0, before.length - match[0].length) + `@${user.username} `;
    this.postText = replaced + after;
    this.postMentionQuery = null;
    setTimeout(() => { el.focus(); el.setSelectionRange(replaced.length, replaced.length); }, 0);
  }

  insertEmoji(emoji: string) {
    const el = this.msgInput.nativeElement;
    const start = el.selectionStart ?? this.messageText.length;
    const end   = el.selectionEnd   ?? start;
    this.messageText = this.messageText.slice(0, start) + emoji + this.messageText.slice(end);
    this.emojiPickerOpen = false;
    setTimeout(() => { el.focus(); el.setSelectionRange(start + emoji.length, start + emoji.length); }, 0);
  }

  insertPostEmoji(emoji: string) {
    const el = this.postInput?.nativeElement;
    if (!el) return;
    const start = el.selectionStart ?? this.postText.length;
    const end   = el.selectionEnd   ?? start;
    this.postText = this.postText.slice(0, start) + emoji + this.postText.slice(end);
    this.postEmojiPickerOpen = false;
    setTimeout(() => { el.focus(); el.setSelectionRange(start + emoji.length, start + emoji.length); }, 0);
  }

  get isAdmin() { return this.me?.role === 'ADMIN'; }
  get currentProjectId() { return this.projectContext.selected?.id; }

  @HostListener('document:click')
  closeOverlays() {
    this.emojiPickerOpen = false;
    this.postEmojiPickerOpen = false;
    this.mentionQuery = null;
    this.postMentionQuery = null;
    this.reactPickerOpenId = null;
  }

  renderContent(text: string): SafeHtml {
    let html = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const sorted = [...this.users].sort((a, b) => b.username.length - a.username.length);
    for (const u of sorted) {
      const safe = u.username.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      html = html.replace(new RegExp('@' + safe, 'g'),
        `<span class="mention-tag">@${u.username}</span>`);
    }
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private fetchMissedMessages(channelId: string) {
    this.channelService.getMessages(channelId).subscribe({
      next: msgs => {
        const before = this.messages.length;
        this.messages = this.mergeMessages(this.messages, msgs);
        if (this.messages.length > before) this.shouldScroll = true;
      }
    });
  }

  scrollToBottom() {
    try { this.msgList.nativeElement.scrollTop = this.msgList.nativeElement.scrollHeight; } catch {}
  }

  isOwnMessage(msg: Message) { return msg.senderId === this.me?.id; }
  formatTime(d: string) { return new Date(d).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }
  formatDay(d: string) { return new Date(d).toLocaleDateString([], { month: 'short', day: 'numeric' }); }

  shouldShowDate(idx: number): boolean {
    if (idx === 0) return true;
    return this.formatDay(this.messages[idx - 1].createdAt) !== this.formatDay(this.messages[idx].createdAt);
  }

  getUserName(senderId: string) {
    if (senderId === this.me?.id) return this.me?.username || 'You';
    return this.users.find(u => u.id === senderId)?.username || senderId.slice(0, 8);
  }

  getUserEmail(senderId: string): string {
    return this.users.find(u => u.id === senderId)?.email || '';
  }

  getUserStatusColor(senderId: string): string {
    const status = this.users.find(u => u.id === senderId)?.status;
    const colors: Record<string, string> = {
      AVAILABLE: '#4a9a6a', BREAK: '#e8a44a', DINNER: '#e87a4a',
      OUT_OF_OFFICE: '#aa4a4a', MEETING: '#5a8aaa', OFFLINE: '#5a5a5a'
    };
    return colors[status || ''] || '#5a5a5a';
  }

  getUserStatusLabel(senderId: string): string {
    const status = this.users.find(u => u.id === senderId)?.status;
    const labels: Record<string, string> = {
      AVAILABLE: 'Dostępny', BREAK: 'Przerwa', DINNER: 'Obiad',
      OUT_OF_OFFICE: 'Poza biurem', MEETING: 'Spotkanie', OFFLINE: 'Offline'
    };
    return labels[status || ''] || 'Offline';
  }

  private mergeMessages(existing: Message[], incoming: Message[]): Message[] {
    const merged = [...existing];

    for (const msg of incoming) {
      const idx = merged.findIndex(m => m.id === msg.id);
      if (idx !== -1) { merged[idx] = msg; continue; }

      if (!msg.id.startsWith('tmp-')) {
        const optIdx = merged.findIndex(m =>
          this.optimisticIds.has(m.id) &&
          m.senderId === msg.senderId &&
          m.channelId === msg.channelId &&
          m.content === msg.content &&
          Math.abs(new Date(m.createdAt).getTime() - new Date(msg.createdAt).getTime()) < 30000
        );
        if (optIdx !== -1) {
          this.optimisticIds.delete(merged[optIdx].id);
          merged[optIdx] = msg;
          continue;
        }
      }

      merged.push(msg);
    }

    return merged.sort((a, b) =>
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );
  }
}
