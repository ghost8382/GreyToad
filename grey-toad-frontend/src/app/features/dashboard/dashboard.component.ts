import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { ProjectService, TeamService, TaskService } from '../../core/services/api.service';
import { Project, Team, Task, User } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [CommonModule, RouterLink]
})
export class DashboardComponent implements OnInit {
  user: User | null = null;
  teams: Team[] = [];
  myTasks: Task[] = [];
  loading = true;

  get isAdmin() { return this.auth.isAdmin; }
  get isAdminOrLeader() { return this.auth.isAdminOrLeader; }

  get displayTasks()  { return this.myTasks.filter(t => !t.archived); }
  get todoCount()     { return this.myTasks.filter(t => t.status === 'TODO').length; }
  get progressCount() { return this.myTasks.filter(t => t.status === 'IN_PROGRESS').length; }
  get doneCount()     { return this.myTasks.filter(t => t.status === 'DONE').length; }

  constructor(
    private auth: AuthService,
    private teamService: TeamService,
    private projectService: ProjectService,
    private taskService: TaskService
  ) {}

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => { this.user = u; });

    const teams$ = this.auth.isAdmin
      ? this.teamService.getAll()
      : this.teamService.getMyTeams();

    teams$.subscribe({
      next: teams => {
        this.teams = teams;
        this.loadTasks();
      },
      error: () => { this.loading = false; }
    });
  }

  loadTasks() {
    this.projectService.getAll().pipe(
      catchError(() => of([] as Project[])),
      switchMap(projects => {
        if (projects.length === 0) return of([] as Task[]);
        return forkJoin(
          projects.map(p => this.taskService.getByProject(p.id, true).pipe(catchError(() => of([] as Task[]))))
        ).pipe(map(groups => groups.flat()));
      })
    ).subscribe({
      next: tasks => {
        this.myTasks = this.auth.isAdmin
          ? tasks
          : tasks.filter(t => t.assigneeId === this.user?.id);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getGreeting() {
    const h = new Date().getHours();
    if (h < 12) return 'Dzień dobry';
    if (h < 18) return 'Dzień dobry';
    return 'Dobry wieczór';
  }
}
