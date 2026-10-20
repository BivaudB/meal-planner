# Meal Planner — instructions pour Claude Code

Application de planification de repas. Langue du produit, des commentaires et des messages
d'erreur : **français**. Noms de code (classes, variables, tables, endpoints) : **anglais**.
Chemins d'URL du front en français (`/recettes`, `/garde-manger`).

## Structure

- `backend/` : Spring Boot 4.1, Java 25, Maven. Package racine `com.mealplanner`, découpé par
  domaine (`food`, `recipe`, `importing`, `pantry`, `recommendation`, `user`, `config`, `common`).
- `frontend/` : Angular 22, composants autonomes, signals, sans zone.js, sans NgModule.
- `infra/` + `docker-compose.yml` : PostgreSQL 18 et Keycloak 26.8.
- `data/` : fichiers de données locaux (Ciqual…), non versionnés.

## Commandes

```bash
docker compose up -d                 # PostgreSQL (5432) + Keycloak (8180)
cd backend && mvn verify             # compilation + tests
cd backend && mvn spring-boot:run    # API sur 8080
cd frontend && npm install && npm start   # interface sur 4200 (proxy /api -> 8080)
cd frontend && npx ng build          # vérification de compilation du front
```

Après toute modification, vérifier au minimum `mvn verify` (backend) et `npx ng build` (frontend).

## Conventions backend

- **Spring Boot 4** : starters `spring-boot-starter-webmvc`, `spring-boot-starter-flyway`,
  `spring-boot-starter-security-oauth2-resource-server`. Jackson 3 (`tools.jackson.*`) si
  besoin d'accéder à l'ObjectMapper.
- **Schéma** : uniquement via migrations Flyway `backend/src/main/resources/db/migration/V<n>__<description>.sql`.
  Ne jamais modifier une migration déjà appliquée : en ajouter une nouvelle.
  `ddl-auto: validate` : toute entité doit correspondre au schéma.
- Entités JPA : identifiants `Long` en `IDENTITY`, pas de Lombok, pas de relations
  bidirectionnelles inutiles (les liens vers `app_user` sont des `Long ownerId/userId`).
- DTO = `record` Java, regroupés dans un fichier `XxxDtos.java` par domaine. Les entités ne
  sortent jamais des services.
- Requêtes de recherche/agrégation en SQL via `JdbcClient` (pg_trgm, `f_normalize()`),
  le CRUD via Spring Data JPA.
- Utilisateur courant : `CurrentUserService.currentUserId()`. Toujours vérifier la propriété
  d'une ressource avant modification (`ForbiddenException` / `NotFoundException`).
- Erreurs : exceptions de `common/`, converties en Problem Details par `ApiExceptionHandler`.
- Tests : JUnit 5 + AssertJ. La logique de parsing est pure (sans Spring) et testée unitairement.

## Conventions frontend

- Un composant = un fichier `.ts` (template et styles inline tant qu'ils restent courts).
  Noms de fichiers sans suffixe (`recipe-list.ts`, classe `RecipeListPage`).
- État local en `signal`, `inject()` plutôt que constructeur, contrôle de flux `@if/@for`.
- Tous les appels HTTP passent par `core/api.service.ts` ; types dans `core/models.ts`
  (miroir des records Java : les tenir synchronisés).
- Les couleurs/typos passent par les variables CSS de `src/styles.css` (le design sera
  remplacé par une maquette Figma : ne pas coder de couleurs en dur dans les composants).

## Authentification

Keycloak, realm `meal-planner` (fichier `infra/keycloak/realm-meal-planner.json`), client
public `meal-planner-web` (Authorization Code + PKCE). Rôles realm `user` et `admin`,
convertis en `ROLE_USER` / `ROLE_ADMIN` côté Spring. Comptes de démo : demo/demo, admin/admin.

## Domaine

- **Ciqual** (Anses) : référentiel d'aliments, PAS de recettes. Tables `food`, `food_group`,
  `nutrient`, `food_nutrient`. Valeurs pour 100 g ; `"< x"` et `"traces"` → `qualifier`.
- **Import de recettes** : format deux lignes par recette (voir README). Chaque ingrédient
  est rattaché à un `food` par similarité (seuil `app.import.match-threshold`).
- **Unités normalisées** : g, kg, mg, ml, cl, dl, l, cas, cac, pincee, piece, gousse,
  tranche, sachet, botte, verre, boite (voir `QuantityParser` et `UnitConversions`).
- **Suggestions** : classement par part d'ingrédients disponibles dans le garde-manger.

## Agents

Des sous-agents spécialisés sont définis dans `.claude/agents/` : `backend-spring`,
`frontend-angular`, `data-import`, `reviewer`.
