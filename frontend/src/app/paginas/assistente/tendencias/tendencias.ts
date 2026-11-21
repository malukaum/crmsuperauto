import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import {
  Assunto,
  AssistenteApi,
  LeituraDasTendencias,
  NOME_ASSUNTO,
  NOME_SATISFACAO,
  PeriodoDasTendencias,
  Satisfacao,
  Tendencias as IndicadoresDeTendencia,
} from '../../../core/api/assistente-api.service';
import { problemaDe } from '../../../core/api/problema';

export interface Fatia {
  rotulo: string;
  quantidade: number;
  percentual: number;
}

/** Ordena do mais para o menos frequente e calcula a fatia de cada um sobre o total de atendimentos resumidos. */
export function fatias<T extends string>(
  contagem: Partial<Record<T, number>>,
  nomes: Record<T, string>,
  total: number,
): Fatia[] {
  return (Object.entries(contagem) as [T, number][])
    .map(([chave, quantidade]) => ({
      rotulo: nomes[chave] ?? chave,
      quantidade,
      percentual: total > 0 ? Math.round((quantidade * 100) / total) : 0,
    }))
    .sort((a, b) => b.quantidade - a.quantidade);
}

/**
 * IA-03 · Tendências da carteira. Os números vêm do banco (dos resumos dos atendimentos); a leitura em texto é da
 * IA, a pedido, e recebe só os números agregados. O vendedor vê só a carteira dele (o servidor filtra).
 */
@Component({
  selector: 'app-tendencias',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressBarModule],
  templateUrl: './tendencias.html',
  styleUrl: './tendencias.scss',
})
export class Tendencias {
  private readonly api = inject(AssistenteApi);
  private readonly destroy = inject(DestroyRef);

  protected readonly inicio = signal('');
  protected readonly fim = signal('');
  protected readonly indicadores = signal<IndicadoresDeTendencia | null>(null);
  protected readonly leitura = signal<LeituraDasTendencias | null>(null);
  protected readonly carregando = signal(false);
  protected readonly lendo = signal(false);
  protected readonly erro = signal('');

  protected readonly total = computed(() => this.indicadores()?.atendimentosResumidos ?? 0);
  protected readonly assuntos = computed(() =>
    fatias<Assunto>(this.indicadores()?.porAssunto ?? {}, NOME_ASSUNTO, this.total()),
  );
  protected readonly satisfacao = computed(() =>
    fatias<Satisfacao>(this.indicadores()?.porSatisfacao ?? {}, NOME_SATISFACAO, this.total()),
  );
  protected readonly periodoInvalido = computed(() => !!this.inicio() && !!this.fim() && this.fim() < this.inicio());

  constructor() {
    this.consultar();
  }

  protected consultar(): void {
    if (this.periodoInvalido()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.leitura.set(null);
    this.api
      .tendencias(this.periodo())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (t) => {
          this.carregando.set(false);
          this.indicadores.set(t);
        },
        error: (e: unknown) => {
          this.carregando.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível carregar as tendências.');
        },
      });
  }

  protected lerComIa(): void {
    this.lendo.set(true);
    this.erro.set('');
    this.api
      .leitura(this.periodo())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (l) => {
          this.lendo.set(false);
          this.leitura.set(l);
        },
        error: (e: unknown) => {
          this.lendo.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível gerar a leitura agora.');
        },
      });
  }

  private periodo(): PeriodoDasTendencias {
    return { inicio: this.inicio() || undefined, fim: this.fim() || undefined };
  }
}
