-- Crée la base dédiée à Keycloak au premier démarrage du conteneur PostgreSQL
-- (exécuté uniquement quand le volume de données est vide).
-- Fichier SQL plutôt que script shell : insensible aux fins de ligne Windows (CRLF).
-- Identifiants de développement local, identiques à ceux du service keycloak (docker-compose.yml).
CREATE USER keycloak WITH PASSWORD 'keycloak';
CREATE DATABASE keycloak OWNER keycloak;
