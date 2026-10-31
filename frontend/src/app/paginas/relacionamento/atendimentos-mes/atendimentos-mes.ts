import { ChangeDetectionStrategy, Component, inject, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { RelacionamentoApi, ContatoDoMes } from '../../../core/api/relacionamento-api.service';
import { problemaDe } from '../../../core/api/problema';
import { formatarData, NOME_RESULTADO } from '../../../shared/formatos/formatos';
/** FIL-01 · Histórico do mês acessível pelo indicador do painel. */
@Component({
  selector: 'app-atendimentos-mes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule],
  templateUrl: './atendimentos-mes.html',
})
export class AtendimentosMes {
  private readonly api = inject(RelacionamentoApi);
  private readonly destroy = inject(DestroyRef);
  protected readonly linhas = signal<ContatoDoMes[]>([]);
  protected readonly erro = signal('');
  protected readonly carregando = signal(false);
  protected readonly formatarData = formatarData;
  protected readonly resultados = NOME_RESULTADO;
  constructor() {
    this.carregar();
  }
  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    this.api
      .atendimentosMes()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (linhas) => {
          this.linhas.set(linhas);
          this.carregando.set(false);
        },
        error: (e) => {
          this.erro.set(problemaDe(e).detail ?? 'Falha ao carregar o histórico.');
          this.carregando.set(false);
        },
      });
  }
}
