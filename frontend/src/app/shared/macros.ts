import { DecimalPipe } from '@angular/common';
import { Component, LOCALE_ID, computed, inject, input } from '@angular/core';

import { Macros } from '../core/models';

/**
 * Les quatre valeurs mises en avant partout (aliments, recettes, suggestions) :
 * calories, protéines, glucides et lipides.
 * - `large` : tuiles, pour les fiches (aliment, recette) ;
 * - `compact` : une ligne, pour les listes et les cartes.
 */
@Component({
  selector: 'app-macros',
  template: `
    @if (size() === 'large') {
      <div class="tiles">
        <div class="tile kcal">
          <span>Calories</span>
          <strong>{{ fmt(m().energyKcal, '1.0-0') }}<small> kcal</small></strong>
        </div>
        <div class="tile protein">
          <span>Protéines</span>
          <strong>{{ fmt(m().proteinG) }}<small> g</small></strong>
        </div>
        <div class="tile carbs">
          <span>Glucides</span>
          <strong>{{ fmt(m().carbohydrateG) }}<small> g</small></strong>
        </div>
        <div class="tile fat">
          <span>Lipides</span>
          <strong>{{ fmt(m().fatG) }}<small> g</small></strong>
        </div>
      </div>
      @if (energyShare(); as share) {
        <div class="split" title="Répartition de l'énergie entre protéines, glucides et lipides">
          <span class="protein" [style.width.%]="share.protein"></span>
          <span class="carbs" [style.width.%]="share.carbs"></span>
          <span class="fat" [style.width.%]="share.fat"></span>
        </div>
      }
    } @else {
      <div class="line">
        <strong class="kcal">{{ fmt(m().energyKcal, '1.0-0') }} kcal</strong>
        <span class="chip protein" title="Protéines">P {{ fmt(m().proteinG, '1.0-0') }} g</span>
        <span class="chip carbs" title="Glucides">G {{ fmt(m().carbohydrateG, '1.0-0') }} g</span>
        <span class="chip fat" title="Lipides">L {{ fmt(m().fatG, '1.0-0') }} g</span>
      </div>
    }
  `,
  styles: `
    .tiles {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
      gap: 10px;
    }
    .tile {
      display: flex;
      flex-direction: column;
      padding: 12px;
      border-radius: var(--radius-sm);
      background: var(--color-surface-muted);
      border-top: 3px solid var(--color-ink);
    }
    .tile span {
      font-size: 0.8rem;
      color: var(--color-ink-muted);
    }
    .tile strong {
      font: 600 1.35rem var(--font-display);
    }
    .tile small {
      font: 400 0.8rem var(--font-body);
      color: var(--color-ink-muted);
    }
    .tile.protein {
      border-top-color: var(--color-protein);
    }
    .tile.carbs {
      border-top-color: var(--color-carbs);
    }
    .tile.fat {
      border-top-color: var(--color-fat);
    }
    .split {
      display: flex;
      height: 6px;
      margin-top: 10px;
      border-radius: 3px;
      overflow: hidden;
      background: var(--color-surface-muted);
    }
    .split .protein {
      background: var(--color-protein);
    }
    .split .carbs {
      background: var(--color-carbs);
    }
    .split .fat {
      background: var(--color-fat);
    }
    .line {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 6px;
      font-size: 0.85rem;
    }
    .chip {
      padding: 1px 8px;
      border-radius: 999px;
      background: var(--color-surface-muted);
      border-left: 3px solid;
      white-space: nowrap;
    }
    .chip.protein {
      border-color: var(--color-protein);
    }
    .chip.carbs {
      border-color: var(--color-carbs);
    }
    .chip.fat {
      border-color: var(--color-fat);
    }
  `,
})
export class MacrosView {
  private readonly decimal = new DecimalPipe(inject(LOCALE_ID));

  /** Valeurs à afficher (null = non renseigné). */
  readonly macros = input.required<Partial<Macros> | null>();
  readonly size = input<'large' | 'compact'>('compact');

  protected readonly m = computed(() => this.macros() ?? {});

  /** Part de l'énergie apportée par chaque macronutriment (4 / 4 / 9 kcal par gramme). */
  protected readonly energyShare = computed(() => {
    const protein = (this.m().proteinG ?? 0) * 4;
    const carbs = (this.m().carbohydrateG ?? 0) * 4;
    const fat = (this.m().fatG ?? 0) * 9;
    const total = protein + carbs + fat;
    return total > 0
      ? { protein: (protein / total) * 100, carbs: (carbs / total) * 100, fat: (fat / total) * 100 }
      : null;
  });

  protected fmt(value: number | null | undefined, format = '1.0-1'): string {
    return value === null || value === undefined ? '—' : (this.decimal.transform(value, format) ?? '—');
  }
}
