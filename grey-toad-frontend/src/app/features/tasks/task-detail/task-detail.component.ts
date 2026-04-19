import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { ProjectService, TaskService, UserService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { Task, Comment, Project, User } from '../../../shared/models';

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
  loading = true;
  saving = false;

  commentForm = this.fb.group({ content: ['', Validators.required] });

  statuses = ['TODO', 'IN_PROGRESS', 'DONE'];

  editingDeadline = false;
  deadlineValue = '';

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    private projectService: ProjectService,
    private userService: UserService,
    private auth: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    forkJoin({
      users: this.userService.getAll().pipe(catchError(() => of([] as User[]))),
      comments: this.taskService.getComments(id).pipe(catchError(() => of([] as Comment[]))),
      task: this.resolveTask(id)
    }).subscribe({
      next: ({ users, comments, task }) => {
        this.users = users;
        this.comments = comments;
        this.task = task;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private resolveTask(taskId: string) {
    const navTask = window.history.state?.task as Task | undefined;
    if (navTask?.id === taskId) {
      return of(navTask);
    }

    return this.projectService.getAll().pipe(
      catchError(() => of([] as Project[])),
      switchMap(projects => {
        if (projects.length === 0) {
          return of([] as Task[][]);
        }

        return forkJoin(
          projects.map(project =>
            this.taskService.getByProject(project.id).pipe(catchError(() => of([] as Task[])))
          )
        );
      }),
      map(taskGroups => taskGroups.flat().find(task => task.id === taskId) ?? null),
      catchError(() => of(null))
    );
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
      next: c => {
        this.comments.push(c);
        this.commentForm.reset();
        this.saving = false;
      },
      error: () => { this.saving = false; }
    });
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
    return { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' }[s] || s;
  }

  formatDate(d?: string) {
    if (!d) return '';
    return new Date(d).toLocaleString();
  }
}
