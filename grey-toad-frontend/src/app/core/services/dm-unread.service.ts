import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DmUnreadService {
  private _map$ = new BehaviorSubject<Record<string, number>>({});
  readonly map$ = this._map$.asObservable();

  seed(counts: Record<string, number>) {
    this._map$.next({ ...counts });
  }

  increment(senderId: string) {
    const m = { ...this._map$.value };
    m[senderId] = (m[senderId] || 0) + 1;
    this._map$.next(m);
  }

  clear(senderId: string) {
    const m = { ...this._map$.value };
    m[senderId] = 0;
    this._map$.next(m);
  }

  get(senderId: string): number {
    return this._map$.value[senderId] || 0;
  }

  get totalUnread(): number {
    return Object.values(this._map$.value).reduce((s, v) => s + v, 0);
  }
}
