import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private base = environment.apiUrl;
  currentUser$ = new BehaviorSubject<User | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    // On app start, try to restore user from saved token
    if (this.isLoggedIn) this.loadMe();
  }

  login(req: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.base}/auth/login`, req).pipe(
      tap(res => {
        localStorage.setItem('gt_token', res.token);
        this.http.patch(`${this.base}/users/me`, { status: 'AVAILABLE' }).subscribe();
        this.loadMe();
      })
    );
  }

  register(req: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.base}/auth/register`, req).pipe(
      tap(res => {
        localStorage.setItem('gt_token', res.token);
        this.loadMe();
      })
    );
  }

  logout() {
    this.http.patch(`${this.base}/users/me`, { status: 'OFFLINE' }).subscribe({
      next: () => this._doLogout(),
      error: () => this._doLogout()
    });
  }

  private _doLogout() {
    localStorage.removeItem('gt_token');
    this.currentUser$.next(null);
    this.router.navigate(['/auth/login']);
  }

  loadMe() {
    this.http.get<User>(`${this.base}/users/me`).subscribe({
      next: u => this.currentUser$.next(u),
      error: () => this._doLogout()
    });
  }

  get isLoggedIn()       { return !!localStorage.getItem('gt_token'); }
  get token()            { return localStorage.getItem('gt_token'); }
  get isAdmin()          { return this.currentUser$.value?.role === 'ADMIN'; }
  get isLeader()         { return this.currentUser$.value?.role === 'LEADER'; }
  get isAdminOrLeader()  { return this.isAdmin || this.isLeader; }
}
