import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Assinatura da marca para fundos azuis (menu, barras): o símbolo da logo, o nome "SUPERAUTO" no
 * estilo da logo (SUPER em amarelo, AUTO em branco) e um complemento que diz onde a pessoa está.
 */
@Component({
  selector: 'app-marca',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <img class="simbolo" src="marca/simbolo.png" alt="" width="40" height="40" />
    <span class="texto">
      <span class="nome"><span class="nome__super">Super</span>Auto</span>
      <span class="complemento">{{ complemento() }}</span>
    </span>
  `,
  styles: `
    :host {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    .simbolo {
      flex-shrink: 0;
    }
    .texto {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    .nome {
      color: var(--crm-sobre-azul);
      font-size: 18px;
      font-style: italic;
      font-weight: 800;
      line-height: 1.1;
      letter-spacing: -0.02em;
      text-transform: uppercase;
    }
    .nome__super {
      color: var(--crm-amarelo);
    }
    .complemento {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 12px;
      color: var(--crm-sobre-azul-secundario);
    }
  `,
})
export class Marca {
  readonly complemento = input.required<string>();
}
