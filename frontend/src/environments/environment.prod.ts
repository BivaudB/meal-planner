/**
 * Production (ng build, configuration "production") : le front, l'API (/api) et Keycloak (/auth)
 * sont servis par Caddy sur le même domaine. Les URL se déduisent donc de l'origine de la page :
 * aucune valeur propre au déploiement n'est compilée dans l'application.
 */
export const environment = {
  authIssuer: `${window.location.origin}/auth/realms/meal-planner`,
  authClientId: 'meal-planner-web',
  apiBaseUrl: '/api',
};
