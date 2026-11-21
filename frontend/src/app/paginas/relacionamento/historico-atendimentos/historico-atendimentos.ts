import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Subject, catchError, of, startWith, switchMap } from 'rxjs';

import {
  EXTENSAO_EXPORTACAO,
  FormatoExportacao,
} from '../../../core/api/carteira-api.service';
import { problemaDe } from '../../../core/api/problema';
import { Cidade } from '../../../core/api/referencias-api.service';
import {
  FiltroHistorico,
  LinhaHistorico,
  NOME_TIPO_ATENDIMENTO,
  RelacionamentoApi,
  ResultadoContato,
  TipoAtendimento,
} from '../../../core/api/relacionamento-api.service';
import { GERENCIAL_OU_VENDEDOR } from '../../../core/auth/papel';
import { TemPapelDirective } from '../../../core/auth/tem-papel.directive';
import { baixar } from '../../../shared/arquivo/baixar';
import { CampoCidade } from '../../../shared/campo-cidade/campo-cidade';
import { NOME_RESULTADO, formatarData } from '../../../shared/formatos/formatos';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { FILTRO_INICIAL, marcaDeCorrecao } from './filtro-do-historico';

/**
 * ATD-05 · Consultar o histórico de atendimentos, do mais recente para o mais antigo, com filtros e exportação
 * (gerencial e vendedor, regras da CAR-07). O vendedor vê só a sua carteira (RG-03).
 */
@Component({
  selector: 'app-historico-atendimentos',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatSelectModule,
    CampoCidade,
    StatusChip,
    TemPapelDirective,
  ],
  templateUrl: './historico-atendimentos.html',
  styleUrl: './historico-atendimentos.scss',
})
export class HistoricoAtendimentos {
  private readonly api = inject(RelacionamentoApi);
  private readonly snack = inject(MatSnackBar);
  private readonly consultas = new Subject<void>();

  protected readonly tipos = Object.keys(NOME_TIPO_ATENDIMENTO) as TipoAtendimento[];
  protected readonly resultados = Object.keys(NOME_RESULTADO) as ResultadoContato[];
  protected readonly nomeTipo = NOME_TIPO_ATENDIMENTO;
  protected readonly nomeResultado = NOME_RESULTADO;
  protected readonly gerencialOuVendedor = GERENCIAL_OU_VENDEDOR;
  protected readonly formatarData = formatarData;
  protected readonly marcaDeCorrecao = marcaDeCorrecao;
  protected readonly autores = toSignal(this.api.autores().pipe(catchError(() => of([]))), {
    initialValue: [],
  });

  protected readonly filtro = signal<FiltroHistorico>({ ...FILTRO_INICIAL });
  protected readonly cidade = signal<Cidade | null>(null);
  protected readonly linhas = signal<LinhaHistorico[]>([]);
  protected readonly carregando = signal(false);
  protected readonly exportando = signal(false);
  protected readonly erro = signal('');
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(25);
  protected readonly visiveis = computed(() =>
    this.linhas().slice(this.pagina() * this.tamanho(), (this.pagina() + 1) * this.tamanho()),
  );

  constructor() {
    this.consultas
      .pipe(
        startWith(undefined),
        switchMap(() => this.consultar()),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((linhas) => {
        this.linhas.set(linhas);
        this.carregando.set(false);
      });
  }

  protected alterar<K extends keyof FiltroHistorico>(campo: K, valor: FiltroHistorico[K]): void {
    this.filtro.update((atual) => ({ ...atual, [campo]: valor }));
  }

  protected escolherCidade(cidade: Cidade | null): void {
    this.cidade.set(cidade);
    this.alterar('cidade', cidade?.id ?? null);
  }

  protected filtrar(): void {
    this.pagina.set(0);
    this.consultas.next();
  }

  protected limpar(): void {
    this.filtro.set({ ...FILTRO_INICIAL });
    this.cidade.set(null);
    this.filtrar();
  }

  protected paginar(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
  }

  protected exportar(formato: FormatoExportacao): void {
    this.exportando.set(true);
    this.api.exportarHistorico(this.filtro(), formato).subscribe({
      next: (arquivo) => {
        baixar(arquivo, `atendimentos-${new Date().toISOString().slice(0, 10)}.${EXTENSAO_EXPORTACAO[formato]}`);
        this.exportando.set(false);
      },
      error: (e: unknown) => {
        this.snack.open(problemaDe(e).detail ?? 'Não foi possível exportar o histórico.', 'OK', { duration: 6000 });
        this.exportando.set(false);
      },
    });
  }

  private consultar() {
    this.carregando.set(true);
    this.erro.set('');
    return this.api.historico(this.filtro()).pipe(
      catchError((e: unknown) => {
        this.erro.set(problemaDe(e).detail ?? 'Não foi possível carregar o histórico.');
        return of([]);
      }),
    );
  }
}
