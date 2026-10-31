import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Página provisória. O título vem de data.titulo da rota (withComponentInputBinding). */
@Component({
  selector: 'app-em-construcao',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  template: `
    <div class="crm-pagina">
      <h1 class="crm-titulo-pagina">{{ titulo() }}</h1>
      <div class="crm-card vazio">
        <mat-icon class="vazio__icone">construction</mat-icon>
        <p class="vazio__titulo">Em construção</p>
        <p class="vazio__texto">Esta área será desenvolvida nas próximas etapas do projeto.</p>
      </div>
    </div>
  `,
  styles: `
    .vazio {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      padding: 48px 24px;
      margin-top: 16px;
    }
    .vazio__icone {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: var(--crm-texto-secundario);
    }
    .vazio__titulo {
      margin: 12px 0 4px;
      font-weight: 600;
      color: var(--crm-marinho);
    }
    .vazio__texto {
      margin: 0;
      color: var(--crm-texto-secundario);
    }
  `,
})
export class EmConstrucao {
  readonly titulo = input('Em construção');
}
