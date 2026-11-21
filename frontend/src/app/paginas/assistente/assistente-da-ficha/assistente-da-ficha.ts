import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  AnotacaoDoAtendimento,
  AssistenteApi,
  NOME_CHANCE_DE_INDICAR,
  NOME_SATISFACAO,
  NOME_SITUACAO_DO_RESUMO,
  Sugestoes,
} from '../../../core/api/assistente-api.service';
import { problemaDe } from '../../../core/api/problema';
import { StatusChip } from '../../../shared/status-chip/status-chip';

/**
 * IA-02 / IA-04 na ficha: os resumos que a IA fez dos atendimentos anotados e, a pedido, até 3 sugestões de
 * abordagem para o próximo contato. Só sugestão: nada é enviado nem alterado.
 */
@Component({
  selector: 'app-assistente-da-ficha',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule, StatusChip],
  templateUrl: './assistente-da-ficha.html',
  styleUrl: './assistente-da-ficha.scss',
})
export class AssistenteDaFicha {
  private readonly api = inject(AssistenteApi);
  private readonly destroy = inject(DestroyRef);

  readonly associadoId = input.required<number>();
  /** Muda a cada recarga da ficha (ex.: atendimento novo), para buscar os resumos de novo. */
  readonly versaoDaFicha = input(0);

  protected readonly nomeSituacao = NOME_SITUACAO_DO_RESUMO;
  protected readonly nomeSatisfacao = NOME_SATISFACAO;
  protected readonly nomeChance = NOME_CHANCE_DE_INDICAR;

  protected readonly anotacoes = signal<AnotacaoDoAtendimento[]>([]);
  protected readonly sugestoes = signal<Sugestoes | null>(null);
  protected readonly pensando = signal(false);
  protected readonly aviso = signal('');

  constructor() {
    effect(() => this.carregar(this.associadoId(), this.versaoDaFicha()));
  }

  protected recarregar(): void {
    this.carregar(this.associadoId(), this.versaoDaFicha());
  }

  protected sugerir(): void {
    this.pensando.set(true);
    this.aviso.set('');
    this.api
      .recomendar(this.associadoId())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (s) => {
          this.pensando.set(false);
          this.sugestoes.set(s);
        },
        error: (e: unknown) => {
          this.pensando.set(false);
          this.aviso.set(problemaDe(e).detail ?? 'Não foi possível gerar sugestões agora.');
        },
      });
  }

  protected tipoDoChip(anotacao: AnotacaoDoAtendimento): 'ativo' | 'hoje' | 'atrasado' {
    if (anotacao.situacao === 'RESUMIDO') return 'ativo';
    return anotacao.situacao === 'FALHOU' ? 'atrasado' : 'hoje';
  }

  private carregar(associadoId: number, _versao: number): void {
    this.sugestoes.set(null);
    this.api
      .anotacoesDoAssociado(associadoId)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (lista) => this.anotacoes.set(lista),
        error: () => this.anotacoes.set([]),
      });
  }
}
