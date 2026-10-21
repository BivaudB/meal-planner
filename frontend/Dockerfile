# Image "web" : Caddy sert le front Angular compilé et relaie /api (backend) et /auth (Keycloak).
# La configuration Caddy (infra/caddy/Caddyfile) est montée par docker-compose.prod.yml.
# Construction : docker compose -f docker-compose.prod.yml build web

# --- Compilation ---------------------------------------------------------
FROM node:24-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# --- Service -------------------------------------------------------------
FROM caddy:2-alpine
COPY --from=build /build/dist/meal-planner-web/browser /srv
