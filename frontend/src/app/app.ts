import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, MatButtonModule],
  template: `
    @if (auth.erroInicializacao(); as erro) {
      <main class="falha">
        <div class="crm-card">
          <h1>Login indisponível</h1>
          <p>{{ erro }}</p>
          <button mat-flat-button type="button" (click)="recarregar()">Tentar novamente</button>
        </div>
      </main>
    } @else {
      <router-outlet />
    }
  `,
  styles: `
    .falha {
      min-height: 100dvh;
      display: grid;
      place-items: center;
      padding: 16px;
      text-align: center;
    }
    .falha .crm-card {
      max-width: 420px;
    }
    p {
      color: var(--crm-texto-secundario);
    }
  `,
})
export class App {
  protected readonly auth = inject(AuthService);

  protected recarregar(): void {
    window.location.reload();
  }
}
