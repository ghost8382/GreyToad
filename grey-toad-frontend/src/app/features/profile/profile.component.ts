import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/api.service';
import { User, USER_STATUSES } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  imports: [CommonModule, ReactiveFormsModule]
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  saving = false;
  saved = false;
  statuses = USER_STATUSES;

  form = this.fb.group({
    quote:  [''],
    status: ['AVAILABLE']
  });

  constructor(
    private auth: AuthService,
    private userService: UserService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => {
      this.user = u;
      if (u) {
        this.form.patchValue({
          quote:  u.quote  || '',
          status: u.status || 'AVAILABLE'
        });
      }
    });
  }

  save() {
    this.saving = true;
    this.userService.updateProfile({
      quote:  this.form.value.quote  ?? '',
      status: this.form.value.status ?? 'AVAILABLE'
    }).subscribe({
      next: () => {
        this.auth.loadMe();
        this.saving = false;
        this.saved = true;
        setTimeout(() => this.saved = false, 2000);
      },
      error: () => { this.saving = false; }
    });
  }

  getStatusInfo(value?: string) {
    return this.statuses.find(s => s.value === (value || 'AVAILABLE')) || this.statuses[0];
  }

  getInitials(name?: string) { return name?.slice(0, 2).toUpperCase() || '?'; }

  get roleLabel() {
    const labels: Record<string, string> = { ADMIN: 'Administrator', LEADER: 'Leader', USER: 'Worker' };
    return labels[this.user?.role || ''] || (this.user?.role || '');
  }
}
