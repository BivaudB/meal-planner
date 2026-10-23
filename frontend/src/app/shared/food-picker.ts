import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { ApiService } from '../core/api.service';
import { FoodSummary } from '../core/models';
import { MacrosView } from './macros';

/**
 * Champ de recherche d'aliment dans le référentiel Ciqual, avec suggestions
 * (calories et macronutriments pour 100 g). Émet l'aliment choisi via (picked).
 */
@Component({
  selector: 'app-food-picker',
  imports: [MacrosView],
  template: `
    <div class="picker">
      <input
        type="search"
        [placeholder]="placeholder()"
        [value]="text()"
        (input)="onInput($any($event.target).value)"
        (focus)="open.set(true)"
        (blur)="closeSoon()"
        (keydown.escape)="open.set(false)"
        autocomplete="off"
      />
      @if (open() && results().length > 0) {
        <ul class="results">
          @for (food of results(); track food.id) {
            <li>
              <button type="button" (mousedown)="pick(food)">
                <span>{{ food.name }}</span>
                <app-macros [macros]="food" />
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    .picker {
      position: relative;
    }
    input {
      width: 100%;
    }
    .results {
      position: absolute;
      z-index: 20;
      top: calc(100% + 4px);
      left: 0;
      right: 0;
      margin: 0;
      padding: 4px;
      list-style: none;
      background: var(--color-surface);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-sm);
      box-shadow: var(--shadow);
      max-height: 280px;
      overflow-y: auto;
    }
    .results button {
      display: flex;
      flex-direction: column;
      gap: 2px;
      width: 100%;
      text-align: left;
      border: 0;
      background: transparent;
      padding: 8px 10px;
      border-radius: var(--radius-sm);
      font: inherit;
      cursor: pointer;
    }
    .results button:hover {
      background: var(--color-surface-muted);
    }
  `,
})
export class FoodPicker {
  private readonly api = inject(ApiService);

  readonly placeholder = input('Rechercher un aliment (ex. tomate)');
  /** Texte initial du champ. */
  readonly initialText = input('');
  readonly picked = output<FoodSummary>();
  /** Texte saisi, émis à chaque frappe (utile pour une saisie libre). */
  readonly typed = output<string>();

  protected readonly text = signal('');
  protected readonly results = signal<FoodSummary[]>([]);
  protected readonly open = signal(false);
  private readonly queries = new Subject<string>();

  constructor() {
    this.queries
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((q) =>
          q.trim().length < 2 ? of([]) : this.api.searchFoods(q, 15).pipe(catchError(() => of([]))),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((foods) => this.results.set(foods));
  }

  ngOnInit(): void {
    this.text.set(this.initialText());
  }

  protected onInput(value: string): void {
    this.text.set(value);
    this.open.set(true);
    this.typed.emit(value);
    this.queries.next(value);
  }

  protected pick(food: FoodSummary): void {
    this.text.set(food.name);
    this.open.set(false);
    this.picked.emit(food);
  }

  protected closeSoon(): void {
    setTimeout(() => this.open.set(false), 150);
  }

  clear(): void {
    this.text.set('');
    this.results.set([]);
  }
}
