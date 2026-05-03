import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { catchError, forkJoin, of } from 'rxjs';
import { TaskService, UserService, AttachmentService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { Task, Comment, User, TimeEntry, Attachment } from '../../../shared/models';

@Component({
  standalone: true,
  selector: 'app-task-detail',
  templateUrl: './task-detail.component.html',
  styleUrls: ['./task-detail.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink]
})
export class TaskDetailComponent implements OnInit {
  task: Task | null = null;
  comments: Comment[] = [];
  users: User[] = [];
  timeEntries: TimeEntry[] = [];
  totalMinutes = 0;
  attachments: Attachment[] = [];

  loading = true;
  saving = false;
  savingTime = false;
  uploading = false;

  commentForm = this.fb.group({ content: ['', Validators.required] });
  timeForm = this.fb.group({
    minutes: [null as number | null, [Validators.required, Validators.min(1)]],
    description: ['']
  });

  statuses = ['TODO', 'IN_PROGRESS', 'DONE'];
  editingDeadline = false;
  deadlineValue = '';

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    private userService: UserService,
    private attachmentService: AttachmentService,
    private auth: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    const navTask = window.history.state?.task as Task | undefined;

    forkJoin({
      users:        this.userService.getAll().pipe(catchError(() => of([] as User[]))),
      comments:     this.taskService.getComments(id).pipe(catchError(() => of([] as Comment[]))),
      task:         navTask?.id === id ? of(navTask) : this.taskService.getById(id).pipe(catchError(() => of(null))),
      timeEntries:  this.taskService.getTimeEntries(id).pipe(catchError(() => of([] as TimeEntry[]))),
      totalMinutes: this.taskService.getTotalMinutes(id).pipe(catchError(() => of(0))),
      attachments:  this.taskService.getAttachments(id).pipe(catchError(() => of([] as Attachment[])))
    }).subscribe({
      next: ({ users, comments, task, timeEntries, totalMinutes, attachments }) => {
        this.users = users;
        this.comments = comments;
        this.task = task;
        this.timeEntries = timeEntries;
        this.totalMinutes = totalMinutes;
        this.attachments = attachments;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  changeStatus(status: string) {
    if (!this.task) return;
    this.taskService.changeStatus(this.task.id, status).subscribe(t => this.task = t);
  }

  assignTo(userId: string) {
    if (!this.task) return;
    this.taskService.assign(this.task.id, userId).subscribe(t => this.task = t);
  }

  submitComment() {
    if (this.commentForm.invalid || !this.task) return;
    const me = this.auth.currentUser$.value;
    if (!me) return;
    this.saving = true;
    this.taskService.addComment(this.task.id, {
      content: this.commentForm.value.content!,
      authorId: me.id
    }).subscribe({
      next: c => { this.comments.push(c); this.commentForm.reset(); this.saving = false; },
      error: () => { this.saving = false; }
    });
  }

  submitTimeEntry() {
    if (this.timeForm.invalid || !this.task) return;
    const me = this.auth.currentUser$.value;
    if (!me) return;
    this.savingTime = true;
    this.taskService.logTime(this.task.id, {
      userId: me.id,
      minutes: this.timeForm.value.minutes!,
      description: this.timeForm.value.description || undefined,
      date: new Date().toISOString().slice(0, 10)
    }).subscribe({
      next: entry => {
        this.timeEntries.push(entry);
        this.totalMinutes += entry.minutes;
        this.timeForm.reset();
        this.savingTime = false;
      },
      error: () => { this.savingTime = false; }
    });
  }

  onFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.task) return;
    this.uploading = true;
    this.taskService.uploadAttachment(this.task.id, file).subscribe({
      next: att => { this.attachments.push(att); this.uploading = false; input.value = ''; },
      error: () => { this.uploading = false; }
    });
  }

  downloadUrl(id: string): string {
    return this.attachmentService.getDownloadUrl(id);
  }

  formatMinutes(m: number): string {
    if (m < 60) return `${m}m`;
    const h = Math.floor(m / 60);
    const min = m % 60;
    return min > 0 ? `${h}h ${min}m` : `${h}h`;
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }

  changePriority(priority: string) {
    if (!this.task) return;
    this.taskService.setPriority(this.task.id, priority).subscribe(t => this.task = t);
  }

  changeType(type: string) {
    if (!this.task) return;
    this.taskService.setType(this.task.id, type).subscribe(t => this.task = t);
  }

  openDeadline() {
    this.deadlineValue = this.task?.deadline ? this.task.deadline.slice(0, 16) : '';
    this.editingDeadline = true;
  }

  saveDeadline() {
    if (!this.task || !this.deadlineValue) return;
    const iso = new Date(this.deadlineValue).toISOString().slice(0, 19);
    this.taskService.setDeadline(this.task.id, iso).subscribe(t => {
      this.task = t;
      this.editingDeadline = false;
    });
  }

  slaLabel(task: Task): string {
    if (!task.slaDeadline) return '';
    const diffH = (new Date(task.slaDeadline).getTime() - Date.now()) / 3600000;
    if (diffH < 0) return 'SLA exceeded';
    if (diffH < 1) return `SLA: ${Math.ceil(diffH * 60)}min`;
    if (diffH < 24) return `SLA: ${Math.ceil(diffH)}h`;
    return `SLA: ${new Date(task.slaDeadline).toLocaleDateString('pl-PL')}`;
  }

  slaClass(task: Task): string {
    if (!task.slaDeadline) return '';
    const diffH = (new Date(task.slaDeadline).getTime() - Date.now()) / 3600000;
    if (diffH < 0) return 'sla-overdue';
    if (diffH < 8) return 'sla-critical';
    if (diffH < 24) return 'sla-warning';
    return 'sla-ok';
  }

  slaBarPercent(task: Task): number {
    if (!task.slaDeadline) return 0;
    const diffH = (new Date(task.slaDeadline).getTime() - Date.now()) / 3600000;
    if (diffH <= 0) return 100;
    if (diffH >= 168) return 4;
    if (diffH >= 24) return Math.round(4 + (168 - diffH) / 144 * 60);
    return Math.round(64 + (24 - diffH) / 24 * 36);
  }

  priorityClass(p?: string) { return 'priority-' + (p || 'medium').toLowerCase(); }
  priorityLabel(p?: string) {
    return ({ CRITICAL: '!! Critical', HIGH: '↑ High', MEDIUM: '– Medium', LOW: '↓ Low' } as any)[p || 'MEDIUM'] || '–';
  }

  typeLabel(t?: string) {
    return ({ TASK: 'Task', BUG: 'Bug', FEATURE: 'Feature', STORY: 'Story' } as any)[t || 'TASK'] || t;
  }

  get isAdmin() { return this.auth.isAdmin; }
  get isAdminOrLeader() { return this.auth.isAdminOrLeader; }

  getUserName(userId?: string) {
    if (!userId) return 'Unassigned';
    return this.users.find(u => u.id === userId)?.username || userId.slice(0, 8);
  }

  statusLabel(s: string) {
    return ({ TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' } as any)[s] || s;
  }

  formatDate(d?: string) {
    if (!d) return '';
    return new Date(d).toLocaleString();
  }
}
