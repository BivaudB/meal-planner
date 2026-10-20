import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth.service';
import { CurrentUserStore } from './core/current-user.store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <div class="topbar-inner">
        <a routerLink="/" class="brand">
          <img src="favicon.svg" alt="" width="28" height="28" />
          <span>Meal Planner</span>
        </a>

        @if (auth.isLoggedIn()) {
          <nav>
            <a routerLink="/recettes" routerLinkActive="active">Recettes</a>
            <a routerLink="/garde-manger" routerLinkActive="active">Mes ingrédients</a>
            <a routerLink="/suggestions" routerLinkActive="active">Suggestions</a>
            <a routerLink="/import" routerLinkActive="active">Import</a>
            <a routerLink="/aliments" routerLinkActive="active">Aliments</a>
            @if (user.isAdmin()) {
              <a routerLink="/admin" routerLinkActive="active">Admin</a>
            }
          </nav>
          <div class="account">
            <span class="muted small">{{ auth.displayName() }}</span>
            <button class="btn btn-sm" (click)="auth.logout()">Déconnexion</button>
          </div>
        } @else {
          <button class="btn btn-primary btn-sm" (click)="auth.login()">Se connecter</button>
        }
      </div>
    </header>

    @if (auth.authServerUnavailable()) {
      <div class="page" style="padding-bottom: 0">
        <div class="alert alert-danger">
          Le serveur d'authentification (Keycloak) est injoignable. Lancez <code>docker compose up -d</code> puis
          rechargez la page.
        </div>
      </div>
    }

    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    .topbar {
      position: sticky;
      top: 0;
      z-index: 10;
      background: rgb(246 241 232 / 92%);
      backdrop-filter: blur(8px);
      border-bottom: 1px solid var(--color-border);
    }
    .topbar-inner {
      max-width: var(--content-width);
      margin: 0 auto;
      padding: 12px 20px;
      display: flex;
      align-items: center;
      gap: 24px;
      flex-wrap: wrap;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      text-decoration: none;
      color: var(--color-ink);
      font: 700 1.2rem var(--font-display);
      margin-right: auto;
    }
    nav {
      display: flex;
      gap: 4px;
      flex-wrap: wrap;
    }
    nav a {
      padding: 6px 12px;
      border-radius: 999px;
      text-decoration: none;
      color: var(--color-ink-muted);
      font-weight: 500;
    }
    nav a:hover {
      color: var(--color-ink);
    }
    nav a.active {
      background: var(--color-primary-soft);
      color: var(--color-primary);
    }
    .account {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  `,
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly user = inject(CurrentUserStore);
}
