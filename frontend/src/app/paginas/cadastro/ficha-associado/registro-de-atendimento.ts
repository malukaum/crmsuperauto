import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { Observable, catchError, map, of, switchMap } from 'rxjs';

import { ContratoDaFicha, FichaAssociado } from '../../../core/api/associados-api.service';
import {
  AssistenteApi,
  REGRAS_DA_ANOTACAO,
  problemaDosAnexos,
} from '../../../core/api/assistente-api.service';
import { problemaDe } from '../../../core/api/problema';
import {
  AtendimentoGravado,
  NOME_TIPO_ATENDIMENTO,
  PreviaContato,
  RegistroContato,
  RelacionamentoApi,
  ResultadoContato,
  TIPOS_AVULSOS,
  TipoAvulso,
} from '../../../core/api/relacionamento-api.service';
import { NOME_RESULTADO, NOME_SITUACAO, formatarData } from '../../../shared/formatos/formatos';

type TipoDoFormulario = 'POS_VENDA' | TipoAvulso;

/**
 * ATD-01 e ATD-02 pela ficha: pós-venda (com prévia da régua) ou avulso (Suporte, Comercial, Reaproximação,
 * Indicação), que nunca mexe na régua. IA-01: anotações livres e anexos vão depois do registro, para a IA resumir
 * em segundo plano; o registro não espera a IA.
 */
@Component({
  selector: 'app-registro-de-atendimento',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  templateUrl: './registro-de-atendimento.html',
  styleUrl: './registro-de-atendimento.scss',
})
export class RegistroDeAtendimento {
  private readonly api = inject(RelacionamentoApi);
  private readonly assistente = inject(AssistenteApi);
  private readonly destroy = inject(DestroyRef);

  readonly ficha = input.required<FichaAssociado>();
  readonly registrado = output<string>();
  /** RG-06: outra pessoa mexeu na régua; a ficha precisa ser recarregada. */
  readonly desatualizado = output<string>();

  protected readonly tipos: readonly TipoDoFormulario[] = ['POS_VENDA', ...TIPOS_AVULSOS];
  protected readonly resultados = Object.keys(NOME_RESULTADO) as ResultadoContato[];
  protected readonly nomeTipo = NOME_TIPO_ATENDIMENTO;
  protected readonly nomeResultado = NOME_RESULTADO;
  protected readonly nomeSituacao = NOME_SITUACAO;
  protected readonly formatarData = formatarData;
  protected readonly regras = REGRAS_DA_ANOTACAO;

  protected readonly tipo = signal<TipoDoFormulario>('POS_VENDA');
  protected readonly contratoId = signal<number | null>(null);
  protected readonly resultado = signal<ResultadoContato>('CONCLUIDO');
  protected readonly retorno = signal('');
  protected readonly realizadoEm = signal('');
  protected readonly descricao = signal('');
  protected readonly previsao = signal<PreviaContato | null>(null);
  protected readonly erro = signal('');
  protected readonly gravando = signal(false);
  protected readonly anotacoes = signal('');
  protected readonly anexos = signal<readonly File[]>([]);
  protected readonly problemaDosAnexos = computed(() => problemaDosAnexos(this.anexos()));
  private readonly temAnotacao = computed(() => !!this.anotacoes().trim() || this.anexos().length > 0);

  protected readonly posVenda = computed(() => this.tipo() === 'POS_VENDA');
  /** Pós-venda: contratos com régua e não cancelados. Avulso: qualquer contrato, inclusive cancelado. */
  protected readonly contratos = computed(() =>
    this.ficha().contratos.filter(
      (c) => !this.posVenda() || (c.versaoRegua !== null && c.situacao !== 'CANCELADO'),
    ),
  );
  protected readonly podeConfirmar = computed(
    () =>
      !this.gravando() &&
      (this.posVenda() ? this.previsao() !== null : true) &&
      (!this.exigeRetorno() || !!this.retorno()) &&
      !this.problemaDosAnexos() &&
      this.anotacoes().length <= REGRAS_DA_ANOTACAO.maximoDeCaracteres,
  );
  protected readonly exigeRetorno = computed(() => this.resultado() === 'REAGENDADO');

  constructor() {
    effect(() => this.reiniciar(this.ficha()));
  }

