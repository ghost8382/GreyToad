import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  Team, CreateTeamRequest, TeamMember, AddMemberRequest,
  Project, CreateProjectRequest,
  Task, CreateTaskRequest,
  Channel, CreateChannelRequest,
  Message, MessageReaction,
  Comment, CreateCommentRequest,
  TimeEntry, CreateTimeEntryRequest,
  Attachment, AuditLogEntry,
  DirectMessage,
  User, UpdateProfileRequest
} from '../../shared/models';

const API = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class TeamService {
  constructor(private http: HttpClient) {}
  getAll()                        { return this.http.get<Team[]>(`${API}/teams`); }
  getById(id: string)             { return this.http.get<Team>(`${API}/teams/${id}`); }
  getMyTeams()                    { return this.http.get<Team[]>(`${API}/teams/my`); }
  getByProject(projectId: string) { return this.http.get<Team[]>(`${API}/teams/project/${projectId}`); }
  create(r: CreateTeamRequest)    { return this.http.post<Team>(`${API}/teams`, r); }
  delete(id: string)              { return this.http.delete(`${API}/teams/${id}`); }

  getMembers(teamId: string)                     { return this.http.get<TeamMember[]>(`${API}/teams/${teamId}/members`); }
  addMember(teamId: string, r: AddMemberRequest) { return this.http.post<TeamMember>(`${API}/teams/${teamId}/members`, r); }
  removeMember(teamId: string, memberId: string) { return this.http.delete(`${API}/teams/${teamId}/members/${memberId}`); }
}

@Injectable({ providedIn: 'root' })
export class ProjectService {
  constructor(private http: HttpClient) {}
  getAll()                        { return this.http.get<Project[]>(`${API}/projects`); }
  getById(id: string)             { return this.http.get<Project>(`${API}/projects/${id}`); }
  create(r: CreateProjectRequest) { return this.http.post<Project>(`${API}/projects`, r); }
  delete(id: string)              { return this.http.delete(`${API}/projects/${id}`); }
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  constructor(private http: HttpClient) {}
  getByProject(projectId: string, showArchived = false) {
    return this.http.get<Task[]>(`${API}/tasks/project/${projectId}?showArchived=${showArchived}`);
  }
  getById(id: string)                              { return this.http.get<Task>(`${API}/tasks/${id}`); }
  create(r: CreateTaskRequest)                     { return this.http.post<Task>(`${API}/tasks`, r); }
  changeStatus(id: string, status: string)         { return this.http.patch<Task>(`${API}/tasks/${id}/status?status=${status}`, {}); }
  assign(id: string, userId: string)               { return this.http.patch<Task>(`${API}/tasks/${id}/assign?userId=${userId}`, {}); }
  archive(id: string)                              { return this.http.patch<Task>(`${API}/tasks/${id}/archive`, {}); }
  setSla(id: string, slaDeadline: string)          { return this.http.patch<Task>(`${API}/tasks/${id}/sla?slaDeadline=${encodeURIComponent(slaDeadline)}`, {}); }
  setPriority(id: string, priority: string)        { return this.http.patch<Task>(`${API}/tasks/${id}/priority?priority=${priority}`, {}); }
  setType(id: string, type: string)                { return this.http.patch<Task>(`${API}/tasks/${id}/type?type=${type}`, {}); }
  setDeadline(id: string, deadline: string)        { return this.http.patch<Task>(`${API}/tasks/${id}/deadline?deadline=${encodeURIComponent(deadline)}`, {}); }
  accept(id: string)                               { return this.http.post<Task>(`${API}/tasks/${id}/accept`, {}); }
  reject(id: string)                               { return this.http.post<Task>(`${API}/tasks/${id}/reject`, {}); }

  getComments(taskId: string)                      { return this.http.get<Comment[]>(`${API}/tasks/${taskId}/comments`); }
  addComment(taskId: string, r: CreateCommentRequest) { return this.http.post<Comment>(`${API}/tasks/${taskId}/comments`, r); }

