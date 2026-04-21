import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ChatWsService } from '../../core/services/chat-ws.service';
import { User, USER_STATUSES, AppNotification } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-shell',
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
  imports: [CommonModule, RouterOutlet, RouterLink]
})
export class ShellComponent implements OnInit, OnDestroy {
  user: User | null = null;
  activeRoute = '';
  sidebarCollapsed = window.innerWidth <= 768;
  isDarkMode = true;

  notifications: AppNotification[] = [];
  bellOpen = false;
  private subs: Subscription[] = [];

  baseNavItems = [
    { path: '/dashboard', label: 'Dashboard',  icon: 'grid' },
    { path: '/teams',     label: 'Teams',       icon: 'users' },
    { path: '/projects',  label: 'Projects',    icon: 'folder' },
    { path: '/tasks',     label: 'Tasks',       icon: 'check-square' },
    { path: '/chat',      label: 'Channels',    icon: 'hash' },
    { path: '/messages',  label: 'Messages',    icon: 'message-circle' },
    { path: '/profile',   label: 'Profile',     icon: 'user' }
  ];

  get navItems() {
    return this.isAdmin
      ? [...this.baseNavItems, { path: '/admin', label: 'Users', icon: 'shield' }]
      : this.baseNavItems;
  }

  constructor(public auth: AuthService, private router: Router, private ws: ChatWsService) {}

  ngOnInit() {
    const savedTheme = localStorage.getItem('gt_theme');
    this.isDarkMode = savedTheme !== 'light';
    document.documentElement.setAttribute('data-theme', this.isDarkMode ? 'dark' : 'light');

    this.auth.currentUser$.subscribe(u => this.user = u);
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.activeRoute = e.url;
      if (window.innerWidth <= 768) this.sidebarCollapsed = true;
    });
    this.activeRoute = this.router.url;

    // Delay WebSocket init past FCP — runs after first browser paint
    setTimeout(() => {
      this.ws.connect();
      this.subs.push(this.ws.notification$.subscribe(n => {
        this.notifications.unshift(n);
      }));
    }, 0);
  }

  ngOnDestroy() {
    this.subs.forEach(s => s.unsubscribe());
  }

  get unreadCount() { return this.notifications.length; }

  toggleBell() { this.bellOpen = !this.bellOpen; }

  clearNotifications() {
    this.notifications = [];
    this.bellOpen = false;
  }

  toggleTheme() {
    this.isDarkMode = !this.isDarkMode;
    const theme = this.isDarkMode ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('gt_theme', theme);
  }

  get isAdmin()        { return this.auth.isAdmin; }
  get isAdminOrLeader(){ return this.auth.isAdminOrLeader; }
  isActive(path: string) { return this.activeRoute.startsWith(path); }
  logout() { this.auth.logout(); }
  getInitials(name?: string) { return name?.slice(0, 2).toUpperCase() || '?'; }

  avatarClass(name?: string): string {
    if (!name) return 'av-0';
    const hash = Array.from(name).reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return 'av-' + (hash % 8);
  }

  getStatusColor(status?: string) {
    return USER_STATUSES.find(s => s.value === status)?.color || '#5a5a5a';
  }

  get roleLabel() {
    const labels: Record<string, string> = { ADMIN: 'Administrator', LEADER: 'Leader', USER: 'Worker' };
    return labels[this.user?.role || ''] || '';
  }
}
