import { Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin } from 'rxjs';
import { Project } from '../../shared/models';
import { ProjectService, TeamService } from './api.service';

const STORAGE_KEY = 'gt_project_id';

@Injectable({ providedIn: 'root' })
export class ProjectContextService {
  private _projects$ = new BehaviorSubject<Project[]>([]);
  private _selected$ = new BehaviorSubject<Project | null>(null);

  readonly projects$ = this._projects$.asObservable();
  readonly selected$ = this._selected$.asObservable();

  constructor(
    private projectService: ProjectService,
    private teamService: TeamService
  ) {}

  load(isAdminOrLeader: boolean) {
    if (isAdminOrLeader) {
      this.projectService.getAll().subscribe(projects => {
        this._projects$.next(projects);
        this._restore(projects);
      });
    } else {
      this.teamService.getMyTeams().subscribe(teams => {
        const ids = [...new Set(
          teams.map(t => t.projectId).filter((id): id is string => !!id)
        )];
        if (!ids.length) { this._projects$.next([]); return; }
        forkJoin(ids.map(id => this.projectService.getById(id))).subscribe(projects => {
          this._projects$.next(projects);
          this._restore(projects);
        });
      });
    }
  }

  select(project: Project) {
    this._selected$.next(project);
    localStorage.setItem(STORAGE_KEY, project.id);
  }

  get selected(): Project | null { return this._selected$.value; }
  get projects(): Project[] { return this._projects$.value; }

  private _restore(projects: Project[]) {
    const savedId = localStorage.getItem(STORAGE_KEY);
    const found = savedId ? projects.find(p => p.id === savedId) : null;
    this._selected$.next(found ?? projects[0] ?? null);
  }
}
