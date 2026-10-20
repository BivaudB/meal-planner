import { DecimalPipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { RecipeDetail, unitLabel } from '../../core/models';

@Component({
  selector: 'app-recipe-detail',
  imports: [RouterLink, DecimalPipe],
  template: `
    <section class="page">
      <a routerLink="/recettes" class="back small">← Toutes les recettes</a>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      @if (recipe(); as r) {
        <div class="page-header">
          <div>
            <h1>{{ r.name }}</h1>
            <div class="row small muted">
              @if (r.servings) {
                <span>{{ r.servings }} personne{{ r.servings > 1 ? 's' : '' }}</span>
              }
              @if (r.prepMinutes) {
                <span>· Préparation {{ r.prepMinutes }} min</span>
              }
              @if (r.cookMinutes) {
                <span>· Cuisson {{ r.cookMinutes }} min</span>
              }
              <span class="badge" [class.badge-primary]="r.visibility === 'PUBLIC'">
                {{ r.visibility === 'PUBLIC' ? 'Publique' : 'Privée' }}
              </span>
            </div>
          </div>
          @if (r.mine) {
            <div class="row">
              <a class="btn" [routerLink]="['/recettes', r.id, 'modifier']">Modifier</a>
              <button class="btn btn-danger" (click)="remove(r)">Supprimer</button>
            </div>
          }
        </div>

        @if (r.description) {
          <p class="description">{{ r.description }}</p>
        }

        <div class="layout">
          <div class="card">
            <h2>Ingrédients</h2>
            <table>
              <tbody>
                @for (i of r.ingredients; track i.id) {
                  <tr>
                    <td class="qty">
                      @if (i.quantity !== null) {
                        {{ i.quantity | number: '1.0-2' }} {{ unit(i.unit) }}
                      } @else {
                        <span class="muted">{{ i.rawQuantity ?? '—' }}</span>
                      }
                    </td>
                    <td>
                      {{ i.name }}
                      @if (i.foodName) {
                        <div class="small muted">Ciqual : {{ i.foodName }}</div>
                      } @else {
                        <div class="small"><span class="badge badge-accent">Non rattaché à Ciqual</span></div>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <aside class="card nutrition">
            <h2>Valeurs nutritionnelles</h2>
            <p class="small muted">Recette entière (estimation).</p>
            <dl>
              <dt>Énergie</dt>
              <dd>{{ r.nutrition.energyKcal | number: '1.0-0' }} kcal</dd>
              <dt>Protéines</dt>
              <dd>{{ r.nutrition.proteinG | number: '1.0-1' }} g</dd>
              <dt>Glucides</dt>
              <dd>{{ r.nutrition.carbohydrateG | number: '1.0-1' }} g</dd>
              <dt>Lipides</dt>
              <dd>{{ r.nutrition.fatG | number: '1.0-1' }} g</dd>
            </dl>
            @if (r.servings && r.servings > 1) {
              <p class="small">Soit environ {{ r.nutrition.energyKcal / r.servings | number: '1.0-0' }} kcal par personne.</p>
            }
            <p class="small muted">
              Calcul basé sur {{ r.nutrition.ingredientsCounted }} ingrédient(s) sur {{ r.nutrition.ingredientsTotal }}
              (quantité convertible en grammes et aliment rattaché à Ciqual).
            </p>
          </aside>
        </div>

        <div class="card steps">
          <h2>Préparation</h2>
          @if (r.steps.length === 0) {
            <div class="empty">
              Les étapes de préparation ne sont pas encore renseignées.
              @if (r.mine) {
                <a [routerLink]="['/recettes', r.id, 'modifier']">Les ajouter</a>
              }
            </div>
          } @else {
            <ol>
              @for (step of r.steps; track $index) {
                <li>{{ step }}</li>
              }
            </ol>
          }
        </div>
      }
    </section>
  `,
  styles: `
    .back {
      display: inline-block;
      margin-bottom: 16px;
      text-decoration: none;
    }
    .description {
      max-width: 720px;
      color: var(--color-ink-muted);
    }
    .layout {
      display: grid;
      grid-template-columns: minmax(0, 2fr) minmax(240px, 1fr);
      gap: 16px;
      align-items: start;
      margin-bottom: 16px;
    }
    @media (max-width: 760px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }
    .qty {
      white-space: nowrap;
      font-weight: 600;
      width: 1%;
    }
    dl {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 6px 12px;
      margin: 0 0 12px;
    }
    dt {
      color: var(--color-ink-muted);
    }
    dd {
      margin: 0;
      font-weight: 600;
      text-align: right;
    }
    ol {
      padding-left: 20px;
      margin: 0;
    }
    ol li {
      margin-bottom: 10px;
    }
  `,
})
export class RecipeDetailPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  /** Paramètre de route :id (liaison via withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly recipe = signal<RecipeDetail | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly unit = unitLabel;

  constructor() {
    effect(() => {
      this.api.recipe(Number(this.id())).subscribe({
        next: (recipe) => this.recipe.set(recipe),
        error: (e) => this.error.set(errorMessage(e)),
      });
    });
  }

  protected remove(recipe: RecipeDetail): void {
    if (!confirm(`Supprimer la recette « ${recipe.name} » ?`)) {
      return;
    }
    this.api.deleteRecipe(recipe.id).subscribe({
      next: () => this.router.navigateByUrl('/recettes'),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
