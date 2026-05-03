import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ChatWsService } from '../../core/services/chat-ws.service';
import { ProjectContextService } from '../../core/services/project-context.service';
import { DmUnreadService } from '../../core/services/dm-unread.service';
import { TaskService } from '../../core/services/api.service';
import { User, USER_STATUSES, AppNotification, Project, Task } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-shell',
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.scss'],
  imports: [CommonModule, RouterOutlet, RouterLink, FormsModule]
})
export class ShellComponent implements OnInit, OnDestroy {
  user: User | null = null;
  activeRoute = '';
  sidebarCollapsed = window.innerWidth <= 768;
  isDarkMode = true;
  projectSelectorOpen = false;

  taskNotifications: AppNotification[] = [];
  channelUnread = 0;
  dmUnreadCount = 0;           // nav badge counter for Messages tab
  projectUnread: Record<string, number> = {};

  bellOpen = false;
  searchOpen = false;
  searchQuery = '';
  searchResults: Task[] = [];
  searchLoading = false;
  private searchDebounce: any;
  private subs: Subscription[] = [];
  private projectsLoaded = false;

  baseNavItems = [
    { path: '/dashboard', label: 'Dashboard', icon: 'grid' },
    { path: '/teams',     label: 'Teams',      icon: 'users' },
    { path: '/tasks',     label: 'Tasks',      icon: 'check-square' },
    { path: '/chat',      label: 'Channels',   icon: 'hash' },
    { path: '/messages',  label: 'Messages',   icon: 'message-circle' },
    { path: '/reports',   label: 'Reports',    icon: 'bar-chart-2' },
    { path: '/profile',   label: 'Profile',    icon: 'user' }
  ];

  get navItems() {
    if (this.isAdmin) {
      return [
        ...this.baseNavItems,
        { path: '/projects', label: 'Projects', icon: 'folder' },
        { path: '/admin',    label: 'Users',    icon: 'shield' }
      ];
    }
    return this.baseNavItems;
  }

  constructor(
    public auth: AuthService,
    private router: Router,
    private ws: ChatWsService,
    public projectContext: ProjectContextService,
    public dmUnreadSvc: DmUnreadService,
    private taskService: TaskService
  ) {}

  ngOnInit() {
    const savedTheme = localStorage.getItem('gt_theme');
    this.isDarkMode = savedTheme !== 'light';
    document.documentElement.setAttribute('data-theme', this.isDarkMode ? 'dark' : 'light');

    this.auth.currentUser$.subscribe(u => {
      this.user = u;
      if (u && !this.projectsLoaded) {
        this.projectsLoaded = true;
        this.projectContext.load(this.auth.isAdmin || this.auth.isLeader);
      }
    });

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.activeRoute = e.url;
      this.projectSelectorOpen = false;
      if (window.innerWidth <= 768) this.sidebarCollapsed = true;
      if (e.url.startsWith('/chat'))     this.channelUnread = 0;
      if (e.url.startsWith('/messages')) this.dmUnreadCount = 0;
      if (this.searchOpen) this.closeSearch();
    });
    this.activeRoute = this.router.url;

    setTimeout(() => {
      this.ws.connect();
      this.subs.push(this.ws.notification$.subscribe(n => {
        if (n.type === 'TASK_ASSIGNED') {
          this.taskNotifications.unshift(n);
          if (n.projectId) {
            this.projectUnread = {
              ...this.projectUnread,
              [n.projectId]: (this.projectUnread[n.projectId] || 0) + 1
            };
          }
        } else if (n.type === 'CHANNEL_MESSAGE') {
          const fromCurrentProject = !n.projectId || n.projectId === this.projectContext.selected?.id;
          if (fromCurrentProject) {
            if (!this.activeRoute.startsWith('/chat')) this.channelUnread++;
          } else {
            this.projectUnread = {
              ...this.projectUnread,
              [n.projectId!]: (this.projectUnread[n.projectId!] || 0) + 1
            };
          }
        } else if (n.type === 'DM') {
          if (!this.activeRoute.startsWith('/messages')) this.dmUnreadCount++;
        }
      }));

      // Per-user DM tracking — feeds MessagesComponent badges
      this.subs.push(this.ws.dm$.subscribe(msg => {
        const myId = this.auth.currentUser$.value?.id;
        if (msg.senderId !== myId && !this.activeRoute.startsWith('/messages')) {
          this.dmUnreadSvc.increment(msg.senderId);
        }
      }));
    }, 0);
  }

  ngOnDestroy() {
    this.subs.forEach(s => s.unsubscribe());
  }

  get bellCount()           { return this.taskNotifications.length; }
  get projectBadgeCount()   { return this.projectUnread[this.selectedProject?.id || ''] || 0; }
  get hasAnyProjectUnread() { return Object.values(this.projectUnread).some(v => v > 0); }
  get channelBadgeCount()   { return this.channelUnread; }
  get dmBadgeCount()        { return this.dmUnreadCount; }

  toggleBell() { this.bellOpen = !this.bellOpen; }

  clearNotifications() {
    this.taskNotifications = [];
    this.bellOpen = false;
  }

  toggleTheme() {
    this.isDarkMode = !this.isDarkMode;
    const theme = this.isDarkMode ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('gt_theme', theme);
  }

  get projects()        { return this.projectContext.projects; }
  get selectedProject() { return this.projectContext.selected; }

  selectProject(project: Project) {
    this.ws.unsubscribeFromChannel();
    if (this.projectUnread[project.id]) {
      const updated = { ...this.projectUnread };
      delete updated[project.id];
      this.projectUnread = updated;
    }
    this.projectContext.select(project);
    this.projectSelectorOpen = false;
    this.router.navigateByUrl('/', { skipLocationChange: true })
      .then(() => this.router.navigate(['/dashboard']));
  }

  getProjectUnread(projectId: string): number { return this.projectUnread[projectId] || 0; }

  projectColorClass(name?: string): string {
    if (!name) return 'pc-0';
    const hash = Array.from(name).reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return 'pc-' + (hash % 8);
  }

  openSearch()  { this.searchOpen = true; }
  closeSearch() {
    this.searchOpen = false;
    this.searchQuery = '';
    this.searchResults = [];
    clearTimeout(this.searchDebounce);
  }

  onSearchInput() {
    clearTimeout(this.searchDebounce);
    if (!this.searchQuery.trim()) { this.searchResults = []; return; }
    this.searchLoading = true;
    this.searchDebounce = setTimeout(() => {
      const pid = this.projectContext.selected?.id;
      if (!pid) { this.searchLoading = false; return; }
      this.taskService.getByProject(pid).subscribe(tasks => {
        const q = this.searchQuery.toLowerCase();
        this.searchResults = tasks.filter(t =>
          t.title.toLowerCase().includes(q) ||
          String(t.caseNumber ?? '').includes(q)
        ).slice(0, 8);
        this.searchLoading = false;
      });
    }, 280);
  }

  goToTask(taskId: string) {
    this.closeSearch();
    this.router.navigate(['/tasks', taskId]);
  }

  get isAdmin()         { return this.auth.isAdmin; }
  get isAdminOrLeader() { return this.auth.isAdminOrLeader; }
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

  statusLabel(s: string) {
    return ({ TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' } as any)[s] || s;
  }

  get roleLabel() {
    const labels: Record<string, string> = { ADMIN: 'Administrator', LEADER: 'Leader', USER: 'Worker' };
    return labels[this.user?.role || ''] || '';
  }
}
