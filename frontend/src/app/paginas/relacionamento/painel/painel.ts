import { CurrencyPipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { RelacionamentoApi, PainelResumo } from '../../../core/api/relacionamento-api.service';
import { problemaDe } from '../../../core/api/problema';
import { AvisoMensalidade } from '../../../shared/aviso-mensalidade/aviso-mensalidade';
import { formatarData, NOME_SITUACAO } from '../../../shared/formatos/formatos';
/** FIL-01 · Indicadores e atalhos do trabalho de pós-venda. */
@Component({
  selector: 'app-painel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CurrencyPipe, KeyValuePipe, MatButtonModule, RouterLink, AvisoMensalidade],
  templateUrl: './painel.html',
  styleUrl: './painel.scss',
})
export class Painel {
  private readonly api = inject(RelacionamentoApi);
  private readonly destroy = inject(DestroyRef);
  protected readonly dados = signal<PainelResumo | null>(null);
  protected readonly erro = signal('');
  protected readonly formatarData = formatarData;
  protected readonly situacoes = NOME_SITUACAO as Record<string, string>;
  constructor() {
    this.carregar();
  }
  protected carregar(): void {
    this.erro.set('');
    this.api
      .painel()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (p) => this.dados.set(p),
        error: (e) =>
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível concluir a operação.'),
      });
  }
}
