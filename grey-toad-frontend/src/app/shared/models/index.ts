// Auth
export interface AuthResponse { token: string; }
export interface LoginRequest  { email: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; }

// User
export interface User { id: string; username: string; email: string; role: string; quote?: string; status?: string; }
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
}
export interface CreateTaskRequest {
  title: string; description?: string;
  projectId: string; assigneeId?: string; status?: string; autoAssign?: boolean;
  priority?: string; type?: string;
}

// Comment
export interface Comment { id: string; content: string; authorId: string; taskId: string; createdAt: string; }
export interface CreateCommentRequest { content: string; authorId: string; }

// Channel
export interface Channel { id: string; name: string; teamId: string; }
export interface CreateChannelRequest { name: string; teamId: string; }

// Channel Message
export interface Message { id: string; content: string; senderId: string; channelId: string; createdAt: string; }

// Direct Message
export interface DirectMessage {
  id: string; content: string;
  senderId: string; receiverId: string; createdAt: string;
}

// Task Notification
export interface TaskNotification {
  taskId: string;
  caseNumber?: number;
  taskTitle: string;
}

// Generic app notification (bell panel)
export interface AppNotification {
  type: 'TASK_ASSIGNED' | 'DM' | 'CHANNEL_MESSAGE';
  title: string;
  body: string;
}
