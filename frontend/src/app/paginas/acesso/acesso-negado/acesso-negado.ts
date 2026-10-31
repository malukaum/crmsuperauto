import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-acesso-negado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, RouterLink],
  template: `
    <main class="negado">
      <div class="crm-card negado__card">
        <mat-icon class="negado__icone">lock</mat-icon>
        <h1>Acesso negado</h1>
        @if (auth.logado()) {
          <p>
            Seu usuário <strong>{{ auth.username() }}</strong> ({{ auth.nomePapel() }}) não tem permissão para
            acessar esta página.
          </p>
        } @else {
          <p>Você precisa entrar para acessar esta página.</p>
        }
        <div class="negado__acoes">
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
    .negado {
      min-height: 100dvh;
      display: grid;
      place-items: center;
      padding: 16px;
      box-sizing: border-box;
    }
    .negado__card {
      max-width: 440px;
      text-align: center;
      padding: 40px 32px;
    }
    .negado__icone {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: var(--crm-vermelho);
    }
    h1 {
      margin: 12px 0 8px;
    }
    p {
      color: var(--crm-texto-secundario);
    }
    .negado__acoes {
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
