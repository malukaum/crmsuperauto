import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { LogoCompleta } from '../../../shared/marca/logo-completa';

@Component({
  selector: 'app-acesso-negado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, RouterLink, LogoCompleta],
  template: `
    <main class="crm-tela-marca">
      <app-logo-completa class="crm-tela-marca__logo" />
      <div class="crm-card crm-tela-marca__cartao">
        <mat-icon class="icone">lock</mat-icon>
        <h1>Acesso negado</h1>
        @if (auth.logado()) {
          <p>
            Seu usuário <strong>{{ auth.username() }}</strong> ({{ auth.nomePapel() }}) não tem permissão para
            acessar esta página.
          </p>
        } @else {
          <p>Você precisa entrar para acessar esta página.</p>
        }
        <div class="acoes">
          @if (auth.logado() && auth.papeis().length) {
            <a mat-flat-button routerLink="/">Ir para o início</a>
          }
          @if (auth.logado()) {
            <button mat-stroked-button type="button" (click)="auth.logout()">Sair</button>
          } @else {
            <button mat-flat-button type="button" (click)="auth.login()">Entrar</button>
          }
        </div>
      </div>
    </main>
  `,
  styles: `
    .icone {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: var(--crm-azul-marca);
    }
    .acoes {
      display: flex;
      gap: 12px;
      justify-content: center;
      flex-wrap: wrap;
      margin-top: 24px;
    }
  `,
})
export class AcessoNegado {
  protected readonly auth = inject(AuthService);
}
