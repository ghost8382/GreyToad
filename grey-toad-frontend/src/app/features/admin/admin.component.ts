import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { UserService, RoleTemplateService } from '../../core/services/api.service';
import { ChatWsService } from '../../core/services/chat-ws.service';
import { User, RoleTemplate } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FormsModule]
})
export class AdminComponent implements OnInit, OnDestroy {
  users: User[] = [];
  showForm = false;
  saving = false;
  error = '';

  roleTemplates: RoleTemplate[] = [];
  showRoleForm = false;
  savingRole = false;
  roleError = '';
  userTemplateMap: Record<string, string> = {};
  private presenceSub?: Subscription;

  form = this.fb.group({
    username: ['', Validators.required],
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]],
    role:     ['USER', Validators.required]
  });

  roleForm = this.fb.group({
    name:            ['', Validators.required],
    permissionLevel: ['USER', Validators.required]
  });

  constructor(
    private userService: UserService,
    private roleTemplateService: RoleTemplateService,
    private ws: ChatWsService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.load();
    this.loadRoleTemplates();
    this.presenceSub = this.ws.presenceUpdate$.subscribe(({ userId, isOnline }) => {
      const idx = this.users.findIndex(u => u.id === userId);
      if (idx !== -1) {
        this.users = [...this.users];
        this.users[idx] = { ...this.users[idx], isOnline };
      }
    });
  }

  ngOnDestroy() {
    this.presenceSub?.unsubscribe();
  }

  load() {
    this.userService.getAll().subscribe(u => {
      this.users = u;
      this.rebuildTemplateMap();
    });
  }

  loadRoleTemplates() {
    this.roleTemplateService.getAll().subscribe(r => {
      this.roleTemplates = r;
      this.rebuildTemplateMap();
    });
  }

  private rebuildTemplateMap() {
    const map: Record<string, string> = {};
    for (const u of this.users) {
      const match = this.roleTemplates.find(r => r.name === u.jobTitle);
      map[u.id] = match?.id ?? '';
    }
    this.userTemplateMap = map;
  }

  createRoleTemplate() {
    if (this.roleForm.invalid) return;
    this.savingRole = true;
    this.roleError = '';
    this.roleTemplateService.create(this.roleForm.value as any).subscribe({
      next: r => {
        this.roleTemplates.push(r);
        this.showRoleForm = false;
        this.roleForm.reset({ permissionLevel: 'USER' });
        this.savingRole = false;
      },
      error: e => {
        this.roleError = e.error?.message || 'Error creating role';
        this.savingRole = false;
      }
    });
  }

  deleteRoleTemplate(id: string) {
    if (!confirm('Delete this role?')) return;
    this.roleTemplateService.delete(id).subscribe(() => {
      this.roleTemplates = this.roleTemplates.filter(r => r.id !== id);
    });
  }

  permissionLabel(level: string) {
    const map: Record<string, string> = { ADMIN: 'Administrator', LEADER: 'Leader', USER: 'Worker' };
    return map[level] || level;
  }

  create() {
    if (this.form.invalid) return;
    this.saving = true;
    this.error = '';
    this.userService.createUser(this.form.value as any).subscribe({
      next: u => {
        this.users.push(u);
        this.showForm = false;
        this.form.reset({ role: 'USER' });
        this.saving = false;
      },
      error: e => {
        this.error = e.error?.message || 'Error creating account';
        this.saving = false;
      }
    });
  }

  delete(id: string) {
    if (!confirm('Delete this user?')) return;
    this.userService.deleteUser(id).subscribe(() => {
      this.users = this.users.filter(u => u.id !== id);
    });
  }

  roleLabel(role: string) {
    const map: Record<string, string> = { ADMIN: 'Administrator', LEADER: 'Leader', USER: 'Worker' };
    return map[role] || role;
  }

  avatarClass(name?: string): string {
    if (!name) return 'av-0';
    const hash = Array.from(name).reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return 'av-' + (hash % 8);
  }

  getEffectiveStatus(user: User): string {
    if (!user.isOnline) return 'OFFLINE';
    return user.status || 'AVAILABLE';
  }

  getStatusColor(status?: string): string {
    const colors: Record<string, string> = {
      AVAILABLE: '#4a9a6a', BREAK: '#e8a44a', DINNER: '#e87a4a',
      OUT_OF_OFFICE: '#aa4a4a', MEETING: '#5a8aaa', OFFLINE: '#5a5a5a'
    };
    return colors[status || ''] || '#5a5a5a';
  }

  changeRole(userId: string, role: string) {
    this.userService.setRole(userId, role).subscribe(() => {
      const u = this.users.find(u => u.id === userId);
      if (u) u.role = role;
    });
  }

  assignRoleTemplate(userId: string, templateId: string) {
    const template = this.roleTemplates.find(r => r.id === templateId);
    const u = this.users.find(u => u.id === userId);

    if (!template) {
      this.userService.setJobTitle(userId, null).subscribe({
        next: () => { if (u) u.jobTitle = undefined; },
        error: () => { this.userTemplateMap[userId] = u?.jobTitle ? (this.roleTemplates.find(r => r.name === u!.jobTitle)?.id ?? '') : ''; }
      });
      return;
    }

    this.userService.setJobTitle(userId, template.name).subscribe({
      next: () => { if (u) u.jobTitle = template.name; },
      error: () => { this.userTemplateMap[userId] = u?.jobTitle ? (this.roleTemplates.find(r => r.name === u!.jobTitle)?.id ?? '') : ''; }
    });
    this.userService.setRole(userId, template.permissionLevel).subscribe({
      next: () => { if (u) u.role = template.permissionLevel; }
    });
  }

  getStatusLabel(status?: string): string {
    const labels: Record<string, string> = {
      AVAILABLE: 'Available', BREAK: 'Break', DINNER: 'Lunch',
      OUT_OF_OFFICE: 'Out of office', MEETING: 'Meeting', OFFLINE: 'Offline'
    };
    return labels[status || ''] || 'Offline';
  }
}
