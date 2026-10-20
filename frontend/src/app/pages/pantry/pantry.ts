import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { FoodSummary, PantryItem } from '../../core/models';
import { FoodPicker } from '../../shared/food-picker';

@Component({
  selector: 'app-pantry',
  imports: [FormsModule, RouterLink, FoodPicker],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Mes ingrédients</h1>
          <p>Ce que vous avez sous la main. Les suggestions de recettes s'appuient sur cette liste.</p>
        </div>
        <a class="btn btn-primary" routerLink="/suggestions">Voir les suggestions</a>
      </div>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      <div class="card add">
        <div class="picker">
          <app-food-picker
            placeholder="Ajouter un ingrédient (ex. œufs, riz, tomate)"
            (picked)="addFood($event)"
            (typed)="freeText = $event"
          />
        </div>
        <button class="btn" [disabled]="!freeText.trim()" (click)="addFreeText()">Ajouter tel quel</button>
      </div>

      @if (items().length === 0) {
        <div class="empty">Votre garde-manger est vide.</div>
      } @else {
        <ul class="chips">
          @for (item of items(); track item.id) {
            <li class="chip" [class.unlinked]="!item.foodId">
              <span>{{ item.name }}</span>
              <button (click)="remove(item)" title="Retirer">✕</button>
            </li>
          }
        </ul>
        <button class="btn btn-sm btn-danger" (click)="clear()">Tout vider</button>
      }
    </section>
  `,
  styles: `
    .add {
      display: flex;
      gap: 10px;
      align-items: center;
      margin-bottom: 24px;
    }
    .picker {
      flex: 1;
    }
    .chips {
      list-style: none;
      padding: 0;
      margin: 0 0 20px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    .chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 8px 6px 14px;
      border-radius: 999px;
      background: var(--color-primary-soft);
      color: var(--color-primary);
      font-weight: 500;
    }
    .chip.unlinked {
      background: var(--color-accent-soft);
      color: #9c4a1c;
    }
    .chip button {
      border: 0;
      background: transparent;
      color: inherit;
      cursor: pointer;
      font-size: 0.8rem;
      opacity: 0.7;
    }
  `,
})
export class PantryPage {
  private readonly api = inject(ApiService);
  private readonly picker = viewChild.required(FoodPicker);

  protected readonly items = signal<PantryItem[]>([]);
  protected readonly error = signal<string | null>(null);
  protected freeText = '';

  constructor() {
    this.reload();
  }

  protected addFood(food: FoodSummary): void {
    // Libellé court : "Tomate, crue" -> "Tomate"
    this.add({ name: food.name.split(',')[0], foodId: food.id, quantity: null, unit: null });
  }

  protected addFreeText(): void {
    this.add({ name: this.freeText.trim(), foodId: null, quantity: null, unit: null });
  }

  private add(item: Omit<PantryItem, 'id'>): void {
    this.api.addPantryItem(item).subscribe({
      next: () => {
        this.freeText = '';
        this.picker().clear();
        this.reload();
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected remove(item: PantryItem): void {
    this.api.removePantryItem(item.id).subscribe({
      next: () => this.reload(),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  protected clear(): void {
    if (confirm('Vider tout le garde-manger ?')) {
      this.api.clearPantry().subscribe(() => this.reload());
    }
  }

  private reload(): void {
    this.api.pantry().subscribe({
      next: (items) => this.items.set(items),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
