import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, filter, map, of, switchMap, tap } from 'rxjs';

import {
  CarteiraApi,
  EXTENSAO_EXPORTACAO,
  Fila,
  FormatoExportacao,
  FiltroCarteira,
  LinhaDaCarteira,
  OrdemCarteira,
  PaginaCarteira,
  Perfil,
  PotencialIndicacao,
  SituacaoContrato,
  TAMANHOS_PAGINA,
  TamanhoPagina,
} from '../../../core/api/carteira-api.service';
import { ContratosApi } from '../../../core/api/contratos-api.service';
import { problemaDe } from '../../../core/api/problema';
import { Cidade, ReferenciasApi } from '../../../core/api/referencias-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { GERENCIAL_OU_VENDEDOR, Papel } from '../../../core/auth/papel';
import { TemPapelDirective } from '../../../core/auth/tem-papel.directive';
import { ConfirmarDialog, DadosConfirmar } from '../../acesso/usuarios-equipe/confirmar-dialog';
import { AvisoMensalidade } from '../../../shared/aviso-mensalidade/aviso-mensalidade';
import { CampoCidade } from '../../../shared/campo-cidade/campo-cidade';
import {
  NOME_FILA,
  NOME_PERFIL,
  NOME_POTENCIAL,
  NOME_SITUACAO,
  chipDaFila,
  descreverFila,
  formatarData,
} from '../../../shared/formatos/formatos';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { EstadoCarteira, comFiltro, comOrdem, estadoDaUrl, urlDoEstado } from './filtro-da-url';

const ESPERA_DIGITACAO_MS = 350;
const SO_GERENCIAL: readonly Papel[] = ['gerencial'];

/** CAR-01 · Buscar e filtrar a carteira (uma linha por contrato). CAR-07 · Exportar a lista filtrada. */
@Component({
  selector: 'app-carteira',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTooltipModule,
    AvisoMensalidade,
    CampoCidade,
    StatusChip,
    TemPapelDirective,
  ],
  templateUrl: './carteira.html',
  styleUrl: './carteira.scss',
})
export class Carteira {
  private readonly api = inject(CarteiraApi);
  private readonly contratos = inject(ContratosApi);
  private readonly referencias = inject(ReferenciasApi);
  private readonly auth = inject(AuthService);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  protected readonly tamanhos = TAMANHOS_PAGINA;
  protected readonly filas = Object.keys(NOME_FILA) as Fila[];
  protected readonly situacoes = Object.keys(NOME_SITUACAO) as SituacaoContrato[];
  protected readonly perfis = Object.keys(NOME_PERFIL) as Perfil[];
  protected readonly potenciais = Object.keys(NOME_POTENCIAL) as PotencialIndicacao[];
  protected readonly nomeFila = NOME_FILA;
  protected readonly nomeSituacao = NOME_SITUACAO;
  protected readonly nomePerfil = NOME_PERFIL;
  protected readonly nomePotencial = NOME_POTENCIAL;
  protected readonly podeExportar = GERENCIAL_OU_VENDEDOR;
  protected readonly soGerencial = SO_GERENCIAL;
  protected readonly chipDaFila = chipDaFila;
  protected readonly descreverFila = descreverFila;
  protected readonly formatarData = formatarData;

  /** O filtro de vendedor não aparece para o vendedor (RG-03). */
  protected readonly veFiltroDeVendedor = computed(() => this.auth.temAlgumPapel(['gerencial', 'atendente']));
  protected readonly gerencial = computed(() => this.auth.temAlgumPapel(SO_GERENCIAL));

  protected readonly estado = toSignal(this.rota.queryParamMap.pipe(map(estadoDaUrl)), { requireSync: true });
  protected readonly filtro = computed(() => this.estado().filtro);
  protected readonly cidadeEscolhida = computed<Cidade | null>(() => {
    const { filtro, cidadeNome } = this.estado();
    if (!filtro.cidade || !cidadeNome) return null;
    const [nome, uf] = cidadeNome.split(' / ');
    return { id: filtro.cidade, nome, uf };
  });

  protected readonly carregando = signal(true);
  protected readonly exportando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly resultado = signal<PaginaCarteira | null>(null);
  /** Incrementado para buscar de novo com o mesmo filtro (ex.: depois da ativação em lote). */
  private readonly recarga = signal(0);
  /** Contratos "a conferir" marcados para a ativação em lote (gerencial). */
  protected readonly marcados = signal<ReadonlySet<number>>(new Set());

  protected readonly tiposVeiculo = toSignal(this.referencias.tiposVeiculo$.pipe(catchError(() => of([]))), {
    initialValue: [],
  });
  protected readonly etapas = toSignal(this.referencias.etapas$.pipe(catchError(() => of([]))), { initialValue: [] });
  protected readonly vendedores = toSignal(
    toObservable(this.veFiltroDeVendedor).pipe(
      switchMap((ve) => (ve ? this.referencias.vendedores().pipe(catchError(() => of([]))) : of([]))),
    ),
    { initialValue: [] },
  );

  protected readonly busca = new FormControl('', { nonNullable: true });

