// Auth
export interface AuthResponse { token: string; }
export interface LoginRequest  { email: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; }

// User
export interface User { id: string; username: string; email: string; role: string; quote?: string; status?: string; isOnline?: boolean; jobTitle?: string; headAdmin?: boolean; }
export interface UpdateProfileRequest { quote?: string; status?: string; }
export const USER_STATUSES = [
  { value: 'AVAILABLE',     label: 'Available',    color: '#4a9a6a' },
  { value: 'BREAK',         label: 'Break',        color: '#e8a44a' },
  { value: 'DINNER',        label: 'Dinner',       color: '#e87a4a' },
  { value: 'OUT_OF_OFFICE', label: 'Out of office',color: '#aa4a4a' },
  { value: 'MEETING',       label: 'Meeting',      color: '#5a8aaa' },
];

// Project — top-level, created by admin only
export interface Project { id: string; name: string; }
export interface CreateProjectRequest { name: string; }

// Team — belongs to a project, created by admin only
export interface Team { id: string; name: string; ownerId: string; projectId?: string; }
export interface CreateTeamRequest { name: string; projectId?: string; }
export interface TeamMember { id: string; userId: string; teamId: string; role: string; }
export interface AddMemberRequest { userId: string; role: string; }

// Task
export interface Task {
  id: string; caseNumber?: number; title: string; description?: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  assigneeId?: string; assigneeName?: string; projectId: string; projectName?: string;
  teamNames?: string[]; deadline?: string; slaDeadline?: string; archived?: boolean;
  priority?: string; type?: string;
  acceptanceStatus?: 'PENDING' | 'ACCEPTED' | null;
  totalWorkedMinutes?: number;
  workingSessionActive?: boolean;
  workStartedAt?: string;
}
export interface CreateTaskRequest {
  title: string; description?: string;
  projectId: string; assigneeId?: string; status?: string; autoAssign?: boolean;
  priority?: string; type?: string;
}

// Comment
export interface Comment { id: string; content: string; authorId: string; authorName?: string; taskId: string; createdAt: string; }
export interface CreateCommentRequest { content: string; authorId: string; }

// Time tracking
export interface TimeEntry { id: string; taskId: string; userId: string; userName: string; minutes: number; description?: string; date: string; }
export interface CreateTimeEntryRequest { userId: string; minutes: number; description?: string; date: string; }

// Attachments
export interface Attachment { id: string; originalName: string; contentType: string; fileSize: number; uploaderName: string; createdAt: string; }

// Audit log
export interface AuditLogEntry { id: string; actorName: string; action: string; entityType: string; entityId: string; details: string; createdAt: string; }

// Channel
export type ChannelScope = 'TEAM' | 'PROJECT';
export interface Channel { id: string; name: string; teamId: string; projectId?: string; scope?: ChannelScope; }
export interface CreateChannelRequest { name: string; teamId: string; scope?: ChannelScope; }

// Message reaction
export interface MessageReaction { emoji: string; count: number; reactedByMe: boolean; reactors?: string[]; }

// Channel Message
export type MessageType = 'CHAT' | 'POST';
export interface Message { id: string; content: string; senderId: string; senderName?: string; channelId: string; parentId?: string; createdAt: string; reactions?: MessageReaction[]; replyCount?: number; type?: MessageType; resolved?: boolean; }

// Direct Message
export interface DirectMessage {
  id: string; content: string;
  senderId: string; receiverId: string; createdAt: string;
  reactions?: MessageReaction[];
}

// Task Notification
export interface TaskNotification {
  taskId: string;
  caseNumber?: number;
  taskTitle: string;
}

// Role Template
export interface RoleTemplate { id: string; name: string; permissionLevel: string; }
export interface CreateRoleTemplateRequest { name: string; permissionLevel: string; }

// Generic app notification (bell panel) — WS-pushed (no id) or DB-loaded (has id)
export interface AppNotification {
  id?: string;
  type: 'TASK_ASSIGNED' | 'DM' | 'CHANNEL_MESSAGE' | 'MENTION';
  title: string;
  body: string;
  projectId?: string;
  read?: boolean;
  createdAt?: string;
}
