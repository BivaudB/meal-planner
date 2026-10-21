#!/bin/sh
# Production : crée la base de Keycloak au premier démarrage de PostgreSQL (volume vide).
# Script shell (et non SQL comme en développement) pour lire le mot de passe dans
# KEYCLOAK_DB_PASSWORD. Fins de ligne LF imposées par .gitattributes.
set -eu

psql -v ON_ERROR_STOP=1 -v kc_password="$KEYCLOAK_DB_PASSWORD" \
    --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
CREATE USER keycloak WITH PASSWORD :'kc_password';
CREATE DATABASE keycloak OWNER keycloak;
EOSQL
