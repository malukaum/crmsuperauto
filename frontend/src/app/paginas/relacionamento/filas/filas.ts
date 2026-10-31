import { CurrencyPipe } from '@angular/common';
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
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, catchError, of, startWith, switchMap } from 'rxjs';
import { RelacionamentoApi, LinhaTrabalho } from '../../../core/api/relacionamento-api.service';
import { ReferenciasApi, Cidade } from '../../../core/api/referencias-api.service';
import { MenuService } from '../../../core/api/menu.service';
import { problemaDe } from '../../../core/api/problema';
import { AvisoMensalidade } from '../../../shared/aviso-mensalidade/aviso-mensalidade';
import { CampoCidade } from '../../../shared/campo-cidade/campo-cidade';
import {
  formatarData,
  formatarTelefone,
  linkWhatsapp,
  NOME_SITUACAO,
} from '../../../shared/formatos/formatos';
import { RegistroNaFila } from './registro-na-fila';
/** FIL-02 a FIL-04 · Consulta das filas e seleção do associado a contatar. */
@Component({
  selector: 'app-filas',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    RouterLink,
    AvisoMensalidade,
    CampoCidade,
    RegistroNaFila,
  ],
  templateUrl: './filas.html',
  styleUrl: './filas.scss',
})
export class Filas {
  private readonly api = inject(RelacionamentoApi);
  private readonly menu = inject(MenuService);
  private readonly rota = inject(ActivatedRoute);
  private readonly referencias = inject(ReferenciasApi);
  private readonly atualizacoes = new Subject<void>();
  protected readonly aniversario = this.rota.snapshot.data['fila'] === 'aniversarios';
  protected readonly atrasados = this.rota.snapshot.data['fila'] === 'atrasados';
  protected readonly titulo = this.aniversario
    ? 'Aniversariantes'
    : this.atrasados
      ? 'Atrasados'
      : 'Contatos de hoje';
  protected readonly tipos = toSignal(this.referencias.tiposVeiculo$, { initialValue: [] });
  protected readonly etapasFiltro = toSignal(this.referencias.etapas$, { initialValue: [] });
  protected readonly linhas = signal<LinhaTrabalho[]>([]);
  protected readonly carregando = signal(false);
  protected readonly erro = signal('');
  protected readonly selecionado = signal<LinhaTrabalho | null>(null);
  protected readonly gravando = signal(false);
  protected readonly pagina = signal(0);
  protected readonly tamanho = signal(25);
  protected readonly visiveis = computed(() =>
    this.linhas().slice(this.pagina() * this.tamanho(), (this.pagina() + 1) * this.tamanho()),
  );
  protected readonly formatarData = formatarData;
  protected readonly formatarTelefone = formatarTelefone;
  protected readonly linkWhatsapp = linkWhatsapp;
  protected readonly situacoes = NOME_SITUACAO;
  protected readonly dias = signal(Number(this.rota.snapshot.queryParamMap.get('dias') ?? 0));
  protected readonly busca = signal('');
  protected readonly cidade = signal<Cidade | null>(null);
  protected readonly etapa = signal<number | null>(null);
  protected readonly tipoVeiculo = signal<number | null>(null);
  constructor() {
    this.atualizacoes
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
  private consultar() {
    this.carregando.set(true);
    const consulta = this.aniversario
      ? this.api.aniversarios(this.dias())
      : this.api.fila(this.atrasados ? 'atrasados' : 'hoje', {
          busca: this.busca(),
          cidade: this.cidade()?.id ?? null,
          etapa: this.etapa(),
          tipoVeiculo: this.tipoVeiculo(),
        });
    return consulta.pipe(
      catchError((erro) => {
        this.erro.set(problemaDe(erro).detail ?? 'Não foi possível carregar a fila.');
        return of([]);
      }),
    );
  }
  protected carregar(): void {
    if (this.gravando()) return;
    this.selecionado.set(null);
    this.erro.set('');
    this.pagina.set(0);
    this.atualizacoes.next();
  }
  protected paginar(evento: PageEvent): void {
    this.pagina.set(evento.pageIndex);
    this.tamanho.set(evento.pageSize);
  }
  protected escolher(linha: LinhaTrabalho): void {
    this.selecionado.set(linha);
    this.erro.set('');
  }
  protected aoRegistrar(): void {
    this.carregar();
    this.menu.carregar();
  }
  protected aoConflito(mensagem: string): void {
    this.carregar();
    this.erro.set(mensagem);
    this.menu.carregar();
  }
}
