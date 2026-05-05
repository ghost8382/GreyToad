import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subscription, fromEvent, merge } from 'rxjs';
import { throttleTime } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PresenceService implements OnDestroy {
  private heartbeatTimer?: ReturnType<typeof setInterval>;
  private activitySub?: Subscription;
  private lastSent = 0;
  private readonly INTERVAL_MS = 30_000;

  constructor(private http: HttpClient) {}

  start() {
    if (this.heartbeatTimer) return;
    this.sendHeartbeat();
    this.activitySub = merge(
      fromEvent(document, 'mousemove'),
      fromEvent(document, 'keydown'),
      fromEvent(document, 'click')
    ).pipe(throttleTime(this.INTERVAL_MS)).subscribe(() => this.sendHeartbeat());
    this.heartbeatTimer = setInterval(() => this.sendHeartbeat(), this.INTERVAL_MS);
  }

  stop() {
    if (this.heartbeatTimer) { clearInterval(this.heartbeatTimer); this.heartbeatTimer = undefined; }
    this.activitySub?.unsubscribe();
    this.activitySub = undefined;
  }

  ngOnDestroy() { this.stop(); }

  private sendHeartbeat() {
    const now = Date.now();
    if (now - this.lastSent < this.INTERVAL_MS - 2000) return;
    this.lastSent = now;
    this.http.put(`${environment.apiUrl}/users/heartbeat`, {}).subscribe({ error: () => {} });
  }
}
