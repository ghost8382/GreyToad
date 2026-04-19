import { Routes } from '@angular/router';
import { ShellComponent } from './shell.component';

export const SHELL_ROUTES: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('../dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'teams',
        loadChildren: () => import('../teams/teams.routes').then(m => m.TEAMS_ROUTES)
      },
      {
        path: 'projects',
        loadComponent: () => import('../projects/projects.component').then(m => m.ProjectsComponent)
      },
      {
        path: 'tasks',
        loadChildren: () => import('../tasks/tasks.routes').then(m => m.TASKS_ROUTES)
      },
      {
        path: 'chat',
        loadComponent: () => import('../chat/chat.component').then(m => m.ChatComponent)
      },
      {
        path: 'messages',
        loadComponent: () => import('../messages/messages.component').then(m => m.MessagesComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('../profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'admin',
        loadComponent: () => import('../admin/admin.component').then(m => m.AdminComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  }
];
