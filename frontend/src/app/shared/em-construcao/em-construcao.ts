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
      <div class="crm-card crm-estado em-construcao">
        <mat-icon>construction</mat-icon>
        <p class="crm-estado__titulo">Em construção</p>
        <p>Esta área será desenvolvida nas próximas etapas do projeto.</p>
      </div>
    </div>
  `,
  styles: `
    .em-construcao {
      margin-top: 16px;
    }
  `,
})
export class EmConstrucao {
  readonly titulo = input('Em construção');
}
