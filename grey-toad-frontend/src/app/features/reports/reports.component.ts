import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskService } from '../../core/services/api.service';
import { ProjectContextService } from '../../core/services/project-context.service';
import { Task } from '../../shared/models';

@Component({
  standalone: true,
  selector: 'app-reports',
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss'],
  imports: [CommonModule]
})
export class ReportsComponent implements OnInit {
  tasks: Task[] = [];
  loading = true;
  noProject = false;

  constructor(
    private taskService: TaskService,
    private projectContext: ProjectContextService
  ) {}

  ngOnInit() {
    const projectId = this.projectContext.selected?.id;
    if (!projectId) { this.loading = false; this.noProject = true; return; }
    this.taskService.getByProject(projectId, true).subscribe({
      next: tasks => { this.tasks = tasks; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get total()          { return this.tasks.length; }
  get doneCount()      { return this.tasks.filter(t => t.status === 'DONE').length; }
  get progressCount()  { return this.tasks.filter(t => t.status === 'IN_PROGRESS').length; }
  get todoCount()      { return this.tasks.filter(t => t.status === 'TODO').length; }
  get archivedCount()  { return this.tasks.filter(t => t.archived).length; }
  get unassignedCount(){ return this.tasks.filter(t => !t.assigneeId).length; }

  get statusStats() {
    const total = this.total || 1;
    return [
      { label: 'To Do',       count: this.todoCount,     pct: Math.round(this.todoCount / total * 100),     cls: 'bar-todo' },
      { label: 'In Progress', count: this.progressCount, pct: Math.round(this.progressCount / total * 100), cls: 'bar-progress' },
      { label: 'Done',        count: this.doneCount,     pct: Math.round(this.doneCount / total * 100),     cls: 'bar-done' },
    ];
  }

  get priorityStats() {
    const counts: Record<string, number> = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 };
    this.tasks.forEach(t => { const p = t.priority || 'MEDIUM'; if (p in counts) counts[p]++; });
    const max = Math.max(...Object.values(counts), 1);
    return [
      { label: 'Critical', count: counts['CRITICAL'], pct: Math.round(counts['CRITICAL'] / max * 100), cls: 'bar-critical' },
      { label: 'High',     count: counts['HIGH'],     pct: Math.round(counts['HIGH'] / max * 100),     cls: 'bar-high' },
      { label: 'Medium',   count: counts['MEDIUM'],   pct: Math.round(counts['MEDIUM'] / max * 100),   cls: 'bar-medium' },
      { label: 'Low',      count: counts['LOW'],       pct: Math.round(counts['LOW'] / max * 100),     cls: 'bar-low' },
    ];
  }

  get typeStats() {
    const counts: Record<string, number> = {};
    this.tasks.forEach(t => { const type = t.type || 'TASK'; counts[type] = (counts[type] || 0) + 1; });
    const total = this.total || 1;
    return Object.entries(counts)
      .map(([type, count]) => ({ type, count, pct: Math.round(count / total * 100) }))
      .sort((a, b) => b.count - a.count);
  }

  get userStats() {
    const map: Record<string, { name: string; count: number }> = {};
    this.tasks.forEach(t => {
      const name = t.assigneeName || 'Unassigned';
      if (!map[name]) map[name] = { name, count: 0 };
      map[name].count++;
    });
    const items = Object.values(map).sort((a, b) => b.count - a.count).slice(0, 12);
    const max = items[0]?.count || 1;
    return items.map(i => ({ name: i.name, count: i.count, pct: Math.round(i.count / max * 100) }));
  }

  get slaStats() {
    const now = Date.now();
    let ok = 0, risk = 0, breached = 0, none = 0;
    this.tasks.forEach(t => {
      if (!t.slaDeadline) { none++; return; }
      const diff = (new Date(t.slaDeadline).getTime() - now) / 3600000;
      if (diff < 0) breached++;
      else if (diff < 8) risk++;
      else ok++;
    });
    return { ok, risk, breached, none };
  }

  get projectName() { return this.projectContext.selected?.name || ''; }
}
