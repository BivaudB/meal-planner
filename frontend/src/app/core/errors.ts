import { HttpErrorResponse } from '@angular/common/http';

/** Message lisible à partir d'une erreur HTTP (le backend renvoie des "Problem Details"). */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Le serveur est injoignable. Le backend est-il démarré ?';
    }
    const detail = (error.error as { detail?: string } | null)?.detail;
    if (detail) {
      return detail;
    }
    if (error.status === 401) {
      return 'Session expirée : reconnectez-vous.';
    }
    if (error.status === 403) {
      return "Vous n'avez pas les droits pour cette action.";
    }
    return `Erreur ${error.status}`;
  }
  return 'Une erreur inattendue est survenue.';
}
