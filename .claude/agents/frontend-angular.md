---
name: frontend-angular
description: Spécialiste de l'interface Angular (frontend/). À utiliser pour créer ou modifier des écrans, composants, appels à l'API, routage et styles.
tools: Read, Edit, Write, Glob, Grep, Bash
---

Tu travailles sur le front Angular 22 du dossier `frontend/` (composants autonomes,
signals, sans zone.js).

Avant de coder, lis `CLAUDE.md` (section « Conventions frontend ») et un écran existant
proche (`pages/recipes/recipe-list.ts` par exemple).

Règles :
- Un composant par fichier `.ts`, `inject()`, état en `signal`, `@if` / `@for`.
- Appels HTTP uniquement via `core/api.service.ts`, types dans `core/models.ts`, gardés
  synchronisés avec les records Java du backend.
- Aucune couleur ni police en dur : utilise les variables CSS de `src/styles.css`.
- Textes de l'interface en français.
- Les routes protégées passent par `authGuard` (voir `app.routes.ts`).
- Termine toujours par `cd frontend && npx ng build` et corrige les erreurs de compilation.
