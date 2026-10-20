import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { Page, RecipeSummary } from '../../core/models';

type Scope = 'mine' | 'public';

@Component({
  selector: 'app-recipe-list',
  imports: [RouterLink],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Recettes</h1>
          <p>Vos recettes et celles que les autres utilisateurs ont rendues publiques.</p>
        </div>
        <div class="row">
          <a class="btn" routerLink="/import">Importer un fichier</a>
          <a class="btn btn-primary" routerLink="/recettes/nouvelle">Nouvelle recette</a>
        </div>
      </div>

      <div class="row toolbar">
        <div class="tabs">
          <button [class.active]="scope() === 'mine'" (click)="setScope('mine')">Mes recettes</button>
          <button [class.active]="scope() === 'public'" (click)="setScope('public')">Partagées</button>
        </div>
        <input
          type="search"
          placeholder="Rechercher une recette"
          [value]="query()"
          (input)="search($any($event.target).value)"
        />
      </div>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      @if (page(); as p) {
        @if (p.content.length === 0) {
          <div class="empty">
            @if (scope() === 'mine') {
              Aucune recette pour l'instant. <a routerLink="/import">Importez un fichier</a> ou
              <a routerLink="/recettes/nouvelle">créez votre première recette</a>.
            } @else {
              Aucune recette partagée pour l'instant.
            }
          </div>
        } @else {
          <div class="grid">
            @for (recipe of p.content; track recipe.id) {
              <a class="card recipe" [routerLink]="['/recettes', recipe.id]">
                <h3>{{ recipe.name }}</h3>
                <div class="row small muted">
                  <span>{{ recipe.ingredientCount }} ingrédient{{ recipe.ingredientCount > 1 ? 's' : '' }}</span>
                  @if (recipe.servings) {
                    <span>· {{ recipe.servings }} pers.</span>
                  }
                  @if (totalTime(recipe); as minutes) {
                    <span>· {{ minutes }} min</span>
                  }
                </div>
                <div class="row badges">
                  @if (recipe.visibility === 'PUBLIC') {
                    <span class="badge badge-primary">Publique</span>
                  }
                  @if (!recipe.hasSteps) {
                    <span class="badge badge-accent">Étapes à compléter</span>
                  }
                  @if (recipe.source === 'IMPORT') {
                    <span class="badge">Importée</span>
                  }
                </div>
              </a>
            }
          </div>

          @if (p.totalPages > 1) {
            <div class="row pager">
              <button class="btn btn-sm" [disabled]="p.page === 0" (click)="load(p.page - 1)">Précédent</button>
              <span class="muted small">Page {{ p.page + 1 }} / {{ p.totalPages }}</span>
              <button class="btn btn-sm" [disabled]="p.page + 1 >= p.totalPages" (click)="load(p.page + 1)">
                Suivant
              </button>
            </div>
          }
        }
      }
    </section>
  `,
  styles: `
    .toolbar {
      justify-content: space-between;
      margin-bottom: 20px;
    }
    .toolbar input {
      flex: 0 1 320px;
    }
    .recipe {
      text-decoration: none;
      color: inherit;
      display: flex;
      flex-direction: column;
      gap: 8px;
      transition: border-color 0.15s;
    }
    .recipe:hover {
      border-color: var(--color-primary);
    }
    .recipe h3 {
      margin: 0;
    }
    .badges {
      gap: 6px;
    }
    .pager {
      justify-content: center;
      margin-top: 24px;
    }
  `,
})
export class RecipeListPage {
  private readonly api = inject(ApiService);

  protected readonly scope = signal<Scope>('mine');
  protected readonly query = signal('');
  protected readonly page = signal<Page<RecipeSummary> | null>(null);
  protected readonly error = signal<string | null>(null);
  private searchTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    this.load(0);
  }

  protected setScope(scope: Scope): void {
    this.scope.set(scope);
    this.load(0);
  }

  protected search(value: string): void {
    this.query.set(value);
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(() => this.load(0), 300);
  }

  protected load(pageIndex: number): void {
    const request =
      this.scope() === 'mine'
        ? this.api.myRecipes(this.query(), pageIndex)
        : this.api.publicRecipes(this.query(), pageIndex);
    request.subscribe({
      next: (page) => {
        this.page.set(page);
        this.error.set(null);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected totalTime(recipe: RecipeSummary): number | null {
    const total = (recipe.prepMinutes ?? 0) + (recipe.cookMinutes ?? 0);
    return total > 0 ? total : null;
  }
}