  protected escolherTipo(tipo: TipoDoFormulario): void {
    this.tipo.set(tipo);
    this.contratoId.set(this.contratoUnico());
    this.invalidarPrevia();
  }

  protected invalidarPrevia(): void {
    this.previsao.set(null);
    this.erro.set('');
  }

  protected prever(): void {
    const contrato = this.contratoEscolhido();
    if (!contrato) {
      this.erro.set('Escolha o contrato do atendimento.');
      return;
    }
    this.erro.set('');
    this.api
      .preverPelaFicha(contrato.id, this.registroDePosVenda(contrato))
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (previa) => this.previsao.set(previa),
        error: (e: unknown) => this.mostrarErro(e),
      });
  }

  protected escolherAnexos(lista: FileList | readonly File[] | null): void {
    this.anexos.update((atuais) => [...atuais, ...Array.from(lista ?? [])]);
  }

  protected removerAnexo(anexo: File): void {
    this.anexos.update((atuais) => atuais.filter((a) => a !== anexo));
  }

  protected registrar(): void {
    if (!this.podeConfirmar()) return;
    const pedido = this.posVenda() ? this.gravarPosVenda() : this.gravarAvulso();
    if (!pedido) return;
    this.gravando.set(true);
    pedido.pipe(switchMap((gravado) => this.anotar(gravado)), takeUntilDestroyed(this.destroy)).subscribe({
      next: (mensagem) => {
        this.gravando.set(false);
        this.registrado.emit(mensagem);
      },
      error: (e: unknown) => {
        this.gravando.set(false);
        this.mostrarErro(e);
      },
    });
  }

  /** O atendimento já está gravado: falha nas anotações não desfaz o registro, só avisa. */
  private anotar(gravado: AtendimentoGravado): Observable<string> {
    if (!this.temAnotacao()) return of('Atendimento registrado.');
    return this.assistente.anotar(gravado.id, this.anotacoes(), this.anexos()).pipe(
      map(() => 'Atendimento registrado. As anotações serão resumidas pela IA em instantes.'),
      catchError((e: unknown) =>
        of(`Atendimento registrado, mas as anotações não foram guardadas: ${problemaDe(e).detail ?? 'erro inesperado'}`),
      ),
    );
  }

  private gravarPosVenda(): Observable<AtendimentoGravado> | null {
    const contrato = this.contratoEscolhido();
    if (!contrato) return null;
    return this.api.registrarPelaFicha(contrato.id, this.registroDePosVenda(contrato));
  }

  private gravarAvulso(): Observable<AtendimentoGravado> {
    return this.api.registrarAvulso({
      associadoId: this.ficha().id,
      contratoId: this.contratoId(),
      tipo: this.tipo() as TipoAvulso,
      resultado: this.resultado(),
      retorno: this.retorno() || null,
      descricao: this.descricao(),
      realizadoEm: this.realizadoEm() || null,
    });
  }

  private registroDePosVenda(contrato: ContratoDaFicha): RegistroContato {
    return {
      versao: contrato.versaoRegua ?? 0,
      resultado: this.resultado(),
      retorno: this.retorno() || null,
      descricao: this.descricao(),
      realizadoEm: this.realizadoEm() || null,
    };
  }

  private contratoEscolhido(): ContratoDaFicha | null {
    return this.contratos().find((c) => c.id === this.contratoId()) ?? null;
  }

  private contratoUnico(): number | null {
    const contratos = this.contratos();
    return this.posVenda() && contratos.length === 1 ? contratos[0].id : null;
  }

  private reiniciar(_ficha: FichaAssociado): void {
    this.resultado.set('CONCLUIDO');
    this.retorno.set('');
    this.realizadoEm.set('');
    this.descricao.set('');
    this.anotacoes.set('');
    this.anexos.set([]);
    this.contratoId.set(this.contratoUnico());
    this.invalidarPrevia();
  }

  private mostrarErro(erro: unknown): void {
    const problema = problemaDe(erro);
    const mensagem = problema.detail ?? 'Não foi possível registrar o atendimento.';
    if (problema.codigo === 'edicao-simultanea') {
      this.desatualizado.emit(mensagem);
      return;
    }
    this.erro.set(mensagem);
  }
}
