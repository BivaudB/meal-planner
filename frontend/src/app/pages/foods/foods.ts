import { DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { FoodDetail, FoodSummary } from '../../core/models';
import { FoodPicker } from '../../shared/food-picker';

@Component({
  selector: 'app-foods',
  imports: [FoodPicker, DecimalPipe],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Aliments</h1>
          <p>Table de composition nutritionnelle Ciqual (Anses), valeurs pour 100 g.</p>
        </div>
      </div>

      <div class="card" style="margin-bottom: 16px">
        <app-food-picker placeholder="Rechercher un aliment (ex. lentilles, saumon)" (picked)="show($event)" />
      </div>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      @if (food(); as f) {
        <div class="card">
          <h2>{{ f.name }}</h2>
          <p class="muted small">
            {{ f.groupName ?? '' }}
            @if (f.scientificName) {
              · <em>{{ f.scientificName }}</em>
            }
          </p>
          <div class="key">
            <div><span>Énergie</span><strong>{{ f.energyKcal ?? '—' }} kcal</strong></div>
            <div><span>Protéines</span><strong>{{ f.proteinG ?? '—' }} g</strong></div>
            <div><span>Glucides</span><strong>{{ f.carbohydrateG ?? '—' }} g</strong></div>
            <div><span>Lipides</span><strong>{{ f.fatG ?? '—' }} g</strong></div>
            <div><span>Fibres</span><strong>{{ f.fiberG ?? '—' }} g</strong></div>
            <div><span>Sel</span><strong>{{ f.saltG ?? '—' }} g</strong></div>
          </div>
          <details>
            <summary>Composition détaillée ({{ f.nutrients.length }} constituants)</summary>
            <table>
              <thead>
                <tr><th>Constituant</th><th>Valeur / 100 g</th></tr>
              </thead>
              <tbody>
                @for (n of f.nutrients; track n.id) {
                  <tr>
                    <td>{{ n.name }}</td>
                    <td>
                      @if (n.qualifier === 'traces') {
                        traces
                      } @else if (n.value !== null) {
                        {{ n.qualifier === '<' ? '< ' : '' }}{{ n.value | number: '1.0-4' }} {{ n.unit }}
                      } @else {
                        <span class="muted">—</span>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </details>
        </div>
      }
    </section>
  `,
  styles: `
    .key {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
      gap: 10px;
      margin: 16px 0;
    }
    .key div {
      display: flex;
      flex-direction: column;
      padding: 12px;
      border-radius: var(--radius-sm);
      background: var(--color-surface-muted);
    }
    .key span {
      font-size: 0.8rem;
      color: var(--color-ink-muted);
    }
    .key strong {
      font: 600 1.2rem var(--font-display);
    }
    summary {
      cursor: pointer;
      font-weight: 500;
      margin-bottom: 8px;
    }
  `,
})
export class FoodsPage {
  private readonly api = inject(ApiService);

  protected readonly food = signal<FoodDetail | null>(null);
  protected readonly error = signal<string | null>(null);

  protected show(summary: FoodSummary): void {
    this.api.food(summary.id).subscribe({
      next: (food) => this.food.set(food),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
