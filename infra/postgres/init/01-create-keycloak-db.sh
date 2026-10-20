#!/bin/bash
# Crée la base dédiée à Keycloak au premier démarrage du conteneur PostgreSQL.
# (Ce script n'est exécuté que lorsque le volume de données est vide.)
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE USER ${KEYCLOAK_DB_USER} WITH PASSWORD '${KEYCLOAK_DB_PASSWORD}';
  CREATE DATABASE ${KEYCLOAK_DB_NAME} OWNER ${KEYCLOAK_DB_USER};
EOSQL
