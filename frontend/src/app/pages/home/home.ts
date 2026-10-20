import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { CurrentUserStore } from '../../core/current-user.store';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  template: `
    <section class="page">
      @if (auth.isLoggedIn()) {
        <div class="page-header">
          <div>
            <h1>Bonjour {{ auth.displayName() }}</h1>
            <p>Que voulez-vous cuisiner aujourd'hui ?</p>
          </div>
        </div>

        <div class="grid">
          <a class="card tile" routerLink="/suggestions">
            <span class="tile-kicker">Suggestions</span>
            <h3>Que cuisiner avec ce que j'ai ?</h3>
            <p class="muted">Des recettes classées selon les ingrédients que vous avez déjà.</p>
          </a>
          <a class="card tile" routerLink="/garde-manger">
            <span class="tile-kicker">Mes ingrédients</span>
            <h3>Mettre à jour mon garde-manger</h3>
            <p class="muted">Indiquez ce que vous avez dans vos placards et votre frigo.</p>
          </a>
          <a class="card tile" routerLink="/recettes">
            <span class="tile-kicker">Recettes</span>
            <h3>Mes recettes et celles partagées</h3>
            <p class="muted">Consultez, complétez les étapes de préparation, partagez.</p>
          </a>
          <a class="card tile" routerLink="/import">
            <span class="tile-kicker">Import</span>
            <h3>Importer un fichier Excel ou CSV</h3>
            <p class="muted">Chargez vos recettes en une fois.</p>
          </a>
        </div>
        @if (user.isAdmin()) {
          <p class="muted small" style="margin-top: 24px">
            Compte administrateur : le chargement du référentiel Ciqual se fait depuis
            <a routerLink="/admin">l'administration</a>.
          </p>
        }
      } @else {
        <div class="hero">
          <h1>Planifiez vos repas avec ce que vous avez déjà.</h1>
          <p>
            Importez vos recettes, indiquez les ingrédients disponibles chez vous et obtenez des suggestions de repas,
            avec leurs valeurs nutritionnelles calculées à partir de la table Ciqual.
          </p>
          <div class="row">
            <button class="btn btn-primary" (click)="auth.login()">Se connecter</button>
            <span class="muted small">Pas encore de compte ? Créez-le depuis la page de connexion.</span>
          </div>
        </div>
      }
    </section>
  `,
  styles: `
    .hero {
      max-width: 640px;
      padding: 48px 0;
    }
    .hero h1 {
      font-size: clamp(2.2rem, 5vw, 3.4rem);
    }
    .hero p {
      font-size: 1.1rem;
      color: var(--color-ink-muted);
      margin-bottom: 28px;
    }
    .tile {
      text-decoration: none;
      color: inherit;
      transition:
        transform 0.15s,
        border-color 0.15s;
    }
    .tile:hover {
      transform: translateY(-2px);
      border-color: var(--color-primary);
    }
    .tile-kicker {
      display: block;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--color-accent);
      margin-bottom: 8px;
    }
    .tile p {
      margin: 0;
    }
  `,
})
export class HomePage {
  protected readonly auth = inject(AuthService);
  protected readonly user = inject(CurrentUserStore);
}
