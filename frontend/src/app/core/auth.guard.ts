import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { AuthService } from './auth.service';

/** Redirige vers la connexion Keycloak si l'utilisateur n'est pas connecté. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) {
    return true;
  }
  auth.login(state.url);
  return false;
};
