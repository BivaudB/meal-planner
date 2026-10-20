import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';

import { ImportJob } from '../core/models';

const STATUS_LABELS: Record<ImportJob['status'], string> = {
  PENDING: 'En attente',
  RUNNING: 'En cours',
  COMPLETED: 'Terminé',
  COMPLETED_WITH_ERRORS: 'Terminé avec erreurs',
  FAILED: 'Échec',
};

/** Historique des imports, avec rapport dépliable. */
@Component({
  selector: 'app-import-job-list',
  imports: [DatePipe],
  template: `
    @if (jobs().length === 0) {
      <p class="muted">Aucun import pour l'instant.</p>
    } @else {
      <div class="stack">
        @for (job of jobs(); track job.id) {
          <div class="job">
            <div class="row spread">
              <strong>{{ job.filename }}</strong>
              <span [class]="badgeClass(job)">{{ label(job) }}</span>
            </div>
            <div class="small muted">
              {{ job.createdAt | date: 'short' }}
              @if (job.status !== 'PENDING' && job.status !== 'RUNNING') {
                · {{ job.importedItems }} importé(s) sur {{ job.totalItems }}
                @if (job.errorItems > 0) {
                  · {{ job.errorItems }} en erreur
                }
              }
            </div>
            @if (job.report) {
              <details>
                <summary class="small">Voir le rapport</summary>
                <pre>{{ job.report }}</pre>
              </details>
            }
          </div>
        }
      </div>
    }
  `,
  styles: `
    .job {
      padding: 12px 0;
      border-bottom: 1px solid var(--color-border);
    }
    .job:last-child {
      border-bottom: 0;
    }
    .spread {
      justify-content: space-between;
    }
    pre {
      white-space: pre-wrap;
      font-size: 0.8rem;
      background: var(--color-surface-muted);
      padding: 10px;
      border-radius: var(--radius-sm);
      max-height: 240px;
      overflow: auto;
    }
  `,
})
export class ImportJobList {
  readonly jobs = input.required<ImportJob[]>();

  protected label(job: ImportJob): string {
    return STATUS_LABELS[job.status];
  }

  protected badgeClass(job: ImportJob): string {
    switch (job.status) {
      case 'COMPLETED':
        return 'badge badge-primary';
      case 'FAILED':
        return 'badge badge-danger';
      case 'COMPLETED_WITH_ERRORS':
        return 'badge badge-accent';
      default:
        return 'badge';
    }
  }
}
