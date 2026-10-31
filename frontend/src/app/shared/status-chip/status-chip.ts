import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TipoStatus = 'hoje' | 'atrasado' | 'ativo' | 'neutro';

/** Chip de status arredondado, com as cores da marca. */
@Component({
  selector: 'app-status-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'status-chip',
    '[class.status-chip--hoje]': "tipo() === 'hoje'",
    '[class.status-chip--atrasado]': "tipo() === 'atrasado'",
    '[class.status-chip--ativo]': "tipo() === 'ativo'",
    '[class.status-chip--neutro]': "tipo() === 'neutro'",
  },
  template: `<ng-content />`,
  styles: `
    :host {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      height: 26px;
      padding: 0 12px;
      border-radius: var(--crm-raio-chip);
      font-size: 12px;
      font-weight: 600;
      line-height: 1;
      white-space: nowrap;
      font-variant-numeric: tabular-nums;
      border: 1px solid transparent;
    }
    :host(.status-chip--hoje) {
      color: var(--crm-azul);
      background: var(--crm-azul-fundo);
    }
    :host(.status-chip--atrasado) {
      color: var(--crm-laranja-texto);
      background: var(--crm-laranja-fundo);
      border-color: var(--crm-laranja);
    }
    :host(.status-chip--ativo) {
      color: var(--crm-verde);
      background: var(--crm-verde-fundo);
    }
    :host(.status-chip--neutro) {
      color: var(--crm-texto-secundario);
      background: var(--crm-fundo);
      border-color: var(--crm-borda);
    }
  `,
})
export class StatusChip {
  readonly tipo = input<TipoStatus>('neutro');
}
