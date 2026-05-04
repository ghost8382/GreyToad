import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { TaskService, UserService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectContextService } from '../../../core/services/project-context.service';
import { ChatWsService } from '../../../core/services/chat-ws.service';
import { Task, Project, User } from '../../../shared/models';

@Component({
  standalone: true,
  selector: 'app-tasks-list',
  templateUrl: './tasks-list.component.html',
  styleUrls: ['./tasks-list.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink]
})
export class TasksListComponent implements OnInit, OnDestroy {
  tasks: Task[] = [];
  projects: Project[] = [];
  users: User[] = [];

  private wsSub?: Subscription;

  selectedProject = '';
  filterStatus = 'ALL';
  filterPriority = 'ALL';
  filterAssignedToMe = false;
  showArchived = false;
  caseSearch: number | null = null;
  loading = false;
  showForm = false;
  saving = false;
  autoAssign = false;
  viewMode: 'list' | 'kanban' = 'list';
  me: any = null;

  slaTaskId: string | null = null;
  slaValue = '';

  readonly PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  readonly TYPES = ['TASK', 'BUG', 'FEATURE', 'STORY'];

  form = this.fb.group({
    title:       ['', Validators.required],
    description: [''],
    projectId:   ['', Validators.required],
    assigneeId:  [''],
    status:      ['TODO'],
    priority:    ['MEDIUM'],
    type:        ['TASK']
  });

  constructor(
    private taskService: TaskService,
    private userService: UserService,
    private auth: AuthService,
    private ws: ChatWsService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private projectContext: ProjectContextService
  ) {}

  ngOnDestroy() {
    this.wsSub?.unsubscribe();
    this.ws.unsubscribeFromTaskUpdates();
  }

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => {
      this.me = u;
      if (u && u.role !== 'ADMIN' && u.role !== 'LEADER') {
        this.filterAssignedToMe = true;
      }
    });
    this.ws.connect();

    // Update project selector list
    this.projectContext.projects$.subscribe(projects => { this.projects = projects; });

    // Load tasks from URL param (deep-link)
    this.route.queryParams.subscribe(p => {
      if (p['projectId']) this.onProjectChange(p['projectId']);
    });

    // Load tasks when project context resolves or user switches project
    this.projectContext.selected$.subscribe(selected => {
      if (selected?.id && selected.id !== this.selectedProject) {
        this.onProjectChange(selected.id);
      }
    });

    this.userService.getAll().subscribe(u => this.users = u);
  }

  onProjectChange(projectId: string) {
    this.selectedProject = projectId;
    if (!projectId) return;
    this.form.patchValue({ projectId });
    this.loadTasks();
    this.subscribeToTaskWs(projectId);
  }

  private subscribeToTaskWs(projectId: string) {
    this.wsSub?.unsubscribe();
    this.ws.subscribeToTaskUpdates(projectId);
    this.wsSub = this.ws.taskUpdate$.subscribe(updated => {
      const idx = this.tasks.findIndex(t => t.id === updated.id);
      if (idx !== -1) {
        if (!this.showArchived && updated.archived) {
          this.tasks = this.tasks.filter(t => t.id !== updated.id);
        } else {
          this.tasks[idx] = updated;
          this.tasks = [...this.tasks];
        }
      } else if (!updated.archived || this.showArchived) {
        this.tasks = [...this.tasks, updated].sort((a, b) => (a.caseNumber ?? 0) - (b.caseNumber ?? 0));
      }
    });
  }

  loadTasks() {
    if (!this.selectedProject) return;
    this.loading = true;
    this.taskService.getByProject(this.selectedProject, this.showArchived).subscribe({
      next: t => {
        this.tasks = t.sort((a, b) => (a.caseNumber ?? 0) - (b.caseNumber ?? 0));
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  toggleArchived() {
    this.showArchived = !this.showArchived;
    this.loadTasks();
  }

  get todoTasks()       { return this.filteredTasks.filter(t => t.status === 'TODO'); }
  get inProgressTasks() { return this.filteredTasks.filter(t => t.status === 'IN_PROGRESS'); }
  get doneTasks()       { return this.filteredTasks.filter(t => t.status === 'DONE'); }

  get filteredTasks() {
    let list = this.tasks;
    if (this.filterAssignedToMe && this.me) list = list.filter(t => t.assigneeId === this.me.id);
    if (this.filterStatus !== 'ALL') list = list.filter(t => t.status === this.filterStatus);
    if (this.filterPriority !== 'ALL') list = list.filter(t => (t.priority || 'MEDIUM') === this.filterPriority);
    if (this.caseSearch !== null) list = list.filter(t => t.caseNumber === this.caseSearch);
    return list;
  }

  get myTaskCount() {
    if (!this.me) return 0;
    return this.tasks.filter(t => t.assigneeId === this.me.id).length;
  }

  countByStatus(s: string) { return this.tasks.filter(t => t.status === s).length; }

  createTask() {
    if (this.form.invalid) return;
    this.saving = true;
    const v = this.form.value;
    this.taskService.create({
      title: v.title!,
      description: v.description || undefined,
      projectId: v.projectId!,
      assigneeId: this.autoAssign ? undefined : (v.assigneeId || undefined),
      status: v.status || 'TODO',
      priority: v.priority || 'MEDIUM',
      type: v.type || 'TASK',
      autoAssign: this.autoAssign
    }).subscribe({
      next: t => {
        this.tasks.push(t);
        this.tasks.sort((a, b) => (a.caseNumber ?? 0) - (b.caseNumber ?? 0));
        this.showForm = false;
        this.autoAssign = false;
        this.form.patchValue({ title: '', description: '', assigneeId: '', status: 'TODO', priority: 'MEDIUM', type: 'TASK' });
        this.saving = false;
      },
      error: () => { this.saving = false; }
    });
  }

  changeStatus(task: Task, status: string) {
    this.taskService.changeStatus(task.id, status).subscribe(updated => {
      const idx = this.tasks.findIndex(t => t.id === task.id);
      if (idx !== -1) this.tasks[idx] = updated;
    });
  }

  changePriority(task: Task, priority: string) {
    this.taskService.setPriority(task.id, priority).subscribe(updated => {
      const idx = this.tasks.findIndex(t => t.id === task.id);
      if (idx !== -1) this.tasks[idx] = updated;
    });
  }

  archiveTask(task: Task, e: Event) {
    e.preventDefault(); e.stopPropagation();
    this.taskService.archive(task.id).subscribe(updated => {
      const idx = this.tasks.findIndex(t => t.id === task.id);
      if (idx !== -1) this.tasks[idx] = updated;
      if (!this.showArchived && updated.archived) {
        this.tasks = this.tasks.filter(t => t.id !== task.id);
      }
    });
  }

  openSla(task: Task, e: Event) {
    e.preventDefault(); e.stopPropagation();
    this.slaTaskId = task.id;
    this.slaValue = task.slaDeadline ? task.slaDeadline.slice(0, 16) : '';
  }

  saveSla(e: Event) {
    e.preventDefault(); e.stopPropagation();
    if (!this.slaTaskId || !this.slaValue) return;
    const iso = new Date(this.slaValue).toISOString().slice(0, 19);
    this.taskService.setSla(this.slaTaskId, iso).subscribe(updated => {
      const idx = this.tasks.findIndex(t => t.id === this.slaTaskId);
      if (idx !== -1) this.tasks[idx] = updated;
      this.slaTaskId = null;
    });
  }

  slaLabel(task: Task): string {
    if (!task.slaDeadline) return '';
    const diffH = (new Date(task.slaDeadline).getTime() - Date.now()) / 3600000;
    if (diffH < 0) return 'SLA przekroczone';
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

  priorityClass(priority?: string): string {
    return 'priority-' + (priority || 'medium').toLowerCase();
  }

  priorityLabel(priority?: string): string {
    const map: Record<string, string> = { CRITICAL: '!!', HIGH: '↑', MEDIUM: '–', LOW: '↓' };
    return map[priority || 'MEDIUM'] || '–';
  }

  typeClass(type?: string): string {
    return 'type-' + (type || 'task').toLowerCase();
  }

  get canCreateTask() { return this.auth.isAdminOrLeader; }
  get isAdmin() { return this.auth.isAdmin; }
  get isAdminOrLeader() { return this.auth.isAdminOrLeader; }

  getUserName(task: Task) {
    if (task.assigneeName) return task.assigneeName;
    if (!task.assigneeId) return '—';
    return this.users.find(u => u.id === task.assigneeId)?.username || '—';
  }

  statusLabel(s: string) {
    return ({ TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' } as any)[s] || s;
  }

  typeLabel(t?: string) {
    return ({ TASK: 'Task', BUG: 'Bug', FEATURE: 'Feature', STORY: 'Story' } as any)[t || 'TASK'] || t;
  }

  countByPriority(p: string) {
    return this.tasks.filter(t => (t.priority || 'MEDIUM') === p).length;
  }
}
