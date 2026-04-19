import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { UserService } from '../../core/services/api.service';
import { User } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink]
})
export class AdminComponent implements OnInit {
  users: User[] = [];
  showForm = false;
  saving = false;
  error = '';

  form = this.fb.group({
    username: ['', Validators.required],
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(4)]],
    role:     ['USER', Validators.required]
  });

  constructor(private userService: UserService, private fb: FormBuilder) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.userService.getAll().subscribe(u => this.users = u);
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
        this.error = e.error?.message || 'Błąd tworzenia konta';
        this.saving = false;
      }
    });
  }

  delete(id: string) {
    if (!confirm('Usunąć tego użytkownika?')) return;
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

  getStatusLabel(status?: string): string {
    const labels: Record<string, string> = {
      AVAILABLE: 'Dostępny', BREAK: 'Przerwa', DINNER: 'Obiad',
      OUT_OF_OFFICE: 'Poza biurem', MEETING: 'Spotkanie', OFFLINE: 'Offline'
    };
    return labels[status || ''] || 'Offline';
  }
}
