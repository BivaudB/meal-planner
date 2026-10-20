import { PercentPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { Recommendation } from '../../core/models';
import { MacrosView } from '../../shared/macros';

@Component({
  selector: 'app-suggestions',
  imports: [RouterLink, PercentPipe, MacrosView],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Suggestions</h1>
          <p>Les recettes que vous pouvez préparer avec vos ingrédients, les plus complètes en premier.</p>
        </div>
        <label class="missing">
          Ingrédients manquants acceptés : {{ maxMissing() }}
          <input type="range" min="0" max="10" [value]="maxMissing()" (change)="setMaxMissing($any($event.target).value)" />
        </label>
      </div>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      @if (loaded() && suggestions().length === 0) {
        <div class="empty">
          Aucune recette ne correspond pour l'instant.
          <a routerLink="/garde-manger">Ajoutez des ingrédients</a> ou acceptez davantage d'ingrédients manquants.
        </div>
      }

      <div class="stack">
        @for (s of suggestions(); track s.recipeId) {
          <a class="card suggestion" [routerLink]="['/recettes', s.recipeId]">
            <div class="head">
              <h3>{{ s.name }}</h3>
              <span class="score">{{ s.coverage | percent: '1.0-0' }}</span>
            </div>
            @if (s.nutrition; as n) {
              <div class="macros">
                <app-macros [macros]="n.perServing ?? n.total" />
                <span class="small muted">{{ n.perServing ? 'par personne' : 'recette entière' }}</span>
              </div>
            }
            <div class="bar"><span [style.width.%]="s.coverage * 100"></span></div>
            <p class="small muted">
              {{ s.availableIngredients }} ingrédient(s) disponible(s) sur {{ s.totalIngredients }}
              @if (!s.mine) {
                · recette partagée
              }
            </p>
            @if (s.missingIngredients.length > 0) {
              <p class="small">Il vous manque : <strong>{{ s.missingIngredients.join(', ') }}</strong></p>
            }
          </a>
        }
      </div>
    </section>
  `,
  styles: `
    .missing {
      font-weight: 500;
      min-width: 260px;
    }
    .suggestion {
      text-decoration: none;
      color: inherit;
      transition: border-color 0.15s;
    }
    .suggestion:hover {
      border-color: var(--color-primary);
    }
    .head {
      display: flex;
      justify-content: space-between;
      gap: 12px;
    }
    .score {
      font: 700 1.3rem var(--font-display);
      color: var(--color-primary);
    }
    .macros {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
      margin-bottom: 6px;
    }
    .bar {
      height: 6px;
      border-radius: 3px;
      background: var(--color-surface-muted);
      overflow: hidden;
      margin: 4px 0 10px;
    }
    .bar span {
      display: block;
      height: 100%;
      background: var(--color-primary);
    }
    .suggestion p {
      margin: 0 0 4px;
    }
  `,
})
export class SuggestionsPage {
  private readonly api = inject(ApiService);

  protected readonly maxMissing = signal(3);
  protected readonly suggestions = signal<Recommendation[]>([]);
  protected readonly loaded = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected setMaxMissing(value: string): void {
    this.maxMissing.set(Number(value));
    this.load();
  }

  private load(): void {
    this.api.recommendations(this.maxMissing()).subscribe({
      next: (list) => {
        this.suggestions.set(list);
        this.loaded.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
