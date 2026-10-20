import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'Meal Planner',
    loadComponent: () => import('./pages/home/home').then((m) => m.HomePage),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: 'recettes',
        title: 'Recettes',
        loadComponent: () => import('./pages/recipes/recipe-list').then((m) => m.RecipeListPage),
      },
      {
        path: 'recettes/nouvelle',
        title: 'Nouvelle recette',
        loadComponent: () => import('./pages/recipes/recipe-edit').then((m) => m.RecipeEditPage),
      },
      {
        path: 'recettes/:id',
        title: 'Recette',
        loadComponent: () => import('./pages/recipes/recipe-detail').then((m) => m.RecipeDetailPage),
      },
      {
        path: 'recettes/:id/modifier',
        title: 'Modifier la recette',
        loadComponent: () => import('./pages/recipes/recipe-edit').then((m) => m.RecipeEditPage),
      },
      {
        path: 'garde-manger',
        title: 'Mes ingrédients',
        loadComponent: () => import('./pages/pantry/pantry').then((m) => m.PantryPage),
      },
      {
        path: 'suggestions',
        title: 'Suggestions',
        loadComponent: () => import('./pages/suggestions/suggestions').then((m) => m.SuggestionsPage),
      },
      {
        path: 'import',
        title: 'Import de recettes',
        loadComponent: () => import('./pages/import/import').then((m) => m.ImportPage),
      },
      {
        path: 'aliments',
        title: 'Aliments',
        loadComponent: () => import('./pages/foods/foods').then((m) => m.FoodsPage),
      },
      {
        path: 'admin',
        title: 'Administration',
        loadComponent: () => import('./pages/admin/admin').then((m) => m.AdminPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
