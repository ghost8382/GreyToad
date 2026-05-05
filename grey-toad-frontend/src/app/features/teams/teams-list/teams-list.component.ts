import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { TeamService, ProjectService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectContextService } from '../../../core/services/project-context.service';
import { Team, Project } from '../../../shared/models';

@Component({
  standalone: true,
  selector: 'app-teams-list',
  templateUrl: './teams-list.component.html',
  styleUrls: ['./teams-list.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink]
})
export class TeamsListComponent implements OnInit {
  projects: Project[] = [];
  teams: Team[] = [];
  selectedProjectId = '';
  loading = false;
  showForm = false;
  saving = false;
  error = '';
  isUserRole = false;

  form = this.fb.group({
    name: ['', Validators.required],
    projectId: ['', Validators.required]
  });

  constructor(
    private teamService: TeamService,
    private projectService: ProjectService,
    public auth: AuthService,
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private projectContext: ProjectContextService
  ) {}

  ngOnInit() {
    this.isUserRole = !this.auth.isAdmin && !this.auth.isLeader;

    if (this.isUserRole) {
      this.loading = true;
      this.teamService.getMyTeams().subscribe({
        next: t => {
          const selectedId = this.projectContext.selected?.id;
          this.teams = selectedId ? t.filter(team => team.projectId === selectedId) : t;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    } else {
      this.projectService.getAll().subscribe(projects => {
        this.projects = projects;
        this.route.queryParams.subscribe(p => {
          const toSelect = p['projectId'] || this.projectContext.selected?.id || '';
          if (toSelect) {
            this.selectProject(toSelect);
            this.form.patchValue({ projectId: toSelect });
          } else {
            this.loadAllTeams();
          }
        });
      });
    }
  }

  loadAllTeams() {
    this.selectedProjectId = '';
    this.teams = [];
    if (!this.projects.length) return;
    this.loading = true;
    forkJoin(this.projects.map(p => this.teamService.getByProject(p.id))).subscribe({
      next: results => { this.teams = results.flat(); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  selectProject(projectId: string) {
    this.selectedProjectId = projectId;
    this.teams = [];
    if (!projectId) { this.loadAllTeams(); return; }
    this.loading = true;
    this.form.patchValue({ projectId });
    this.teamService.getByProject(projectId).subscribe({
      next: t => { this.teams = t; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  create() {
    if (this.form.invalid) return;
    this.saving = true; this.error = '';
    this.teamService.create({
      name: this.form.value.name!,
      projectId: this.form.value.projectId!
    }).subscribe({
      next: t => {
        this.teams.unshift(t);
        this.showForm = false;
        this.form.patchValue({ name: '' });
        this.saving = false;
      },
      error: (e) => { this.error = e.error?.message || 'Failed to create team'; this.saving = false; }
    });
  }

  delete(id: string, e: Event) {
    e.preventDefault(); e.stopPropagation();
    if (!confirm('Delete this team?')) return;
    this.teamService.delete(id).subscribe(() => {
      this.teams = this.teams.filter(t => t.id !== id);
    });
  }

  get isAdmin() { return this.auth.isAdmin; }
  get isAdminOrLeader() { return this.auth.isAdminOrLeader; }
  getProjectName(id: string) { return this.projects.find(p => p.id === id)?.name || ''; }

  get teamsByProject(): { projectId: string; projectName: string; teams: Team[] }[] {
    const groups: Record<string, { projectName: string; teams: Team[] }> = {};
    for (const team of this.teams) {
      const pid = team.projectId ?? '';
      if (!groups[pid]) groups[pid] = { projectName: this.getProjectName(pid), teams: [] };
      groups[pid].teams.push(team);
    }
    return Object.entries(groups).map(([projectId, { projectName, teams }]) => ({ projectId, projectName, teams }));
  }
}
