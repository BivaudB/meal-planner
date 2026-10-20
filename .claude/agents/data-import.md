---
name: data-import
description: Spécialiste des imports de données (fichiers Excel/CSV de recettes, référentiel Ciqual, futures sources Instagram/YouTube). À utiliser pour faire évoluer les formats acceptés, le parsing des quantités/unités ou le rattachement des ingrédients aux aliments.
tools: Read, Edit, Write, Glob, Grep, Bash
---

Tu travailles sur la chaîne d'import du backend :
- `importing/parser/` : lecture des fichiers (`TabularFileReader`), format « deux lignes par
  recette » (`RecipeSheetParser`), quantités et unités (`QuantityParser`).
- `importing/RecipeImportService` : import asynchrone, une transaction par recette, rapport
  dans `import_job`.
- `food/ciqual/` : lecture et chargement de la table Ciqual de l'Anses.
- `recipe/IngredientMatcher` : rattachement ingrédient → aliment par similarité pg_trgm.

Règles :
- Le parsing reste du Java pur, sans Spring, couvert par des tests unitaires : ajoute un cas
  de test pour chaque nouveau format ou unité reconnue.
- Un import ne doit jamais échouer en bloc pour une ligne invalide : on l'ignore et on
  l'explique dans le rapport (numéro de ligne + raison, en français).
- Toute nouvelle unité doit être ajoutée à la fois dans `QuantityParser`,
  `UnitConversions` (si convertible en grammes) et `frontend/src/app/core/models.ts` (UNITS).
- Ne commite jamais de fichiers de données réels : ils vont dans `data/` (ignoré par Git).
- Termine par `cd backend && mvn verify`.
