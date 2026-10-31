import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** RG-09 · Toda tela com mensalidade avisa que os valores são contratados, não recebidos. */
@Component({
  selector: 'app-aviso-mensalidade',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  template: `
    <mat-icon aria-hidden="true">info</mat-icon>
    <span>Mensalidades são valores contratados, não recebidos. Recebimento só com a integração ao SGA.</span>
  `,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: var(--crm-texto-secundario);
    }
    mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
  `,
})
export class AvisoMensalidade {}
