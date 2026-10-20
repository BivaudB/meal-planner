import { Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { errorMessage } from '../../core/errors';
import { ImportJob } from '../../core/models';
import { ImportJobList } from '../../shared/import-job-list';

@Component({
  selector: 'app-import',
  imports: [RouterLink, ImportJobList],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Importer des recettes</h1>
          <p>Fichier Excel (.xlsx, .xls) ou CSV, au format « deux lignes par recette ».</p>
        </div>
      </div>

      <div class="layout">
        <div class="card stack">
          <h2>Nouveau fichier</h2>
          <label class="drop" [class.has-file]="file()">
            <input type="file" accept=".xlsx,.xls,.csv" (change)="choose($any($event.target).files)" />
            @if (file(); as f) {
              <strong>{{ f.name }}</strong>
              <span class="small muted">Cliquez pour changer de fichier</span>
            } @else {
              <strong>Choisir un fichier</strong>
              <span class="small muted">.xlsx, .xls ou .csv · 20 Mo maximum</span>
            }
          </label>
          @if (error()) {
            <div class="alert alert-danger">{{ error() }}</div>
          }
          <button class="btn btn-primary" [disabled]="!file() || uploading()" (click)="upload()">
            {{ uploading() ? 'Envoi…' : 'Importer' }}
          </button>
          @if (lastCompleted()) {
            <p class="small">Import terminé. <a routerLink="/recettes">Voir mes recettes</a></p>
          }
        </div>

        <div class="card format">
          <h2>Format attendu</h2>
          <table>
            <tbody>
              <tr><td><strong>Omelette</strong></td><td>Oeufs</td><td>Lait</td><td>Sel</td></tr>
              <tr><td></td><td>3</td><td>5 cl</td><td>1 pincée</td></tr>
              <tr><td><strong>Salade</strong></td><td>Tomates</td><td>Huile d'olive</td><td></td></tr>
              <tr><td></td><td>4</td><td>2 c. à soupe</td><td></td></tr>
            </tbody>
          </table>
          <ul class="small muted">
            <li>1re ligne : nom de la recette, puis un ingrédient par colonne.</li>
            <li>2e ligne : première case vide, puis la quantité de chaque ingrédient (avec l'unité).</li>
            <li>Les ingrédients sont rattachés automatiquement à la table Ciqual quand c'est possible.</li>
            <li>Les étapes de préparation se complètent ensuite dans chaque recette.</li>
          </ul>
        </div>
      </div>

      <div class="card" style="margin-top: 16px">
        <h2>Historique</h2>
        <app-import-job-list [jobs]="jobs()" />
      </div>
    </section>
  `,
  styles: `
    .layout {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 16px;
    }
    .drop {
      position: relative;
      align-items: center;
      justify-content: center;
      text-align: center;
      min-height: 140px;
      border: 2px dashed var(--color-border);
      border-radius: var(--radius);
      cursor: pointer;
      font-weight: 400;
    }
    .drop.has-file {
      border-color: var(--color-primary);
      background: var(--color-primary-soft);
    }
    .drop input {
      position: absolute;
      inset: 0;
      opacity: 0;
      cursor: pointer;
    }
    .format table td {
      border: 1px solid var(--color-border);
      font-size: 0.85rem;
    }
    .format ul {
      padding-left: 18px;
      margin: 12px 0 0;
    }
  `,
})
export class ImportPage {
  private readonly api = inject(ApiService);

  protected readonly file = signal<File | null>(null);
  protected readonly uploading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly jobs = signal<ImportJob[]>([]);
  protected readonly lastCompleted = signal(false);
  private pollTimer?: ReturnType<typeof setInterval>;

  constructor() {
    this.refresh();
    inject(DestroyRef).onDestroy(() => clearInterval(this.pollTimer));
  }

  protected choose(files: FileList | null): void {
    this.file.set(files && files.length > 0 ? files[0] : null);
    this.error.set(null);
  }

  protected upload(): void {
    const file = this.file();
    if (!file) {
      return;
    }
    this.uploading.set(true);
    this.lastCompleted.set(false);
    this.api.importRecipes(file).subscribe({
      next: () => {
        this.uploading.set(false);
        this.file.set(null);
        this.refresh();
        this.startPolling();
      },
      error: (e) => {
        this.uploading.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  /** L'import tourne en tâche de fond : on rafraîchit l'historique tant qu'il est en cours. */
  private startPolling(): void {
    clearInterval(this.pollTimer);
    this.pollTimer = setInterval(() => this.refresh(), 1500);
  }

  private refresh(): void {
    this.api.imports().subscribe({
      next: (jobs) => {
        this.jobs.set(jobs);
        const running = jobs.some((j) => j.status === 'PENDING' || j.status === 'RUNNING');
        if (!running && this.pollTimer) {
          clearInterval(this.pollTimer);
          this.pollTimer = undefined;
          this.lastCompleted.set(true);
        }
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
