import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { Me } from './models';

/** Profil applicatif de l'utilisateur connecté (chargé depuis GET /api/me). */
@Injectable({ providedIn: 'root' })
export class CurrentUserStore {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  private readonly me = signal<Me | null>(null);
  readonly profile = this.me.asReadonly();
  readonly isAdmin = computed(() => this.me()?.admin ?? false);

  constructor() {
    effect(() => {
      if (this.auth.isLoggedIn()) {
        this.api.me().subscribe({ next: (me) => this.me.set(me), error: () => this.me.set(null) });
      } else {
        this.me.set(null);
      }
    });
  }
}
