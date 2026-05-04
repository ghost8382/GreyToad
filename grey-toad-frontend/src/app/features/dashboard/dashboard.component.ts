import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { catchError, forkJoin, map, of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { ProjectContextService } from '../../core/services/project-context.service';
import { TeamService, TaskService } from '../../core/services/api.service';
import { Team, Task, User } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [CommonModule, RouterLink]
})
export class DashboardComponent implements OnInit, OnDestroy {
  user: User | null = null;
  teams: Team[] = [];
  myTasks: Task[] = [];
  loading = false;
  private sub?: Subscription;

  get isAdmin()         { return this.auth.isAdmin; }
  get isAdminOrLeader() { return this.auth.isAdminOrLeader; }
  get selectedProject() { return this.projectContext.selected; }

  get displayTasks()        { return this.myTasks.filter(t => !t.archived).slice(0, 6); }
  get todoCount()           { return this.myTasks.filter(t => t.status === 'TODO').length; }
  get progressCount()       { return this.myTasks.filter(t => t.status === 'IN_PROGRESS').length; }
  get doneCount()           { return this.myTasks.filter(t => t.status === 'DONE').length; }
  get notMemberOfProject()  { return !this.loading && !this.isAdminOrLeader && this.teams.length === 0 && !!this.selectedProject; }

  constructor(
    private auth: AuthService,
    private projectContext: ProjectContextService,
    private teamService: TeamService,
    private taskService: TaskService
  ) {}

  ngOnInit() {
    this.auth.currentUser$.subscribe(u => { this.user = u; });

    // Reload whenever selected project changes
    this.sub = this.projectContext.selected$.subscribe(project => {
      this.teams = [];
      this.myTasks = [];
      if (!project) return;
      this.load(project.id);
    });
  }

  ngOnDestroy() { this.sub?.unsubscribe(); }

  private load(projectId: string) {
    this.loading = true;

    const teams$ = this.isAdmin
      ? this.teamService.getAll().pipe(
          map(t => t.filter(team => team.projectId === projectId)),
          catchError(() => of([] as Team[]))
        )
      : this.teamService.getMyTeams().pipe(
          map(t => t.filter(team => team.projectId === projectId)),
          catchError(() => of([] as Team[]))
        );

    teams$.subscribe(teams => { this.teams = teams; });

    this.taskService.getByProject(projectId, true).pipe(
      catchError(() => of([] as Task[]))
    ).subscribe(tasks => {
      this.myTasks = this.isAdmin
        ? tasks
        : tasks.filter(t => t.assigneeId === this.user?.id);
      this.loading = false;
    });
  }

  getGreeting() {
    const h = new Date().getHours();
    if (h < 12) return 'Dzień dobry';
    if (h < 18) return 'Dzień dobry';
    return 'Dobry wieczór';
  }
}
