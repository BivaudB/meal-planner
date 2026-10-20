export const environment = {
  /** Realm Keycloak (serveur d'autorisation OAuth2 / OpenID Connect). */
  authIssuer: 'http://localhost:8180/realms/meal-planner',
  authClientId: 'meal-planner-web',
  /** Les appels /api sont relayés vers le backend (voir proxy.conf.json). */
  apiBaseUrl: '/api',
};