  getTimeEntries(taskId: string)                   { return this.http.get<TimeEntry[]>(`${API}/tasks/${taskId}/time-entries`); }
  logTime(taskId: string, r: CreateTimeEntryRequest) { return this.http.post<TimeEntry>(`${API}/tasks/${taskId}/time-entries`, r); }
  getTotalMinutes(taskId: string)                  { return this.http.get<number>(`${API}/tasks/${taskId}/time-entries/total`); }

  getAttachments(taskId: string)                   { return this.http.get<Attachment[]>(`${API}/tasks/${taskId}/attachments`); }
  uploadAttachment(taskId: string, file: File)     {
    const fd = new FormData(); fd.append('file', file);
    return this.http.post<Attachment>(`${API}/tasks/${taskId}/attachments`, fd);
  }
}

@Injectable({ providedIn: 'root' })
export class ChannelService {
  constructor(private http: HttpClient) {}
  getByTeam(teamId: string)       { return this.http.get<Channel[]>(`${API}/channels/team/${teamId}`); }
  getByProject(projectId: string) { return this.http.get<Channel[]>(`${API}/channels/project/${projectId}`); }
  create(r: CreateChannelRequest) { return this.http.post<Channel>(`${API}/channels`, r); }
  getMessages(channelId: string)  { return this.http.get<Message[]>(`${API}/channels/${channelId}/messages`); }
  sendMessage(channelId: string, content: string) {
    return this.http.post<Message>(`${API}/channels/${channelId}/messages`, { content });
  }
  getReplies(channelId: string, messageId: string) {
    return this.http.get<Message[]>(`${API}/channels/${channelId}/messages/${messageId}/replies`);
  }
  sendReply(channelId: string, messageId: string, content: string) {
    return this.http.post<Message>(`${API}/channels/${channelId}/messages/${messageId}/replies`, { content });
  }
  toggleReaction(messageId: string, emoji: string) {
    return this.http.post<MessageReaction[]>(`${API}/messages/${messageId}/reactions?emoji=${encodeURIComponent(emoji)}`, {});
  }
  getThreadStarters(channelId: string) {
    return this.http.get<Message[]>(`${API}/channels/${channelId}/messages/threads`);
  }
  getPosts(channelId: string) {
    return this.http.get<Message[]>(`${API}/channels/${channelId}/messages/posts`);
  }
  createPost(channelId: string, content: string) {
    return this.http.post<Message>(`${API}/channels/${channelId}/messages/posts`, { content });
  }
  deleteChannel(channelId: string) {
    return this.http.delete(`${API}/channels/${channelId}`);
  }
  resolvePost(channelId: string, messageId: string) {
    return this.http.patch<Message>(`${API}/channels/${channelId}/messages/posts/${messageId}/resolve`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}
  getAll()            { return this.http.get<User[]>(`${API}/users`); }
  getById(id: string) { return this.http.get<User>(`${API}/users/${id}`); }
  updateProfile(r: UpdateProfileRequest) { return this.http.patch<User>(`${API}/users/me`, r); }
  setRole(id: string, role: string) { return this.http.patch<User>(`${API}/users/${id}/role?role=${role}`, {}); }
  createUser(data: { username: string; email: string; password: string; role: string }) {
    return this.http.post<User>(`${API}/users`, data);
  }
  deleteUser(id: string) { return this.http.delete(`${API}/users/${id}`); }
}

@Injectable({ providedIn: 'root' })
export class DirectMessageService {
  constructor(private http: HttpClient) {}
  getConversation(otherUserId: string) {
    return this.http.get<DirectMessage[]>(`${API}/direct-messages/${otherUserId}`);
  }
  toggleReaction(dmId: string, emoji: string) {
    return this.http.post<MessageReaction[]>(`${API}/direct-messages/${dmId}/reactions?emoji=${encodeURIComponent(emoji)}`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  constructor(private http: HttpClient) {}
  getByProject(projectId: string) {
    return this.http.get<AuditLogEntry[]>(`${API}/audit-log/project/${projectId}`);
  }
}

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  getDownloadUrl(id: string): string { return `${API}/attachments/${id}/download`; }
}