  constructor() {
    const destroy = inject(DestroyRef);
    this.busca.setValue(this.filtro().busca ?? '', { emitEvent: false });
    this.busca.valueChanges
      .pipe(
        debounceTime(ESPERA_DIGITACAO_MS),
        map((v) => v.trim()),
        distinctUntilChanged(),
        filter((v) => v !== (this.filtro().busca ?? '')),
        takeUntilDestroyed(destroy),
      )
      .subscribe((busca) => this.filtrar({ busca: busca || undefined }));

    toObservable(computed(() => ({ filtro: this.filtro(), recarga: this.recarga() })))
      .pipe(
        tap(() => {
          this.carregando.set(true);
          this.erro.set(null);
          this.marcados.set(new Set());
        }),
        switchMap(({ filtro }) =>
          this.api.buscar(filtro).pipe(
            catchError((e: unknown) => {
              this.erro.set(problemaDe(e).detail ?? 'Não foi possível carregar a carteira.');
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(destroy),
      )
      .subscribe((pagina) => {
        this.resultado.set(pagina);
        this.carregando.set(false);
      });
  }

  protected filtrar(mudanca: Partial<FiltroCarteira>, cidadeNome?: string): void {
    this.navegar(comFiltro(this.estado(), mudanca, cidadeNome));
  }

  protected escolherCidade(cidade: Cidade | null): void {
    this.filtrar({ cidade: cidade?.id }, cidade ? `${cidade.nome} / ${cidade.uf}` : undefined);
  }

  protected limparFiltros(): void {
    this.busca.setValue('', { emitEvent: false });
    this.navegar({ filtro: { tamanho: this.filtro().tamanho } });
  }

  protected ordenar(coluna: OrdemCarteira): void {
    this.navegar(comOrdem(this.estado(), coluna));
  }

  /** Valor de aria-sort da coluna (e o ícone da direção). */
  protected ordenadoPor(coluna: OrdemCarteira): 'ascending' | 'descending' | null {
    const f = this.filtro();
    if ((f.ordem ?? 'NOME') !== coluna) return null;
    return f.decrescente ? 'descending' : 'ascending';
  }

  protected mudarPagina(evento: PageEvent): void {
    const estado = this.estado();
    this.navegar({
      ...estado,
      filtro: { ...estado.filtro, pagina: evento.pageIndex, tamanho: evento.pageSize as TamanhoPagina },
    });
  }

  protected abrirFicha(linha: LinhaDaCarteira): void {
    this.router.navigate(['/carteira/associados', linha.associadoId]);
  }

  protected temFiltro(): boolean {
    const { pagina: _p, tamanho: _t, ordem: _o, decrescente: _d, ...filtros } = this.filtro();
    return Object.values(filtros).some((v) => v !== undefined);
  }

  // ---------- ativação em lote (CAR-05, proposta) ----------

  protected marcar(linha: LinhaDaCarteira, marcado: boolean): void {
    this.marcados.update((atual) => {
      const novo = new Set(atual);
      if (marcado) novo.add(linha.contratoId);
      else novo.delete(linha.contratoId);
      return novo;
    });
  }

  protected ativarMarcados(): void {
    const ids = [...this.marcados()];
    const dados: DadosConfirmar = {
      titulo: `Marcar ${ids.length} ${ids.length === 1 ? 'contrato' : 'contratos'} como ativos?`,
      mensagem: 'Os contratos "a conferir" escolhidos passam para Ativo. A mudança fica registrada na auditoria.',
      acao: 'Marcar como ativos',
    };
    this.dialog
      .open<ConfirmarDialog, DadosConfirmar, boolean>(ConfirmarDialog, { data: dados })
      .afterClosed()
      .pipe(
        filter((ok) => ok === true),
        switchMap(() => this.contratos.ativarEmLote(ids)),
      )
      .subscribe({
        next: (r) => {
          this.avisar(`${r.ativados} ${r.ativados === 1 ? 'contrato ativado' : 'contratos ativados'}.`);
          this.recarga.update((n) => n + 1);
        },
        error: (e: unknown) => this.avisar(problemaDe(e).detail ?? 'Não foi possível ativar os contratos.'),
      });
  }

  // ---------- exportação (CAR-07) ----------

  protected exportar(formato: FormatoExportacao): void {
    this.exportando.set(true);
    this.api.exportar(this.filtro(), formato).subscribe({
      next: (arquivo) => {
        baixar(arquivo, `carteira-${new Date().toISOString().slice(0, 10)}.${EXTENSAO_EXPORTACAO[formato]}`);
        this.exportando.set(false);
      },
      error: (e: unknown) => {
        this.avisar(problemaDe(e).detail ?? 'Não foi possível exportar a carteira.');
        this.exportando.set(false);
      },
    });
  }

  private navegar(estado: EstadoCarteira): void {
    this.router.navigate([], { relativeTo: this.rota, queryParams: urlDoEstado(estado), replaceUrl: true });
  }

  private avisar(mensagem: string): void {
    this.snack.open(mensagem, 'OK', { duration: 6000 });
  }
}

function baixar(arquivo: Blob, nome: string): void {
  const url = URL.createObjectURL(arquivo);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  link.click();
  URL.revokeObjectURL(url);
}
