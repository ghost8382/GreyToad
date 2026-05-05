import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import * as SockJS from 'sockjs-client';
import { BehaviorSubject, Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Message, DirectMessage, AppNotification, Task, Comment } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class ChatWsService {
  private client!: Client;
  private connecting = false;
  private connectedSubject = new BehaviorSubject<boolean>(false);

  private channelMsgSubject = new Subject<Message>();
  channelMessage$ = this.channelMsgSubject.asObservable();

  private dmSubject = new Subject<DirectMessage>();
  dm$ = this.dmSubject.asObservable();
  connected$ = this.connectedSubject.asObservable();

  private notificationSubject = new Subject<AppNotification>();
  notification$ = this.notificationSubject.asObservable();
  private notificationSub?: { unsubscribe: () => void };

  private taskUpdateSubject = new Subject<Task>();
  taskUpdate$ = this.taskUpdateSubject.asObservable();
  private activeTaskProjectId?: string;
  private stompTaskSub?: { unsubscribe: () => void };

  private commentSubject = new Subject<Comment>();
  commentUpdate$ = this.commentSubject.asObservable();
  private activeCommentTaskId?: string;
  private stompCommentSub?: { unsubscribe: () => void };

  private presenceSubject = new Subject<{ userId: string; isOnline: boolean }>();
  presenceUpdate$ = this.presenceSubject.asObservable();
  private stompPresenceSub?: { unsubscribe: () => void };

  // Active subscriptions — restored automatically on reconnect
  private activeChannelId?: string;
  private dmActive = false;
  private stompChannelSub?: { unsubscribe: () => void };
  private stompDmSub?: { unsubscribe: () => void };

  connect() {
    if (this.client?.connected || this.connecting) return;

    const token = localStorage.getItem('gt_token');
    this.connecting = true;
    this.client = new Client({
      webSocketFactory: () => new (SockJS as any)(environment.wsUrl),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        this.connecting = false;
        this.connectedSubject.next(true);
        this.resubscribeAll();
      },
      onStompError: () => {
        this.connecting = false;
        this.connectedSubject.next(false);
      },
      onWebSocketClose: () => {
        this.connecting = false;
        this.connectedSubject.next(false);
      }
    });
    this.client.activate();
  }

  private resubscribeAll() {
    // Always subscribe to unified notifications (task, DM, channel) — active regardless of tab
    this.notificationSub?.unsubscribe();
    this.notificationSub = this.client.subscribe('/user/queue/notifications', (msg: IMessage) => {
      this.notificationSubject.next(JSON.parse(msg.body) as AppNotification);
    });
    // Always subscribe to DM payload for chat view — active regardless of tab
    this.stompDmSub?.unsubscribe();
    this.stompDmSub = this.client.subscribe('/user/queue/dm', (msg: IMessage) => {
      this.dmSubject.next(JSON.parse(msg.body) as DirectMessage);
    });
    if (this.activeChannelId) {
      this.stompChannelSub?.unsubscribe();
      this.stompChannelSub = this.client.subscribe(`/topic/channel/${this.activeChannelId}`, (msg: IMessage) => {
        this.channelMsgSubject.next(JSON.parse(msg.body) as Message);
      });
    }
    if (this.activeTaskProjectId) {
      this.stompTaskSub?.unsubscribe();
      this.stompTaskSub = this.client.subscribe(`/topic/tasks/${this.activeTaskProjectId}`, (msg: IMessage) => {
        this.taskUpdateSubject.next(JSON.parse(msg.body) as Task);
      });
    }
    if (this.activeCommentTaskId) {
      this.stompCommentSub?.unsubscribe();
      this.stompCommentSub = this.client.subscribe(`/topic/task/${this.activeCommentTaskId}/comments`, (msg: IMessage) => {
        this.commentSubject.next(JSON.parse(msg.body) as Comment);
      });
    }
    this.stompPresenceSub?.unsubscribe();
    this.stompPresenceSub = this.client.subscribe('/topic/presence', (msg: IMessage) => {
      this.presenceSubject.next(JSON.parse(msg.body) as { userId: string; isOnline: boolean });
    });
  }

  subscribeToChannel(channelId: string) {
    this.activeChannelId = channelId;
    if (!this.client?.connected) return;
    this.stompChannelSub?.unsubscribe();
    this.stompChannelSub = this.client.subscribe(`/topic/channel/${channelId}`, (msg: IMessage) => {
      this.channelMsgSubject.next(JSON.parse(msg.body) as Message);
    });
  }

  unsubscribeFromChannel() {
    this.activeChannelId = undefined;
    this.stompChannelSub?.unsubscribe();
    this.stompChannelSub = undefined;
  }

  subscribeToDMs() {
    this.dmActive = true;
    if (!this.client?.connected) return;
    this.stompDmSub?.unsubscribe();
    this.stompDmSub = this.client.subscribe('/user/queue/dm', (msg: IMessage) => {
      this.dmSubject.next(JSON.parse(msg.body) as DirectMessage);
    });
  }

  unsubscribeFromDMs() {
    this.dmActive = false;
    this.stompDmSub?.unsubscribe();
    this.stompDmSub = undefined;
  }

  subscribeToTaskUpdates(projectId: string) {
    this.activeTaskProjectId = projectId;
    if (!this.client?.connected) return;
    this.stompTaskSub?.unsubscribe();
    this.stompTaskSub = this.client.subscribe(`/topic/tasks/${projectId}`, (msg: IMessage) => {
      this.taskUpdateSubject.next(JSON.parse(msg.body) as Task);
    });
  }

  unsubscribeFromTaskUpdates() {
    this.activeTaskProjectId = undefined;
    this.stompTaskSub?.unsubscribe();
    this.stompTaskSub = undefined;
  }

  subscribeToTaskComments(taskId: string) {
    this.activeCommentTaskId = taskId;
    if (!this.client?.connected) return;
    this.stompCommentSub?.unsubscribe();
    this.stompCommentSub = this.client.subscribe(`/topic/task/${taskId}/comments`, (msg: IMessage) => {
      this.commentSubject.next(JSON.parse(msg.body) as Comment);
    });
  }

  unsubscribeFromTaskComments() {
    this.activeCommentTaskId = undefined;
    this.stompCommentSub?.unsubscribe();
    this.stompCommentSub = undefined;
  }

  sendChannelMessage(channelId: string, content: string) {
    this.client.publish({ destination: `/app/chat.send/${channelId}`, body: JSON.stringify({ content }) });
  }

  sendDirectMessage(receiverId: string, content: string) {
    this.client.publish({ destination: `/app/dm.send`, body: JSON.stringify({ content, receiverId }) });
  }

  release() {
    // Keep WS alive for app lifetime — just clean up active subscriptions if needed
  }

  get isConnected() { return this.client?.connected ?? false; }
}
