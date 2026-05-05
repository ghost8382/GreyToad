import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked, HostListener } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DirectMessageService, UserService } from '../../core/services/api.service';

import { ChatWsService } from '../../core/services/chat-ws.service';
import { DmUnreadService } from '../../core/services/dm-unread.service';
import { User, DirectMessage } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss'],
  imports: [CommonModule, FormsModule]
})
export class MessagesComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('msgContainer') msgContainer!: ElementRef;
  @ViewChild('msgInput') msgInput!: ElementRef<HTMLTextAreaElement>;

  me: User | null = null;
  allUsers: User[] = [];
  selectedUser: User | null = null;

  private historyMap: Record<string, DirectMessage[]> = {};

  messages: DirectMessage[] = [];
  messageText = '';
  searchQuery = '';
  shouldScroll = false;
  wsReady = false;
  loadingConversation = false;
  unreadSnapshot: Record<string, number> = {};
  reactPickerOpenId: string | null = null;
  reactPickerPos: { top: number; left: number } | null = null;
  readonly QUICK_EMOJIS = ['👍','👎','❤️','😂','🔥','✅','🎉','😮','😢','👀'];

  mentionQuery: string | null = null;
  mentionIndex = 0;
  emojiPickerOpen = false;

  readonly EMOJIS = [
    '😀','😂','🥲','😍','🤔','😎','🥳','😅','🤣','😭',
    '👍','👎','👋','🙌','🔥','❤️','💯','✅','❌','⚡',
    '🎉','🎯','🚀','💡','🐛','⭐','🌟','💪','🤝','👀',
    '📋','📌','📎','🔗','💬','📢','🔔','⏰','📅','🗓️',
    '😤','😡','🥺','😴','🤯','🤫','🤭','🙃','😬','🫡',
    '🫂','👏','🤌','✌️','🫠','🤡','💀','🙏','🫶','🤞'
  ];

  private destroy$ = new Subject<void>();
  private dmSub?: Subscription;
  private wsStateSub?: Subscription;
  private optimisticMessageIds = new Set<string>();

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private directMessageService: DirectMessageService,
    private ws: ChatWsService,
    private route: ActivatedRoute,
    public dmUnread: DmUnreadService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    this.dmUnread.map$.subscribe(map => { this.unreadSnapshot = { ...map }; });

    this.auth.currentUser$.subscribe(u => {
      this.me = u;
      if (u) this.initWs();
    });

    this.directMessageService.getUnreadCounts().subscribe(counts => {
      this.dmUnread.seed(counts);
    });

    this.userService.getAll().subscribe(users => {
      this.allUsers = users;
      this.route.queryParams.subscribe(p => {
        if (p['userId']) {
          const u = users.find(x => x.id === p['userId']);
          if (u) this.openConversation(u);
        }
      });
    });

    this.ws.presenceUpdate$.subscribe(({ userId, isOnline }) => {
      const idx = this.allUsers.findIndex(u => u.id === userId);
      if (idx !== -1) {
        this.allUsers = [...this.allUsers];
        this.allUsers[idx] = { ...this.allUsers[idx], isOnline };
        if (this.selectedUser?.id === userId) {
          this.selectedUser = { ...this.selectedUser, isOnline };
        }
      }
    });
  }

  initWs() {
    this.ws.connect();

    this.wsStateSub?.unsubscribe();
    this.wsStateSub = this.ws.connected$.subscribe(connected => {
      const wasDisconnected = !this.wsReady && connected;
      this.wsReady = connected;

      if (connected) {
        this.ws.subscribeToDMs();
        // On reconnect: fetch missed messages for the active conversation
        if (wasDisconnected && this.selectedUser) {
          this.fetchHistory(this.selectedUser.id, false);
        }
      }
    });

    // Real-time incoming messages via WebSocket only — no polling
    this.dmSub = this.ws.dm$.subscribe((msg: DirectMessage) => {
      const otherId = msg.senderId === this.me?.id ? msg.receiverId : msg.senderId;
      this.historyMap[otherId] = this.mergeMessages(this.historyMap[otherId] ?? [], [msg]);

      if (this.selectedUser?.id === otherId) {
        this.messages = [...this.historyMap[otherId]];
        this.dmUnread.clear(otherId);
        this.shouldScroll = true;
        if (msg.senderId !== this.me?.id) {
          this.directMessageService.markConversationRead(msg.senderId).subscribe();
        }
      } else if (msg.senderId !== this.me?.id) {
        this.dmUnread.increment(otherId);
      }
    });
  }

  ngAfterViewChecked() {
    if (this.shouldScroll) { this.scrollToBottom(); this.shouldScroll = false; }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.dmSub?.unsubscribe();
    this.wsStateSub?.unsubscribe();
    this.ws.unsubscribeFromDMs();
  }

  get otherUsers() { return this.allUsers.filter(u => u.id !== this.me?.id); }

  get filteredUsers() {
    const q = this.searchQuery.toLowerCase();
    if (!q) return this.sortedUsers;
    return this.sortedUsers.filter(u =>
      u.username.toLowerCase().includes(q) || u.email.toLowerCase().includes(q)
    );
  }

  get sortedUsers() {
    return [...this.otherUsers].sort((a, b) => {
      const aLast = this.historyMap[a.id]?.at(-1)?.createdAt ?? '';
      const bLast = this.historyMap[b.id]?.at(-1)?.createdAt ?? '';
      return bLast.localeCompare(aLast);
    });
  }

  openConversation(user: User) {
    this.selectedUser = user;
    this.dmUnread.clear(user.id);
    this.messages = [...(this.historyMap[user.id] ?? [])];
    this.fetchHistory(user.id, true);
    this.directMessageService.markConversationRead(user.id).subscribe();
  }

  private fetchHistory(userId: string, showLoading: boolean) {
    if (showLoading) this.loadingConversation = true;

    this.directMessageService.getConversation(userId).subscribe({
      next: conversation => {
        this.historyMap[userId] = this.mergeMessages(this.historyMap[userId] ?? [], conversation);
        if (this.selectedUser?.id === userId) {
          this.messages = [...this.historyMap[userId]];
          this.dmUnread.clear(userId);
          if (showLoading) this.shouldScroll = true;
        }
        this.loadingConversation = false;
      },
      error: () => {
        this.loadingConversation = false;
      }
    });
  }

  send() {
    const text = this.messageText.trim();
    if (!text || !this.me || !this.selectedUser || !this.wsReady) return;

    const optimistic: DirectMessage = {
      id: `tmp-${crypto.randomUUID()}`,
      content: text,
      senderId: this.me.id,
      receiverId: this.selectedUser.id,
      createdAt: new Date().toISOString()
    };

    this.optimisticMessageIds.add(optimistic.id);
    this.historyMap[this.selectedUser.id] = this.mergeMessages(
      this.historyMap[this.selectedUser.id] ?? [],
      [optimistic]
    );
    this.messages = [...this.historyMap[this.selectedUser.id]];
    this.shouldScroll = true;

    this.ws.sendDirectMessage(this.selectedUser.id, text);
    this.messageText = '';
  }

  onKeydown(e: KeyboardEvent) {
    if (this.mentionQuery !== null && this.mentionUsers.length > 0) {
      if (e.key === 'ArrowDown')  { e.preventDefault(); this.mentionIndex = Math.min(this.mentionIndex + 1, this.mentionUsers.length - 1); return; }
      if (e.key === 'ArrowUp')    { e.preventDefault(); this.mentionIndex = Math.max(this.mentionIndex - 1, 0); return; }
      if (e.key === 'Tab' || e.key === 'Enter') { e.preventDefault(); this.insertMention(this.mentionUsers[this.mentionIndex]); return; }
      if (e.key === 'Escape') { this.mentionQuery = null; return; }
    }
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.send(); }
  }

  onInput(event: Event) {
    const el = event.target as HTMLTextAreaElement;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 160) + 'px';
    const before = el.value.slice(0, el.selectionStart ?? 0);
    const match = before.match(/@(\w*)$/);
    if (match) { this.mentionQuery = match[1]; this.mentionIndex = 0; }
    else        { this.mentionQuery = null; }
  }

  get mentionUsers(): User[] {
    if (this.mentionQuery === null) return [];
    const q = this.mentionQuery.toLowerCase();
    return this.allUsers.filter(u => u.id !== this.me?.id && u.username.toLowerCase().startsWith(q)).slice(0, 6);
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

  insertEmoji(emoji: string) {
    const el = this.msgInput.nativeElement;
    const start = el.selectionStart ?? this.messageText.length;
    const end   = el.selectionEnd   ?? start;
    this.messageText = this.messageText.slice(0, start) + emoji + this.messageText.slice(end);
    this.emojiPickerOpen = false;
    setTimeout(() => { el.focus(); el.setSelectionRange(start + emoji.length, start + emoji.length); }, 0);
  }

  toggleReaction(dmId: string, emoji: string) {
    this.directMessageService.toggleReaction(dmId, emoji).subscribe(reactions => {
      const msg = this.messages.find(m => m.id === dmId);
      if (msg) (msg as any).reactions = reactions;
      const histMsg = this.historyMap[this.selectedUser!.id]?.find(m => m.id === dmId);
      if (histMsg) (histMsg as any).reactions = reactions;
    });
    this.reactPickerOpenId = null;
  }

  openReactPicker(msgId: string, event: MouseEvent) {
    event.stopPropagation();
    if (this.reactPickerOpenId === msgId) {
      this.reactPickerOpenId = null;
      this.reactPickerPos = null;
      return;
    }
    const btn = event.currentTarget as HTMLElement;
    const rect = btn.getBoundingClientRect();
    const pickerH = 44;
    const pickerW = 314;
    const top = rect.top > pickerH + 12 ? rect.top - pickerH - 8 : rect.bottom + 8;
    const left = Math.max(8, rect.right - pickerW);
    this.reactPickerOpenId = msgId;
    this.reactPickerPos = { top, left };
  }

  @HostListener('document:click')
  closeOverlays() { this.emojiPickerOpen = false; this.mentionQuery = null; this.reactPickerOpenId = null; this.reactPickerPos = null; }

  renderContent(text: string): SafeHtml {
    let html = text
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    const sorted = [...this.allUsers].sort((a, b) => b.username.length - a.username.length);
    for (const u of sorted) {
      const safe = u.username.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      html = html.replace(new RegExp('@' + safe, 'g'),
        `<span class="mention-tag">@${u.username}</span>`);
    }
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  scrollToBottom() {
    try { this.msgContainer.nativeElement.scrollTop = this.msgContainer.nativeElement.scrollHeight; }
    catch {}
  }

  isOwn(msg: DirectMessage) { return msg.senderId === this.me?.id; }
  formatTime(d: string) { return new Date(d).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }

  formatDay(d: string) {
    const date = new Date(d);
    if (date.toDateString() === new Date().toDateString()) return 'Today';
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
  }

  shouldShowDate(idx: number): boolean {
    if (idx === 0) return true;
    return this.formatDay(this.messages[idx - 1].createdAt) !== this.formatDay(this.messages[idx].createdAt);
  }

  getInitials(name: string) { return name.slice(0, 2).toUpperCase(); }

  avatarClass(name?: string): string {
    if (!name) return 'av-0';
    const hash = Array.from(name).reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return 'av-' + (hash % 8);
  }
  getLastMessage(user: User): string { return this.historyMap[user.id]?.at(-1)?.content?.slice(0, 35) ?? ''; }
  hasHistory(user: User): boolean { return (this.historyMap[user.id]?.length ?? 0) > 0; }
  hasUnread(user: User): boolean { return (this.unreadSnapshot[user.id] ?? 0) > 0; }
  getUnreadCount(user: User): number { return this.unreadSnapshot[user.id] ?? 0; }

  getEffectiveStatus(user: User): string {
    if (!user.isOnline) return 'OFFLINE';
    return user.status || 'AVAILABLE';
  }

  getStatusColor(status?: string): string {
    const map: Record<string, string> = {
      AVAILABLE: '#4a9a6a', BREAK: '#e8a44a', DINNER: '#e87a4a',
      OUT_OF_OFFICE: '#aa4a4a', MEETING: '#5a8aaa', OFFLINE: '#5a5a5a'
    };
    return map[status || 'OFFLINE'] ?? '#5a5a5a';
  }

  getStatusLabel(status?: string): string {
    const map: Record<string, string> = {
      AVAILABLE: 'Available', BREAK: 'Break', DINNER: 'Dinner',
      OUT_OF_OFFICE: 'Out of office', MEETING: 'Meeting', OFFLINE: 'Offline'
    };
    return map[status || 'OFFLINE'] ?? 'Offline';
  }

  private mergeMessages(existing: DirectMessage[], incoming: DirectMessage[]) {
    const merged = [...existing];

    for (const message of incoming) {
      const existingIndex = merged.findIndex(m => m.id === message.id);
      if (existingIndex !== -1) {
        merged[existingIndex] = message;
        this.optimisticMessageIds.delete(message.id);
        continue;
      }

      const optimisticIndex = merged.findIndex(m =>
        this.optimisticMessageIds.has(m.id) &&
        !message.id.startsWith('tmp-') &&
        m.senderId === message.senderId &&
        m.receiverId === message.receiverId &&
        m.content === message.content &&
        Math.abs(new Date(m.createdAt).getTime() - new Date(message.createdAt).getTime()) < 30000
      );

      if (optimisticIndex !== -1) {
        this.optimisticMessageIds.delete(merged[optimisticIndex].id);
        merged[optimisticIndex] = message;
        continue;
      }

      merged.push(message);
    }

    return merged.sort((a, b) =>
      new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );
  }
}
