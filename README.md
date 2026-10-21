# Meal Planner

Application de planification de repas : import de recettes depuis Excel/CSV, référentiel
nutritionnel Ciqual, garde-manger et suggestions de recettes selon les ingrédients disponibles.

| Brique            | Technologie                                             | Port  |
|-------------------|---------------------------------------------------------|-------|
| Base de données   | PostgreSQL 18 (Docker)                                  | 5432  |
| Authentification  | Keycloak 26.8, OAuth2 / OpenID Connect (Docker)         | 8180  |
| API               | Spring Boot 4.1, Java 25, Maven (`backend/`)            | 8080  |
| Interface         | Angular 22 (`frontend/`)                                | 4200  |

## Prérequis

- Docker (Docker Desktop ou équivalent)
- JDK 25 ou plus récent (le projet cible Java 25)
- Maven 3.9+ (ou celui intégré à IntelliJ IDEA)
- Node.js 24.15+ (ou 22.22+)

## Démarrage

```bash
# 1. Infrastructure : PostgreSQL + Keycloak (le realm "meal-planner" est importé automatiquement)
cp .env.example .env
docker compose up -d

# 2. Backend (crée le schéma via Flyway au premier lancement)
cd backend
mvn spring-boot:run

# 3. Frontend (dans un autre terminal)
cd frontend
npm install
npm start
```

Ouvrez http://localhost:4200 et connectez-vous avec un compte de démonstration :

| Utilisateur | Mot de passe | Rôles         |
|-------------|--------------|---------------|
| `demo`      | `demo`       | user          |
| `admin`     | `admin`      | user, admin   |

La page de connexion Keycloak permet aussi de créer un compte. Console d'administration
Keycloak : http://localhost:8180 (admin / admin).

### Données fournies : référentiel Ciqual et recettes de base

Rien à faire : la base est remplie automatiquement au premier lancement.

1. `docker compose up -d` lance aussi le service `ciqual-download`, qui télécharge une fois
   la table Ciqual 2025 de l'Anses (≈ 1,5 Mo, [entrepôt Recherche Data Gouv](https://doi.org/10.57745/RDMHWY))
   dans `data/ciqual.xlsx` (ignoré par Git). Supprimez ce fichier pour forcer un nouveau
   téléchargement ; `CIQUAL_URL` permet de changer de source.
2. Au démarrage, le backend importe `data/ciqual.xlsx` si la table des aliments est vide
   (3 484 aliments, quelques secondes). Autre fichier : variable `CIQUAL_IMPORT_PATH`.
3. Il ajoute ensuite les **recettes de base** de `backend/src/main/resources/seed/recipes.json`
   (une vingtaine de plats courants avec étapes), en recettes publiques visibles de tous
   (onglet *Partagées*) et donc proposées dans les *Suggestions*. Chaque ingrédient y est
   rattaché à son code Ciqual (`ciqual`) ; une recette déjà présente n'est pas modifiée,
   compléter le fichier suffit pour en ajouter. Désactivation : `RECIPE_CATALOG_ENABLED=false`.

Il reste possible de recharger Ciqual depuis l'application : connecté en `admin`, menu
*Admin* → choisir le fichier `Table_Ciqual_2025_FR_*.xlsx` → *Charger*.

### Valeurs nutritionnelles

Calories, protéines, glucides et lipides sont affichés en priorité partout : pour 100 g sur
les aliments (recherche et fiche), par personne sur les recettes (liste, fiche, suggestions),
avec le total de la recette entière.

## Format d'import des recettes

Excel (.xlsx, .xls) ou CSV (séparateur `;` recommandé), **deux lignes par recette** :

| Omelette | Oeufs | Lait | Sel      |
|----------|-------|------|----------|
|          | 3     | 5 cl | 1 pincée |

- Ligne 1 : nom de la recette, puis un ingrédient par colonne.
- Ligne 2 : première case vide, puis la quantité de chaque ingrédient, unité comprise
  (`200 g`, `1,5 kg`, `20 cl`, `2 c. à soupe`, `1 pincée`, `3`…).
- Les cases `…` et les lignes vides sont ignorées.
- Chaque ingrédient est rattaché automatiquement à l'aliment Ciqual le plus proche
  (recherche approximative, insensible aux accents). Le rattachement est modifiable ensuite.
- Les étapes de préparation ne font pas partie du fichier : elles se saisissent dans la
  fiche de chaque recette.

## Architecture

