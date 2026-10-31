import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
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
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import {
  RelacionamentoApi,
  LinhaTrabalho,
  PreviaContato,
  RegistroContato,
  ResultadoContato,
} from '../../../core/api/relacionamento-api.service';
import { problemaDe } from '../../../core/api/problema';
import { formatarData } from '../../../shared/formatos/formatos';
import { mensagemInicial, conversaComMensagem } from './mensagem-do-contato';
/** FIL-02 a FIL-05 · Formulário de registro com prévia, confirmação e edição da mensagem. */
@Component({
  selector: 'app-registro-na-fila',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './registro-na-fila.html',
  styleUrl: './registro-na-fila.scss',
})
export class RegistroNaFila {
  private readonly api = inject(RelacionamentoApi);
  private readonly destroy = inject(DestroyRef);
  private readonly pedidosDePrevia = new Subject<RegistroContato | null>();
  readonly linha = input.required<LinhaTrabalho>();
  readonly aniversario = input(false);
  readonly registrado = output<void>();
  readonly cancelado = output<void>();
  readonly conflito = output<string>();
  readonly ocupacaoAlterada = output<boolean>();
  protected readonly resultado = signal<ResultadoContato>('CONCLUIDO');
  protected readonly retorno = signal('');
  protected readonly descricao = signal('');
  protected readonly textoMensagem = signal('');
  protected readonly erro = signal('');
  protected readonly gravando = signal(false);
  protected readonly repeticao = signal(false);
  protected readonly previsao = signal<PreviaContato | null>(null);
  protected readonly formatarData = formatarData;
  constructor() {
    effect(() => this.reiniciar(this.linha(), this.aniversario()));
    this.pedidosDePrevia
      .pipe(
        switchMap((pedido) =>
          pedido
            ? this.api.preverContato(this.linha().contratoId, pedido).pipe(
                catchError((erro) => {
                  this.mostrarErro(erro);
                  return EMPTY;
                }),
              )
            : EMPTY,
        ),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((previa) => this.previsao.set(previa));
  }
  private reiniciar(linha: LinhaTrabalho, aniversario: boolean): void {
    this.resultado.set('CONCLUIDO');
    this.retorno.set('');
    this.descricao.set('');
    this.erro.set('');
    this.repeticao.set(false);
    this.textoMensagem.set(mensagemInicial(linha, aniversario));
    this.invalidarPrevia();
  }
  protected invalidarPrevia(): void {
    this.previsao.set(null);
    this.pedidosDePrevia.next(null);
  }
  protected whatsapp(): string {
    return conversaComMensagem(this.linha().whatsapp ?? '', this.textoMensagem());
  }
  protected prever(): void {
    this.previsao.set(null);
    this.erro.set('');
    this.pedidosDePrevia.next(this.registro());
  }
  private registro(): RegistroContato {
    return {
      versao: this.linha().versao,
      resultado: this.resultado(),
      retorno: this.retorno() || null,
      descricao: this.descricao(),
    };
  }
  protected registrar(): void {
    if (this.gravando() || (!this.aniversario() && !this.previsao())) return;
    this.definirOcupacao(true);
    this.erro.set('');
    const pedido = this.aniversario()
      ? this.api.registrarAniversario(this.linha().contratoId, this.repeticao())
      : this.api.registrar(this.linha().contratoId, this.registro());
    pedido.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.definirOcupacao(false);
        this.registrado.emit();
      },
      error: (erro) => {
        this.definirOcupacao(false);
        this.mostrarErro(erro);
      },
    });
  }
  private definirOcupacao(gravando: boolean): void {
    this.gravando.set(gravando);
    this.ocupacaoAlterada.emit(gravando);
  }
  private mostrarErro(erro: unknown): void {
    const problema = problemaDe(erro);
    const mensagem = problema.detail ?? 'Não foi possível concluir a operação.';
    this.erro.set(mensagem);
    if (problema.codigo === 'aniversario-ja-registrado') this.repeticao.set(true);
    if (problema.codigo === 'edicao-simultanea' || problema.codigo === 'fila-mudou')
      this.conflito.emit(mensagem);
  }
}
