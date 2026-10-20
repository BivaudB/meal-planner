import { Component, inject, signal } from '@angular/core';

import { ApiService } from '../../core/api.service';
import { CurrentUserStore } from '../../core/current-user.store';
import { errorMessage } from '../../core/errors';
import { ImportJob } from '../../core/models';
import { ImportJobList } from '../../shared/import-job-list';

@Component({
  selector: 'app-admin',
  imports: [ImportJobList],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Administration</h1>
          <p>Référentiel d'aliments Ciqual.</p>
        </div>
      </div>

      @if (!user.isAdmin()) {
        <div class="alert">Cette page est réservée aux administrateurs (rôle « admin » dans Keycloak).</div>
      } @else {
        <div class="card stack">
          <h2>Charger la table Ciqual</h2>
          <p class="muted small">
            Fichier <code>Table_Ciqual_AAAA_FR_*.xlsx</code> téléchargeable sur ciqual.anses.fr. Le chargement met à jour
            les aliments existants : il peut être relancé à chaque nouvelle version de la table.
          </p>
          <div class="row">
            <input type="file" accept=".xlsx" (change)="choose($any($event.target).files)" />
            <button class="btn btn-primary" [disabled]="!file() || running()" (click)="upload()">
              {{ running() ? 'Chargement… (quelques secondes)' : 'Charger' }}
            </button>
          </div>
          @if (error()) {
            <div class="alert alert-danger">{{ error() }}</div>
          }
          <h3>Historique</h3>
          <app-import-job-list [jobs]="jobs()" />
        </div>
      }
    </section>
  `,
})
export class AdminPage {
  private readonly api = inject(ApiService);
  protected readonly user = inject(CurrentUserStore);

  protected readonly file = signal<File | null>(null);
  protected readonly running = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly jobs = signal<ImportJob[]>([]);

  constructor() {
    this.refresh();
  }

  protected choose(files: FileList | null): void {
    this.file.set(files && files.length > 0 ? files[0] : null);
  }

  protected upload(): void {
    const file = this.file();
    if (!file) {
      return;
    }
    this.running.set(true);
    this.error.set(null);
    this.api.importCiqual(file).subscribe({
      next: () => {
        this.running.set(false);
        this.refresh();
      },
      error: (e) => {
        this.running.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }

  private refresh(): void {
    this.api.ciqualImports().subscribe({
      next: (jobs) => this.jobs.set(jobs),
      error: () => this.jobs.set([]),
    });
  }
}