```
meal-planner/
├── docker-compose.yml          PostgreSQL + Keycloak
├── infra/
│   ├── keycloak/               realm importé au démarrage (clients, rôles, comptes de démo)
│   └── postgres/init/          création de la base Keycloak
├── backend/                    API Spring Boot
│   └── src/main/java/com/mealplanner/
│       ├── config/             sécurité (resource server JWT), CORS, propriétés
│       ├── user/               utilisateur courant (créé à la 1re connexion)
│       ├── food/               référentiel Ciqual : recherche, détail, import
│       ├── recipe/             recettes, ingrédients, étapes, calcul nutritionnel
│       ├── importing/          import Excel/CSV en tâche de fond + suivi
│       ├── pantry/             garde-manger
│       └── recommendation/     suggestions selon le garde-manger
└── frontend/                   Angular (composants autonomes, signals, sans zone.js)
    └── src/app/
        ├── core/               authentification OIDC, client API, modèles
        ├── shared/             composants réutilisables
        └── pages/              écrans
```

### Choix techniques

- **PostgreSQL** plutôt que MySQL : les extensions `pg_trgm` et `unaccent` font la recherche
  approximative des aliments (« tomates » → « Tomate, crue ») directement en base, avec index.
- **Keycloak** comme serveur d'autorisation : comptes, connexion, inscription, mot de passe
  oublié et connexion Google éventuelle sont gérés hors de l'application. L'API vérifie
  seulement les jetons JWT ; le front utilise le flux Authorization Code + PKCE.
- **Import dans le backend** (pas de service séparé) : volumes faibles, traitement asynchrone
  avec suivi dans `import_job`. Le module `importing` est isolé pour pouvoir être extrait
  plus tard (par exemple pour le scraping Instagram/YouTube).
- **Flyway** gère le schéma (`backend/src/main/resources/db/migration`) ; Hibernate se
  contente de le valider.

## API (aperçu)

| Méthode | Chemin                         | Description                                   |
|---------|--------------------------------|-----------------------------------------------|
| GET     | `/api/me`                      | Profil de l'utilisateur connecté              |
| GET     | `/api/recipes`                 | Mes recettes (`q`, `page`, `size`)            |
| GET     | `/api/recipes/public`          | Recettes partagées                            |
| GET     | `/api/recipes/{id}`            | Détail + valeurs nutritionnelles              |
| POST/PUT/DELETE | `/api/recipes[/{id}]`  | Création, modification, suppression           |
| GET     | `/api/foods?q=`                | Recherche d'aliments Ciqual (`basic` : ingrédients crus de base) |
| GET     | `/api/foods/catalog`           | Catalogue paginé (`q`, `group`, `basic`, `sort`, `direction`) |
| GET     | `/api/foods/categories`        | Groupes et sous-groupes Ciqual (`basic`)      |
| GET     | `/api/foods/{id}`              | Composition détaillée d'un aliment            |
| POST    | `/api/imports/recipes`         | Import d'un fichier (multipart `file`)        |
| GET     | `/api/imports[/{id}]`          | Suivi des imports                             |
| GET/POST/DELETE | `/api/pantry[/{id}]`   | Garde-manger                                  |
| GET     | `/api/recommendations`         | Suggestions (`maxMissing`, `limit`)           |
| POST    | `/api/admin/imports/ciqual`    | Chargement Ciqual (rôle admin)                |

## Déploiement

`docker-compose.prod.yml` lance toute l'application sur un seul serveur, derrière Caddy
(HTTPS automatique, un seul domaine : `/`, `/api`, `/auth`). Procédure complète pour une VM
gratuite Oracle Cloud : [docs/deploiement.md](docs/deploiement.md).

```bash
cp .env.prod.example .env.prod   # domaine, mots de passe
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## Feuille de route

1. **V1 (ce dépôt)** : import, référentiel Ciqual, recettes, garde-manger, suggestions.
2. Préférences alimentaires (aliments exclus, allergies) prises en compte dans les suggestions.
3. Régimes et objectifs (perte de poids, prise de muscle) : cibles nutritionnelles par repas.
4. Planning de la semaine et liste de courses.
5. Récupération de recettes depuis Instagram / YouTube (description, sous-titres), avec
   extraction des ingrédients et étapes par un modèle de langage.

## Développer avec Claude Code

Le fichier `CLAUDE.md` décrit les conventions du projet et `.claude/agents/` contient des
agents spécialisés (backend, frontend, import de données, relecture). Lancez `claude` à la
racine du dépôt.
