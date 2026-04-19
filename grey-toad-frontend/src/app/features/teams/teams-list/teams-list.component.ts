import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TeamService, ProjectService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
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
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.isUserRole = !this.auth.isAdmin && !this.auth.isLeader;

    if (this.isUserRole) {
      this.loading = true;
      this.teamService.getMyTeams().subscribe({
        next: t => { this.teams = t; this.loading = false; },
        error: () => { this.loading = false; }
      });
    } else {
      this.projectService.getAll().subscribe(projects => {
        this.projects = projects;
        this.route.queryParams.subscribe(p => {
          if (p['projectId']) {
            this.selectProject(p['projectId']);
            this.form.patchValue({ projectId: p['projectId'] });
          }
        });
      });
    }
  }

  selectProject(projectId: string) {
    this.selectedProjectId = projectId;
    this.teams = [];
    if (!projectId) return;
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
}
