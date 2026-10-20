import { Injectable, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { filter, take } from 'rxjs';

import { environment } from '../../environments/environment';

/**
 * Connexion OAuth2 / OpenID Connect auprès de Keycloak, flux "Authorization Code + PKCE".
 * Le mot de passe n'est jamais saisi dans l'application : l'utilisateur est redirigé
 * vers la page de connexion Keycloak, puis revient avec un code échangé contre des jetons.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly router = inject(Router);

  private readonly loggedIn = signal(false);
  readonly isLoggedIn = this.loggedIn.asReadonly();
  readonly authServerUnavailable = signal(false);

  private readonly claims = signal<Record<string, unknown> | null>(null);
  readonly username = computed(() => (this.claims()?.['preferred_username'] as string | undefined) ?? '');
  readonly displayName = computed(() => (this.claims()?.['name'] as string | undefined) ?? this.username());

  async init(): Promise<void> {
    this.oauth.configure({
      issuer: environment.authIssuer,
      clientId: environment.authClientId,
      responseType: 'code',
      redirectUri: window.location.origin + '/',
      postLogoutRedirectUri: window.location.origin + '/',
      scope: 'openid profile email',
      requireHttps: false, // développement local uniquement
      showDebugInformation: false,
    });
    this.oauth.events.subscribe(() => this.refreshState());
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
      this.oauth.setupAutomaticSilentRefresh();
    } catch (error) {
      console.error('Keycloak injoignable', error);
      this.authServerUnavailable.set(true);
    }
    this.refreshState();

    // Retour de connexion : on renvoie l'utilisateur vers la page qu'il demandait,
    // une fois la navigation initiale du routeur terminée.
    const state = this.oauth.state;
    if (this.loggedIn() && state && state !== '/' && state !== '%2F') {
      this.router.events
        .pipe(
          filter((event) => event instanceof NavigationEnd),
          take(1),
        )
        .subscribe(() => this.router.navigateByUrl(decodeURIComponent(state)));
    }
  }

  login(returnUrl = '/'): void {
    this.oauth.initCodeFlow(encodeURIComponent(returnUrl));
  }

  logout(): void {
    this.oauth.logOut();
  }

  accessToken(): string | null {
    return this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
  }

  private refreshState(): void {
    const valid = this.oauth.hasValidAccessToken();
    this.loggedIn.set(valid);
    this.claims.set(valid ? (this.oauth.getIdentityClaims() as Record<string, unknown>) : null);
  }
}
