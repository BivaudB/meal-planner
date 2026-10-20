import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  FoodDetail,
  FoodSummary,
  ImportJob,
  Me,
  Page,
  PantryItem,
  RecipeDetail,
  RecipeRequest,
  RecipeSummary,
  Recommendation,
} from './models';

/** Point d'accès unique à l'API REST du backend. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  me(): Observable<Me> {
    return this.http.get<Me>(`${this.base}/me`);
  }

  // --- Recettes -------------------------------------------------------
  myRecipes(q = '', page = 0, size = 24): Observable<Page<RecipeSummary>> {
    return this.http.get<Page<RecipeSummary>>(`${this.base}/recipes`, { params: pageParams(q, page, size) });
  }

  publicRecipes(q = '', page = 0, size = 24): Observable<Page<RecipeSummary>> {
    return this.http.get<Page<RecipeSummary>>(`${this.base}/recipes/public`, { params: pageParams(q, page, size) });
  }

  recipe(id: number): Observable<RecipeDetail> {
    return this.http.get<RecipeDetail>(`${this.base}/recipes/${id}`);
  }

  createRecipe(request: RecipeRequest): Observable<RecipeDetail> {
    return this.http.post<RecipeDetail>(`${this.base}/recipes`, request);
  }

  updateRecipe(id: number, request: RecipeRequest): Observable<RecipeDetail> {
    return this.http.put<RecipeDetail>(`${this.base}/recipes/${id}`, request);
  }

  deleteRecipe(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/recipes/${id}`);
  }

  // --- Référentiel d'aliments (Ciqual) -------------------------------
  searchFoods(q: string, limit = 15): Observable<FoodSummary[]> {
    return this.http.get<FoodSummary[]>(`${this.base}/foods`, { params: { q, limit } });
  }

  food(id: number): Observable<FoodDetail> {
    return this.http.get<FoodDetail>(`${this.base}/foods/${id}`);
  }

  // --- Imports ---------------------------------------------------------
  importRecipes(file: File): Observable<ImportJob> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<ImportJob>(`${this.base}/imports/recipes`, body);
  }

  imports(): Observable<ImportJob[]> {
    return this.http.get<ImportJob[]>(`${this.base}/imports`);
  }

  importJob(id: number): Observable<ImportJob> {
    return this.http.get<ImportJob>(`${this.base}/imports/${id}`);
  }

  importCiqual(file: File): Observable<ImportJob> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<ImportJob>(`${this.base}/admin/imports/ciqual`, body);
  }

  ciqualImports(): Observable<ImportJob[]> {
    return this.http.get<ImportJob[]>(`${this.base}/admin/imports/ciqual`);
  }

  // --- Garde-manger et suggestions ------------------------------------
  pantry(): Observable<PantryItem[]> {
    return this.http.get<PantryItem[]>(`${this.base}/pantry`);
  }

  addPantryItem(item: Omit<PantryItem, 'id'>): Observable<PantryItem> {
    return this.http.post<PantryItem>(`${this.base}/pantry`, item);
  }

  removePantryItem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/pantry/${id}`);
  }

  clearPantry(): Observable<void> {
    return this.http.delete<void>(`${this.base}/pantry`);
  }

  recommendations(maxMissing: number): Observable<Recommendation[]> {
    return this.http.get<Recommendation[]>(`${this.base}/recommendations`, { params: { maxMissing } });
  }
}

function pageParams(q: string, page: number, size: number): HttpParams {
  let params = new HttpParams().set('page', page).set('size', size);
  if (q.trim()) {
    params = params.set('q', q.trim());
  }
  return params;
}
