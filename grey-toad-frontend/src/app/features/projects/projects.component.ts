import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ProjectService, TeamService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { Project, Team } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-projects',
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink]
})
export class ProjectsComponent implements OnInit {
  projects: Project[] = [];
  teamsByProject: Record<string, Team[]> = {};
  loading = false;
  showForm = false;
  saving = false;
  error = '';
  expandedProject: string | null = null;

  form = this.fb.group({ name: ['', Validators.required] });

  constructor(
    private projectService: ProjectService,
    private teamService: TeamService,
    public auth: AuthService,
    private fb: FormBuilder,
    private router: Router
  ) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.projectService.getAll().subscribe({
      next: p => { this.projects = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  create() {
    if (this.form.invalid) return;
    this.saving = true; this.error = '';
    this.projectService.create({ name: this.form.value.name! }).subscribe({
      next: p => {
        this.projects.unshift(p);
        this.showForm = false;
        this.form.reset();
        this.saving = false;
      },
      error: (e) => { this.error = e.error?.message || 'Failed to create project'; this.saving = false; }
    });
  }

  delete(id: string, e: Event) {
    e.preventDefault(); e.stopPropagation();
    if (!confirm('Delete this project and all its teams?')) return;
    this.projectService.delete(id).subscribe(() => {
      this.projects = this.projects.filter(p => p.id !== id);
    });
  }

  toggleExpand(projectId: string) {
    if (this.expandedProject === projectId) {
      this.expandedProject = null;
      return;
    }
    this.expandedProject = projectId;
    if (!this.teamsByProject[projectId]) {
      this.teamService.getByProject(projectId).subscribe(
        teams => this.teamsByProject[projectId] = teams
      );
    }
  }

  viewTasks(projectId: string) {
    this.router.navigate(['/tasks'], { queryParams: { projectId } });
  }

  goToTeam(teamId: string) {
    this.router.navigate(['/teams', teamId]);
  }

  get isAdmin() { return this.auth.isAdmin; }
}
