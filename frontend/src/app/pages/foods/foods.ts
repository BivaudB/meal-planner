import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, LOCALE_ID, booleanAttribute, computed, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { CatalogSort, FoodCatalogItem, FoodCategory, Page } from '../../core/models';

const PAGE_SIZE = 50;

const SORTS: { key: CatalogSort; label: string; unit: string }[] = [
  { key: 'energyKcal', label: 'Calories', unit: 'kcal' },
  { key: 'proteinG', label: 'Protéines', unit: 'g' },
  { key: 'carbohydrateG', label: 'Glucides', unit: 'g' },
  { key: 'fatG', label: 'Lipides', unit: 'g' },
];

/**
 * Catalogue des ingrédients (référentiel Ciqual) : filtre par catégorie (groupe Ciqual)
 * et sous-catégorie (sous-groupe), tri par nom ou par valeur nutritionnelle.
 * Les filtres vivent dans l'URL : le retour depuis une fiche les conserve.
 */
@Component({
  selector: 'app-foods',
  imports: [RouterLink],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Ingrédients</h1>
          <p>Table de composition nutritionnelle Ciqual (Anses), valeurs pour 100 g.</p>
        </div>
      </div>

      <div class="card filters">
        <input
          type="search"
          placeholder="Filtrer par nom (ex. lentilles, saumon)"
          [value]="q() ?? ''"
          (input)="typed.next($any($event.target).value)"
          aria-label="Filtrer par nom"
        />
        <select
          (change)="update({ categorie: $any($event.target).value || null })"
          aria-label="Catégorie"
        >
          <option value="">Toutes les catégories</option>
          @for (c of categories(); track c.code) {
            <option [value]="c.code" [selected]="c.code === categoryCode()">{{ capitalize(c.name) }} ({{ c.foodCount }})</option>
          }
        </select>
        <select
          [disabled]="subcategories().length === 0"
          (change)="update({ categorie: $any($event.target).value || categoryCode() || null })"
          aria-label="Sous-catégorie"
        >
          <option value="">Toutes les sous-catégories</option>
          @for (s of subcategories(); track s.code) {
            <option [value]="s.code" [selected]="s.code === subcategoryCode()">{{ capitalize(s.name) }} ({{ s.foodCount }})</option>
          }
        </select>
        <label class="toggle">
          <input type="checkbox" [checked]="!tout()" (change)="toggleBasic($any($event.target).checked)" />
          Ingrédients de base uniquement
        </label>
      </div>

      @if (error()) {
        <div class="alert alert-danger">{{ error() }}</div>
      }

      @if (page(); as p) {
        <p class="small muted count">
          {{ p.totalElements }} ingrédient{{ p.totalElements > 1 ? 's' : '' }}
          @if (sort() !== 'name') {
            · triés par {{ sortLabel() }} {{ direction() === 'desc' ? 'décroissants' : 'croissants' }}
          }
        </p>

        @if (p.content.length === 0) {
          <div class="empty">Aucun ingrédient ne correspond à ces critères.</div>
        } @else {
          <div class="card table-card">
            <table>
              <thead>
                <tr>
                  <th [attr.aria-sort]="ariaSort('name')">
                    <button type="button" (click)="sortBy('name')">Nom {{ arrow('name') }}</button>
                  </th>
                  @for (s of sorts; track s.key) {
                    <th class="num" [attr.aria-sort]="ariaSort(s.key)">
                      <button type="button" (click)="sortBy(s.key)">
                        {{ s.label }} <span class="unit">({{ s.unit }})</span> {{ arrow(s.key) }}
                      </button>
                    </th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (food of p.content; track food.id) {
                  <tr>
                    <td>
                      <a [routerLink]="['/aliments', food.id]" class="name">{{ food.name }}</a>
                      <span class="category small muted">
                        {{ food.categoryName ? capitalize(food.categoryName) : 'Sans catégorie' }}
                        @if (food.subcategoryName) {
                          › {{ food.subcategoryName }}
                        }
                      </span>
                    </td>
                    <td class="num kcal">{{ fmt(food.energyKcal, '1.0-0') }}</td>
                    <td class="num">{{ fmt(food.proteinG) }}</td>
                    <td class="num">{{ fmt(food.carbohydrateG) }}</td>
                    <td class="num">{{ fmt(food.fatG) }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          @if (p.totalPages > 1) {
            <div class="row pager">
              <button class="btn btn-sm" [disabled]="p.page === 0" (click)="update({ page: p.page })">
                Précédent
              </button>
              <span class="muted small">Page {{ p.page + 1 }} / {{ p.totalPages }}</span>
              <button class="btn btn-sm" [disabled]="p.page + 1 >= p.totalPages" (click)="update({ page: p.page + 2 })">
                Suivant
              </button>
            </div>
          }
        }
      }
    </section>
  `,
  styles: `
    .filters {
      display: grid;
      grid-template-columns: 2fr 1.5fr 1.5fr auto;
      gap: 10px;
      align-items: center;
      margin-bottom: 12px;
    }
    @media (max-width: 860px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
    .toggle {
      flex-direction: row;
      align-items: center;
      gap: 8px;
      font-weight: 400;
      white-space: nowrap;
    }
    .count {
      margin: 0 0 8px;
    }
    .table-card {
      padding: 0;
      overflow-x: auto;
    }
    th button {
      border: 0;
      background: none;
      padding: 0;
      font: inherit;
      color: inherit;
      text-transform: inherit;
      letter-spacing: inherit;
      cursor: pointer;
      white-space: nowrap;
    }
    th button:hover {
      color: var(--color-ink);
    }
    th[aria-sort='ascending'] button,
    th[aria-sort='descending'] button {
      color: var(--color-primary);
    }
    .unit {
      text-transform: none;
      font-weight: 400;
    }
    td,
    th {
      padding: 10px 14px;
    }
    .num {
      text-align: right;
      white-space: nowrap;
      font-variant-numeric: tabular-nums;
    }
    th.num button {
      text-align: right;
    }
    .kcal {
      font-weight: 600;
    }
    .name {
      font-weight: 500;
      color: var(--color-ink);
      text-decoration: none;
    }
    .name:hover {
      color: var(--color-primary);
      text-decoration: underline;
    }
    .category {
      display: block;
    }
    tbody tr:hover {
      background: var(--color-surface-muted);
    }
    .pager {
      justify-content: center;
      margin-top: 16px;
    }
  `,
})
export class FoodsPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  // Paramètres d'URL (liaison via withComponentInputBinding)
  readonly q = input<string>();
  /** Code de groupe (2 chiffres) ou de sous-groupe (4 chiffres). */
  readonly categorie = input<string>();
  readonly tri = input<string>();
  readonly ordre = input<string>();
  readonly pageNumber = input<string>(undefined, { alias: 'page' });
  /** Inclut aussi les plats et produits préparés. */
  readonly tout = input(false, { transform: booleanAttribute });

  protected readonly sorts = SORTS;
  protected readonly categories = signal<FoodCategory[]>([]);
  protected readonly page = signal<Page<FoodCatalogItem> | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly typed = new Subject<string>();

  protected readonly sort = computed<CatalogSort>(() =>
    this.tri() && (this.tri() === 'name' || SORTS.some((s) => s.key === this.tri()))
      ? (this.tri() as CatalogSort)
      : 'name',
  );
  protected readonly direction = computed(() =>
    this.ordre() === 'desc' || this.ordre() === 'asc' ? this.ordre()! : this.sort() === 'name' ? 'asc' : 'desc',
  );
  protected readonly categoryCode = computed(() => this.categorie()?.slice(0, 2) ?? '');
  protected readonly subcategoryCode = computed(() =>
    this.categorie()?.length === 4 ? this.categorie()! : '',
  );
  protected readonly subcategories = computed(
    () => this.categories().find((c) => c.code === this.categoryCode())?.subcategories ?? [],
  );
  protected readonly sortLabel = computed(() =>
    (SORTS.find((s) => s.key === this.sort())?.label ?? '').toLowerCase(),
  );

  constructor() {
    effect(() => {
      this.api.foodCategories(!this.tout()).subscribe({
        next: (categories) => this.categories.set(categories),
        error: (e) => this.error.set(errorMessage(e)),
      });
    });

    effect(() => {
      const query = {
        q: this.q(),
        group: this.categorie(),
        basic: !this.tout(),
        sort: this.sort(),
        direction: this.direction() as 'asc' | 'desc',
        page: Math.max(Number(this.pageNumber() ?? 1) - 1, 0) || 0,
        size: PAGE_SIZE,
      };
      this.api.foodCatalog(query).subscribe({
        next: (page) => {
          this.page.set(page);
          this.error.set(null);
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    });

    this.typed
      .pipe(debounceTime(300), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe((q) => this.update({ q: q.trim() || null }));
  }

  /** Met à jour les paramètres d'URL ; tout changement de filtre ramène à la première page. */
  protected update(params: Record<string, string | number | boolean | null>): void {
    const queryParams = 'page' in params ? params : { ...params, page: null };
    void this.router.navigate([], { queryParams, queryParamsHandling: 'merge', replaceUrl: !('page' in params) });
    if ('page' in params) {
      window.scrollTo({ top: 0 });
    }
  }

  protected toggleBasic(basicOnly: boolean): void {
    this.update({ tout: basicOnly ? null : true, categorie: null });
  }

  protected sortBy(key: CatalogSort): void {
    const defaultDirection = key === 'name' ? 'asc' : 'desc';
    const direction = this.sort() === key ? (this.direction() === 'asc' ? 'desc' : 'asc') : defaultDirection;
    this.update({ tri: key === 'name' ? null : key, ordre: direction === defaultDirection ? null : direction });
  }

  protected arrow(key: CatalogSort): string {
    return this.sort() === key ? (this.direction() === 'asc' ? '↑' : '↓') : '';
  }

  protected ariaSort(key: CatalogSort): string | null {
    return this.sort() === key ? (this.direction() === 'asc' ? 'ascending' : 'descending') : null;
  }

  /** Les libellés de groupes Ciqual sont en minuscules. */
  protected capitalize(text: string): string {
    return text.charAt(0).toUpperCase() + text.slice(1);
  }

  protected fmt(value: number | null, format = '1.0-1'): string {
    return value === null ? '—' : (this.decimal.transform(value, format) ?? '—');
  }

  private readonly decimal = new DecimalPipe(inject(LOCALE_ID));
}
