import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { FoodSummary, RecipeDetail, RecipeRequest, UNITS, Visibility } from '../../core/models';
import { FoodPicker } from '../../shared/food-picker';

interface IngredientForm {
  key: number;
  name: string;
  quantity: number | null;
  unit: string | null;
  foodId: number | null;
  foodName: string | null;
}

interface StepForm {
  key: number;
  text: string;
}

let nextKey = 1;

@Component({
  selector: 'app-recipe-edit',
  imports: [FormsModule, RouterLink, FoodPicker],
  template: `
    <section class="page">
      <div class="page-header">
        <h1>{{ id() ? 'Modifier la recette' : 'Nouvelle recette' }}</h1>
      </div>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      <form class="stack" (ngSubmit)="save()">
        <div class="card stack">
          <label>
            Nom de la recette
            <input name="name" [(ngModel)]="name" required maxlength="255" />
          </label>
          <label>
            Description
            <textarea name="description" [(ngModel)]="description" rows="3"></textarea>
          </label>
          <div class="fields">
            <label>
              Personnes
              <input type="number" name="servings" min="1" max="100" [(ngModel)]="servings" />
            </label>
            <label>
              Préparation (min)
              <input type="number" name="prep" min="0" [(ngModel)]="prepMinutes" />
            </label>
            <label>
              Cuisson (min)
              <input type="number" name="cook" min="0" [(ngModel)]="cookMinutes" />
            </label>
            <label>
              Visibilité
              <select name="visibility" [(ngModel)]="visibility">
                <option value="PRIVATE">Privée</option>
                <option value="PUBLIC">Publique (partagée)</option>
              </select>
            </label>
          </div>
        </div>

        <div class="card stack">
          <div class="row spread">
            <h2>Ingrédients</h2>
            <button type="button" class="btn btn-sm" (click)="addIngredient()">+ Ingrédient</button>
          </div>
          @for (ingredient of ingredients(); track ingredient.key; let i = $index) {
            <div class="ingredient">
              <input
                class="qty"
                type="number"
                step="any"
                min="0"
                placeholder="Qté"
                [name]="'qty' + ingredient.key"
                [(ngModel)]="ingredient.quantity"
              />
              <select [name]="'unit' + ingredient.key" [(ngModel)]="ingredient.unit">
                <option [ngValue]="null">—</option>
                @for (u of units; track u.value) {
                  <option [ngValue]="u.value">{{ u.label }}</option>
                }
              </select>
              <input
                placeholder="Ingrédient"
                [name]="'name' + ingredient.key"
                [(ngModel)]="ingredient.name"
                required
              />
              <div class="food">
                <app-food-picker
                  [initialText]="ingredient.foodName ?? ''"
                  placeholder="Aliment Ciqual (auto si vide)"
                  (picked)="linkFood(ingredient, $event)"
                  (typed)="unlinkIfCleared(ingredient, $event)"
                />
              </div>
              <button type="button" class="btn btn-ghost btn-sm" (click)="removeIngredient(i)" title="Retirer">
                ✕
              </button>
            </div>
          } @empty {
            <p class="muted">Aucun ingrédient.</p>
          }
          <p class="small muted">
            Si aucun aliment Ciqual n'est choisi, le rattachement est proposé automatiquement à l'enregistrement.
          </p>
        </div>

        <div class="card stack">
          <div class="row spread">
            <h2>Étapes de préparation</h2>
            <button type="button" class="btn btn-sm" (click)="addStep()">+ Étape</button>
          </div>
          @for (step of steps(); track step.key; let i = $index) {
            <div class="step">
              <span class="step-number">{{ i + 1 }}</span>
              <textarea rows="2" [name]="'step' + step.key" [(ngModel)]="step.text"></textarea>
              <button type="button" class="btn btn-ghost btn-sm" (click)="removeStep(i)" title="Retirer">✕</button>
            </div>
          } @empty {
            <p class="muted">Aucune étape pour l'instant.</p>
          }
        </div>

        <div class="row">
          <button type="submit" class="btn btn-primary" [disabled]="saving() || !name.trim()">
            {{ saving() ? 'Enregistrement…' : 'Enregistrer' }}
          </button>
          <a class="btn btn-ghost" [routerLink]="id() ? ['/recettes', id()] : ['/recettes']">Annuler</a>
        </div>
      </form>
    </section>
  `,
  styles: `
    .fields {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 12px;
    }
    .spread {
      justify-content: space-between;
    }
    .spread h2 {
      margin: 0;
    }
    .ingredient {
      display: grid;
      grid-template-columns: 80px 130px minmax(140px, 1fr) minmax(180px, 1.3fr) auto;
      gap: 8px;
      align-items: center;
    }
    @media (max-width: 760px) {
      .ingredient {
        grid-template-columns: 80px 1fr auto;
      }
      .ingredient .food {
        grid-column: 1 / -1;
      }
    }
    .step {
      display: grid;
      grid-template-columns: 28px 1fr auto;
      gap: 8px;
      align-items: start;
    }
    .step-number {
      display: grid;
      place-items: center;
      width: 28px;
      height: 28px;
      margin-top: 6px;
      border-radius: 50%;
      background: var(--color-primary-soft);
      color: var(--color-primary);
      font-weight: 600;
      font-size: 0.85rem;
    }
  `,
})
export class RecipeEditPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  /** Paramètre de route :id, absent pour une nouvelle recette. */
  readonly id = input<string>();

  protected readonly units = UNITS;
  protected name = '';
  protected description = '';
  protected servings: number | null = null;
  protected prepMinutes: number | null = null;
  protected cookMinutes: number | null = null;
  protected visibility: Visibility = 'PRIVATE';
  private sourceUrl: string | null = null;
  protected readonly ingredients = signal<IngredientForm[]>([]);
  protected readonly steps = signal<StepForm[]>([]);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      if (!id) {
        this.addIngredient();
        return;
      }
      this.api.recipe(Number(id)).subscribe({
        next: (recipe) => this.fill(recipe),
        error: (e) => this.error.set(errorMessage(e)),
      });
    });
  }

  private fill(recipe: RecipeDetail): void {
    this.name = recipe.name;
    this.description = recipe.description ?? '';
    this.servings = recipe.servings;
    this.prepMinutes = recipe.prepMinutes;
    this.cookMinutes = recipe.cookMinutes;
    this.visibility = recipe.visibility;
    this.sourceUrl = recipe.sourceUrl;
    this.ingredients.set(
      recipe.ingredients.map((i) => ({
        key: nextKey++,
        name: i.name,
        quantity: i.quantity,
        unit: i.unit,
        foodId: i.foodId,
        foodName: i.foodName,
      })),
    );
    this.steps.set(recipe.steps.map((text) => ({ key: nextKey++, text })));
  }

  protected addIngredient(): void {
    this.ingredients.update((list) => [
      ...list,
      { key: nextKey++, name: '', quantity: null, unit: null, foodId: null, foodName: null },
    ]);
  }

  protected removeIngredient(index: number): void {
    this.ingredients.update((list) => list.filter((_, i) => i !== index));
  }

  protected linkFood(ingredient: IngredientForm, food: FoodSummary): void {
    ingredient.foodId = food.id;
    ingredient.foodName = food.name;
    if (!ingredient.name.trim()) {
      ingredient.name = food.name.split(',')[0];
    }
  }

  protected unlinkIfCleared(ingredient: IngredientForm, text: string): void {
    if (!text.trim()) {
      ingredient.foodId = null;
      ingredient.foodName = null;
    }
  }

  protected addStep(): void {
    this.steps.update((list) => [...list, { key: nextKey++, text: '' }]);
  }

  protected removeStep(index: number): void {
    this.steps.update((list) => list.filter((_, i) => i !== index));
  }

  protected save(): void {
    const request: RecipeRequest = {
      name: this.name.trim(),
      description: this.description.trim() || null,
      servings: this.servings,
      prepMinutes: this.prepMinutes,
      cookMinutes: this.cookMinutes,
      visibility: this.visibility,
      sourceUrl: this.sourceUrl,
      ingredients: this.ingredients()
        .filter((i) => i.name.trim())
        .map((i) => ({ name: i.name.trim(), quantity: i.quantity, unit: i.unit, foodId: i.foodId })),
      steps: this.steps()
        .map((s) => s.text.trim())
        .filter((text) => text.length > 0),
    };
    const id = this.id();
    const call = id ? this.api.updateRecipe(Number(id), request) : this.api.createRecipe(request);
    this.saving.set(true);
    call.subscribe({
      next: (recipe) => this.router.navigate(['/recettes', recipe.id]),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.saving.set(false);
      },
    });
  }
}
