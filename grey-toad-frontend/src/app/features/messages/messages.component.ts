import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subject, Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { DirectMessageService, UserService } from '../../core/services/api.service';
import { ChatWsService } from '../../core/services/chat-ws.service';
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

  me: User | null = null;
  allUsers: User[] = [];
  selectedUser: User | null = null;

  private historyMap: Record<string, DirectMessage[]> = {};
  private unreadMap: Record<string, number> = {};

  messages: DirectMessage[] = [];
  messageText = '';
  searchQuery = '';
  shouldScroll = false;
  wsReady = false;
  loadingConversation = false;

  private destroy$ = new Subject<void>();
  private dmSub?: Subscription;
  private wsStateSub?: Subscription;
  private optimisticMessageIds = new Set<string>();

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private directMessageService: DirectMessageService,
    private ws: ChatWsService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => {
      this.me = u;
      if (u) this.initWs();
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
        this.unreadMap[otherId] = 0;
        this.shouldScroll = true;
      } else if (msg.senderId !== this.me?.id) {
        this.unreadMap[otherId] = (this.unreadMap[otherId] ?? 0) + 1;
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
    this.unreadMap[user.id] = 0;
    this.messages = [...(this.historyMap[user.id] ?? [])];
    // One-time HTTP load of history, then WebSocket takes over
    this.fetchHistory(user.id, true);
  }

  private fetchHistory(userId: string, showLoading: boolean) {
    if (showLoading) this.loadingConversation = true;

    this.directMessageService.getConversation(userId).subscribe({
      next: conversation => {
        this.historyMap[userId] = this.mergeMessages(this.historyMap[userId] ?? [], conversation);
        if (this.selectedUser?.id === userId) {
          this.messages = [...this.historyMap[userId]];
          this.unreadMap[userId] = 0;
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
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); this.send(); }
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
  hasUnread(user: User): boolean { return (this.unreadMap[user.id] ?? 0) > 0; }

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
