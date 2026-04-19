import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, Subscription } from 'rxjs';
import { ChatWsService } from '../../core/services/chat-ws.service';
import { ChannelService, TeamService, UserService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { Channel, Team, Message, User } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'],
  imports: [CommonModule, FormsModule, RouterLink]
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('msgList') msgList!: ElementRef;

  teams: Team[] = [];
  channels: Channel[] = [];
  messages: Message[] = [];
  users: User[] = [];
  me: User | null = null;
  wsConnected = false;

  selectedTeam = '';
  selectedChannel: Channel | null = null;
  showChannelSidebar = true;
  messageText = '';
  loading = false;
  shouldScroll = false;

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
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => this.me = u);
    this.userService.getAll().subscribe(users => this.users = users);
    this.teamService.getMyTeams().subscribe(t => {
      this.teams = t;
      this.route.queryParams.subscribe(p => {
        const teamId = p['teamId'] || t[0]?.id;
        if (teamId) this.selectTeam(teamId, p['channelId']);
      });
    });

    this.ws.connect();

    // On WS reconnect: re-subscribe and fetch any messages missed during disconnect
    this.connSub = this.ws.connected$.subscribe(connected => {
      this.wsConnected = connected;
      if (connected && this.selectedChannel) {
        this.ws.subscribeToChannel(this.selectedChannel.id);
        this.fetchMissedMessages(this.selectedChannel.id);
      }
    });

    // Real-time incoming messages via WebSocket only
    this.wsSub = this.ws.channelMessage$.subscribe(msg => {
      if (msg.channelId !== this.selectedChannel?.id) return;
      this.messages = this.mergeMessages(this.messages, [msg]);
      this.shouldScroll = true;
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

  selectTeam(teamId: string, preselectChannelId?: string) {
    this.selectedTeam = teamId;
    this.selectedChannel = null;
    this.messages = [];
    this.channelService.getByTeam(teamId).subscribe(channels => {
      this.channels = channels;
      if (preselectChannelId) {
        const ch = channels.find(c => c.id === preselectChannelId);
        if (ch) this.openChannel(ch);
      }
    });
  }

  openChannel(ch: Channel) {
    this.selectedChannel = ch;
    if (window.innerWidth <= 768) this.showChannelSidebar = false;
    this.messages = [];
    this.optimisticIds.clear();
    this.loading = true;

    // One-time HTTP load of history, then WebSocket takes over
    this.channelService.getMessages(ch.id).subscribe({
      next: msgs => { this.messages = msgs; this.loading = false; this.shouldScroll = true; },
      error: () => { this.loading = false; }
    });

    this.ws.subscribeToChannel(ch.id);
  }

  send() {
    const text = this.messageText.trim();
    if (!text || !this.selectedChannel || !this.me) return;

    // Optimistic update — appears instantly, replaced by server message via WS
    const tmpId = `tmp-${crypto.randomUUID()}`;
    const optimistic: Message = {
      id: tmpId,
      content: text,
      senderId: this.me.id,
      channelId: this.selectedChannel.id,
      createdAt: new Date().toISOString()
    };
    this.optimisticIds.add(tmpId);
    this.messages = [...this.messages, optimistic];
    this.shouldScroll = true;

    this.ws.sendChannelMessage(this.selectedChannel.id, text);
    this.messageText = '';
  }

  onKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.send(); }
  }

  autoResize(event: Event) {
    const el = event.target as HTMLTextAreaElement;
    el.style.height = 'auto';
    el.style.height = el.scrollHeight + 'px';
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

      // Match and replace optimistic message
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
