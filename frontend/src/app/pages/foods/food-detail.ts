import { DecimalPipe, Location } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { FoodDetail } from '../../core/models';
import { MacrosView } from '../../shared/macros';

/** Fiche nutritionnelle complète d'un aliment Ciqual (valeurs pour 100 g). */
@Component({
  selector: 'app-food-detail',
  imports: [RouterLink, DecimalPipe, MacrosView],
  template: `
    <section class="page">
      @if (fromCatalog) {
        <button type="button" class="back small" (click)="location.back()">← Retour</button>
      } @else {
        <a routerLink="/aliments" class="back small">← Tous les ingrédients</a>
      }

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      @if (food(); as f) {
        <div class="page-header">
          <div>
            <h1>{{ f.name }}</h1>
            <p class="small">
              {{ f.groupName ?? '' }}
              @if (f.scientificName) {
                · <em>{{ f.scientificName }}</em>
              }
            </p>
          </div>
        </div>

        <div class="card">
          <p class="small muted per">Pour 100 g</p>
          <app-macros [macros]="f" size="large" />
          <div class="key">
            <div><span>dont sucres</span><strong>{{ (f.sugarsG | number: '1.0-2') ?? '—' }} g</strong></div>
            <div><span>dont AG saturés</span><strong>{{ (f.saturatedFatG | number: '1.0-2') ?? '—' }} g</strong></div>
            <div><span>Fibres</span><strong>{{ (f.fiberG | number: '1.0-2') ?? '—' }} g</strong></div>
            <div><span>Sel</span><strong>{{ (f.saltG | number: '1.0-2') ?? '—' }} g</strong></div>
            <div><span>Énergie</span><strong>{{ (f.energyKj | number: '1.0-0') ?? '—' }} kJ</strong></div>
          </div>
          <h2>Composition détaillée</h2>
          <p class="small muted">{{ f.nutrients.length }} constituants, source Ciqual (Anses).</p>
          <table>
            <thead>
              <tr><th>Constituant</th><th class="num">Valeur / 100 g</th></tr>
            </thead>
            <tbody>
              @for (n of f.nutrients; track n.id) {
                <tr>
                  <td>{{ n.name }}</td>
                  <td class="num">
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
        </div>
      }
    </section>
  `,
  styles: `
    .back {
      display: inline-block;
      margin-bottom: 16px;
      padding: 0;
      border: 0;
      background: none;
      font: inherit;
      color: var(--color-primary);
      text-decoration: underline;
      cursor: pointer;
    }
    .per {
      margin: 0 0 6px;
    }
    .key {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
      gap: 8px;
      margin: 12px 0 24px;
    }
    .key div {
      display: flex;
      flex-direction: column;
      padding: 6px 10px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--color-border);
    }
    .key span {
      font-size: 0.75rem;
      color: var(--color-ink-muted);
    }
    .key strong {
      font-weight: 500;
    }
    .num {
      text-align: right;
      white-space: nowrap;
    }
  `,
})
export class FoodDetailPage {
  private readonly api = inject(ApiService);
  protected readonly location = inject(Location);

  /** Paramètre de route :id (liaison via withComponentInputBinding). */
  readonly id = input.required<string>();

  /** Vrai si l'on vient de la liste : le retour arrière conserve alors filtres et tri. */
  protected readonly fromCatalog = inject(Router).currentNavigation()?.previousNavigation != null;

  protected readonly food = signal<FoodDetail | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.api.food(Number(this.id())).subscribe({
        next: (food) => this.food.set(food),
        error: (e) => this.error.set(errorMessage(e)),
      });
    });
  }
}
