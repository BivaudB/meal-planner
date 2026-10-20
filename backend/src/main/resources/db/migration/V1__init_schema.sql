-- =====================================================================
-- Schéma initial : utilisateurs, référentiel Ciqual, recettes, imports,
-- garde-manger (ingrédients disponibles).
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() n'est pas IMMUTABLE : ce wrapper permet de l'utiliser dans un index.
CREATE OR REPLACE FUNCTION f_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

-- Texte normalisé pour la recherche : minuscules, sans accents.
-- Appel qualifié par le schéma : depuis PostgreSQL 17, la création d'index exécute les
-- fonctions avec un search_path restreint (pg_catalog), où f_unaccent serait introuvable.
CREATE OR REPLACE FUNCTION f_normalize(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT pg_catalog.lower(public.f_unaccent($1)) $$;

-- ---------------------------------------------------------------------
-- Utilisateurs (créés à la première requête, à partir du jeton Keycloak)
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id   VARCHAR(64)  NOT NULL UNIQUE,      -- claim "sub" du jeton
    username      VARCHAR(255) NOT NULL,
    email         VARCHAR(255),
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- Référentiel Ciqual (Anses) : aliments et composition nutritionnelle
-- ---------------------------------------------------------------------
CREATE TABLE food_group (
    code        VARCHAR(10) PRIMARY KEY,                 -- ex. 01, 0101, 010101
    level       SMALLINT     NOT NULL,                   -- 1 = groupe, 2 = sous-groupe, 3 = sous-sous-groupe
    parent_code VARCHAR(10) REFERENCES food_group (code),
    name        VARCHAR(255) NOT NULL
);

CREATE TABLE food (
    id              BIGINT PRIMARY KEY,                  -- alim_code Ciqual
    name            VARCHAR(500) NOT NULL,
    scientific_name VARCHAR(500),
    group_code      VARCHAR(10) REFERENCES food_group (code),
    -- Valeurs clés pour 100 g, dénormalisées pour les calculs rapides
    energy_kcal     NUMERIC(10, 3),
    energy_kj       NUMERIC(10, 3),
    protein_g       NUMERIC(10, 3),
    carbohydrate_g  NUMERIC(10, 3),
    sugars_g        NUMERIC(10, 3),
    fat_g           NUMERIC(10, 3),
    saturated_fat_g NUMERIC(10, 3),
    fiber_g         NUMERIC(10, 3),
    salt_g          NUMERIC(10, 3),
    source          VARCHAR(20)  NOT NULL DEFAULT 'CIQUAL',
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX food_name_trgm_idx ON food USING gin (f_normalize(name) gin_trgm_ops);

CREATE TABLE nutrient (
    id          INTEGER PRIMARY KEY,                     -- code ORIGCPCD Ciqual
    infoods_tag VARCHAR(20),
    name        VARCHAR(255) NOT NULL,
    unit        VARCHAR(20),
    sort_order  INTEGER      NOT NULL
);

CREATE TABLE food_nutrient (
    food_id     BIGINT  NOT NULL REFERENCES food (id) ON DELETE CASCADE,
    nutrient_id INTEGER NOT NULL REFERENCES nutrient (id),
    value       NUMERIC(14, 6),                          -- null si non renseigné ("-")
    qualifier   VARCHAR(10),                             -- '<' (inférieur à) ou 'traces'
    raw_value   VARCHAR(50),
    PRIMARY KEY (food_id, nutrient_id)
);

-- ---------------------------------------------------------------------
-- Recettes
-- ---------------------------------------------------------------------
CREATE TABLE recipe (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id     BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    servings     INTEGER,
    prep_minutes INTEGER,
    cook_minutes INTEGER,
    visibility   VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',  -- PRIVATE | PUBLIC
    source       VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',   -- MANUAL | IMPORT | INSTAGRAM | YOUTUBE
    source_url   VARCHAR(1000),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX recipe_owner_idx ON recipe (owner_id);
CREATE INDEX recipe_public_idx ON recipe (visibility) WHERE visibility = 'PUBLIC';
CREATE INDEX recipe_name_trgm_idx ON recipe USING gin (f_normalize(name) gin_trgm_ops);

CREATE TABLE recipe_ingredient (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id    BIGINT       NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position     INTEGER      NOT NULL,
    name         VARCHAR(255) NOT NULL,                  -- nom tel que saisi / importé
    quantity     NUMERIC(12, 3),
    unit         VARCHAR(30),                            -- g, kg, ml, cl, l, cas, cac, piece, pincee...
    raw_quantity VARCHAR(100),                           -- texte d'origine ("2 c. à soupe")
    food_id      BIGINT REFERENCES food (id),            -- aliment Ciqual rattaché (si trouvé)
    match_score  DOUBLE PRECISION
);
CREATE INDEX recipe_ingredient_recipe_idx ON recipe_ingredient (recipe_id);
CREATE INDEX recipe_ingredient_food_idx ON recipe_ingredient (food_id);
CREATE INDEX recipe_ingredient_name_idx ON recipe_ingredient (f_normalize(name));

CREATE TABLE recipe_step (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    recipe_id   BIGINT  NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    instruction TEXT    NOT NULL
);
CREATE INDEX recipe_step_recipe_idx ON recipe_step (recipe_id);

-- ---------------------------------------------------------------------
-- Imports de fichiers (suivi des traitements)
-- ---------------------------------------------------------------------
CREATE TABLE import_job (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        BIGINT REFERENCES app_user (id) ON DELETE CASCADE,
    type           VARCHAR(20)  NOT NULL,                -- RECIPES | CIQUAL
    filename       VARCHAR(255) NOT NULL,
    status         VARCHAR(30)  NOT NULL,                -- PENDING | RUNNING | COMPLETED | COMPLETED_WITH_ERRORS | FAILED
    total_items    INTEGER      NOT NULL DEFAULT 0,
    imported_items INTEGER      NOT NULL DEFAULT 0,
    error_items    INTEGER      NOT NULL DEFAULT 0,
    report         TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    started_at     TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ
);
CREATE INDEX import_job_user_idx ON import_job (user_id, created_at DESC);

-- ---------------------------------------------------------------------
-- Garde-manger : ingrédients dont l'utilisateur dispose
-- ---------------------------------------------------------------------
CREATE TABLE pantry_item (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    food_id    BIGINT REFERENCES food (id),
    quantity   NUMERIC(12, 3),
    unit       VARCHAR(30),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX pantry_item_user_idx ON pantry_item (user_id);
