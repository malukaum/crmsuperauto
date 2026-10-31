import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth/auth.service';
import { LogoCompleta } from './shared/marca/logo-completa';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, MatButtonModule, LogoCompleta],
  template: `
    @if (auth.erroInicializacao(); as erro) {
      <main class="crm-tela-marca">
        <app-logo-completa class="crm-tela-marca__logo" />
        <div class="crm-card crm-tela-marca__cartao">
          <h1>Login indisponível</h1>
          <p>{{ erro }}</p>
          <button mat-flat-button type="button" (click)="recarregar()">Tentar novamente</button>
        </div>
      </main>
    } @else {
      <router-outlet />
    }
  `,
})
export class App {
  protected readonly auth = inject(AuthService);

  protected recarregar(): void {
    window.location.reload();
  }
}
