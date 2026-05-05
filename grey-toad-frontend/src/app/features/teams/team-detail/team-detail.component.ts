import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TeamService, ChannelService, UserService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { Team, TeamMember, Channel, User } from '../../../shared/models';

@Component({
  standalone: true,
  selector: 'app-team-detail',
  templateUrl: './team-detail.component.html',
  styleUrls: ['./team-detail.component.scss'],
  imports: [CommonModule, ReactiveFormsModule, RouterLink]
})
export class TeamDetailComponent implements OnInit {
  team: Team | null = null;
  members: TeamMember[] = [];
  channels: Channel[] = [];
  allUsers: User[] = [];
  loading = true;
  activeTab: 'members' | 'channels' = 'members';

  showAddMember = false;
  showAddChannel = false;
  saving = false;

  memberForm = this.fb.group({
    userId: ['', Validators.required],
    role: ['MEMBER', Validators.required]
  });

  channelForm = this.fb.group({ name: ['', Validators.required] });

  constructor(
    private route: ActivatedRoute,
    private teamService: TeamService,
    private channelService: ChannelService,
    private userService: UserService,
    public auth: AuthService,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.teamService.getById(id).subscribe(t => { this.team = t; this.loading = false; });
    this.teamService.getMembers(id).subscribe(m => this.members = m);
    this.channelService.getByTeam(id).subscribe(c => this.channels = c);
    this.userService.getAll().subscribe(u => this.allUsers = u);
  }

  addMember() {
    if (this.memberForm.invalid || !this.team) return;
    this.saving = true;
    this.teamService.addMember(this.team.id, this.memberForm.value as any).subscribe({
      next: m => { this.members.push(m); this.showAddMember = false; this.memberForm.reset({ role: 'MEMBER' }); this.saving = false; },
      error: () => { this.saving = false; }
    });
  }

  removeMember(memberId: string) {
    if (!this.team || !confirm('Remove member?')) return;
    this.teamService.removeMember(this.team.id, memberId).subscribe(() => {
      this.members = this.members.filter(m => m.id !== memberId);
    });
  }

  setAsLeader(member: TeamMember) {
    const user = this.allUsers.find(u => u.id === member.userId);
    if (!confirm(`Ustaw ${user?.username} jako Lidera?`)) return;
    this.userService.setRole(member.userId, 'LEADER').subscribe({
      next: () => { const u = this.allUsers.find(u => u.id === member.userId); if (u) u.role = 'LEADER'; }
    });
  }

  demoteToWorker(member: TeamMember) {
    const user = this.allUsers.find(u => u.id === member.userId);
    if (!confirm(`Remove Leader role from ${user?.username}?`)) return;
    this.userService.setRole(member.userId, 'USER').subscribe({
      next: () => { const u = this.allUsers.find(u => u.id === member.userId); if (u) u.role = 'USER'; }
    });
  }

  getUserRole(userId: string): string {
    return this.allUsers.find(u => u.id === userId)?.role || 'USER';
  }

  addChannel() {
    if (this.channelForm.invalid || !this.team) return;
    this.saving = true;
    this.channelService.create({ name: this.channelForm.value.name!, teamId: this.team.id }).subscribe({
      next: c => { this.channels.push(c); this.showAddChannel = false; this.channelForm.reset(); this.saving = false; },
      error: () => { this.saving = false; }
    });
  }

  getUserName(userId: string) {
    return this.allUsers.find(u => u.id === userId)?.username || userId.slice(0,8);
  }

  get isAdmin() { return this.auth.isAdmin; }
  get isLeader() { return this.auth.isLeader; }

  getUserStatusColor(userId: string): string {
    const status = this.allUsers.find(u => u.id === userId)?.status;
    const colors: Record<string, string> = {
      AVAILABLE: '#4a9a6a', BREAK: '#e8a44a', DINNER: '#e87a4a',
      OUT_OF_OFFICE: '#aa4a4a', MEETING: '#5a8aaa', OFFLINE: '#5a5a5a'
    };
    return colors[status || ''] || '#5a5a5a';
  }

  getUserStatusLabel(userId: string): string {
    const status = this.allUsers.find(u => u.id === userId)?.status;
    const labels: Record<string, string> = {
      AVAILABLE: 'Available', BREAK: 'Break', DINNER: 'Lunch',
      OUT_OF_OFFICE: 'Out of office', MEETING: 'Meeting', OFFLINE: 'Offline'
    };
    return labels[status || ''] || 'Offline';
  }

  getUserEmail(userId: string): string {
    return this.allUsers.find(u => u.id === userId)?.email || '';
  }

  getUserJobTitle(userId: string): string {
    return this.allUsers.find(u => u.id === userId)?.jobTitle || '';
  }
}
