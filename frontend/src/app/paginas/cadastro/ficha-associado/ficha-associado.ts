import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ComponentType } from '@angular/cdk/portal';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { catchError, combineLatest, filter, of, switchMap, tap } from 'rxjs';

import {
  AssociadosApi,
  ContatoDaFicha,
  ContratoDaFicha,
  FichaAssociado as Ficha,
} from '../../../core/api/associados-api.service';
import { problemaDe } from '../../../core/api/problema';
import { AuthService } from '../../../core/auth/auth.service';
import { GERENCIAL_OU_VENDEDOR, Papel } from '../../../core/auth/papel';
import { TemPapelDirective } from '../../../core/auth/tem-papel.directive';
import { ConfirmarDialog, DadosConfirmar } from '../../acesso/usuarios-equipe/confirmar-dialog';
import { AvisoMensalidade } from '../../../shared/aviso-mensalidade/aviso-mensalidade';
import {
  NOME_PERFIL,
  NOME_POTENCIAL,
  NOME_RESULTADO,
  NOME_SITUACAO,
  NOME_STATUS_INDICACAO,
  NOME_TIPO_CONTATO,
  chipDaFila,
  descreverFila,
  filaMaisUrgente,
  formatarData,
  linkWhatsapp,
} from '../../../shared/formatos/formatos';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { AnonimizarDialog } from './anonimizar-dialog';
import { ClassificacaoDialog } from './classificacao-dialog';
import { ContatoDialog, DadosContatoDialog } from './contato-dialog';
import { ContratoDialog } from './contrato-dialog';
import { DadosPessoaisDialog } from './dados-pessoais-dialog';

const SO_GERENCIAL: readonly Papel[] = ['gerencial'];

/** CAR-03 · Ver a ficha do cliente: dados, contatos, contratos com a régua, histórico e indicações. */
@Component({
  selector: 'app-ficha-associado',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTooltipModule,
    AvisoMensalidade,
    StatusChip,
    TemPapelDirective,
  ],
  templateUrl: './ficha-associado.html',
  styleUrl: './ficha-associado.scss',
})
export class FichaAssociado {
  private readonly api = inject(AssociadosApi);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  /** Id do associado, vindo da rota (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly gerencialOuVendedor = GERENCIAL_OU_VENDEDOR;
  protected readonly soGerencial = SO_GERENCIAL;
  protected readonly nomePerfil = NOME_PERFIL;
  protected readonly nomePotencial = NOME_POTENCIAL;
  protected readonly nomeSituacao = NOME_SITUACAO;
  protected readonly nomeResultado = NOME_RESULTADO;
  protected readonly nomeStatusIndicacao = NOME_STATUS_INDICACAO;
  protected readonly nomeTipoContato = NOME_TIPO_CONTATO;
  protected readonly chipDaFila = chipDaFila;
  protected readonly descreverFila = descreverFila;
  protected readonly formatarData = formatarData;

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly ficha = signal<Ficha | null>(null);
  /** Documento completo depois de "mostrar" (RG-04); cada exibição fica na auditoria. */
  protected readonly documentoCompleto = signal<string | null>(null);
  private readonly recarga = signal(0);

  /** O WhatsApp principal, usado no botão de conversa (FIL-05). */
  protected readonly whatsapp = computed(() =>
    this.ficha()?.contatos.find((c) => c.tipo === 'WHATSAPP' && c.principal) ?? null,
  );
  protected readonly filaDoCabecalho = computed(() => {
    const contratos = this.ficha()?.contratos ?? [];
    const fila = filaMaisUrgente(contratos.map((c) => c.fila));
    const contrato = contratos.find((c) => c.fila === fila);
    return fila && contrato ? { fila, diasAtraso: contrato.diasAtraso } : null;
  });
  /** CAR-08 · Só com todos os contratos cancelados (proposta). */
  protected readonly podeAnonimizar = computed(() => {
    const f = this.ficha();
    return !!f && !f.anonimizado && f.contratos.every((c) => c.situacao === 'CANCELADO');
  });
  protected readonly gerencial = computed(() => this.auth.temAlgumPapel(SO_GERENCIAL));

  constructor() {
    combineLatest([toObservable(this.id), toObservable(this.recarga)])
      .pipe(
        tap(() => {
          this.carregando.set(true);
          this.erro.set(null);
        }),
        switchMap(([id]) =>
          this.api.ficha(Number(id)).pipe(
            catchError((e: unknown) => {
              this.erro.set(problemaDe(e).detail ?? 'Não foi possível abrir a ficha.');
              return of(null);
            }),
          ),
        ),
      )
      .subscribe((ficha) => {
        this.ficha.set(ficha);
        this.carregando.set(false);
      });
  }

  protected linkWhatsapp(contato: ContatoDaFicha): string {
    return linkWhatsapp(contato.valor);
  }

  protected mostrarDocumento(): void {
    const f = this.ficha();
    if (!f) return;
    this.api.documento(f.id).subscribe({
      next: (r) => this.documentoCompleto.set(r.documento),
      error: (e: unknown) => this.avisar(problemaDe(e).detail ?? 'Não foi possível mostrar o documento.'),
    });
  }

  protected editarDados(): void {
    this.abrir(DadosPessoaisDialog, this.ficha(), 'Dados atualizados.');
  }

  protected classificar(): void {
    this.abrir(ClassificacaoDialog, this.ficha(), 'Classificação atualizada.');
  }

  protected novoContato(): void {
    const f = this.ficha();
    if (f) this.abrir<DadosContatoDialog>(ContatoDialog, { associadoId: f.id, versao: f.versao }, 'Contato incluído.');
  }

  protected alterarContato(contato: ContatoDaFicha): void {
    const f = this.ficha();
    if (f) this.abrir<DadosContatoDialog>(ContatoDialog, { associadoId: f.id, versao: f.versao, contato }, 'Contato alterado.');
  }

  protected removerContato(contato: ContatoDaFicha): void {
    const f = this.ficha();
    if (!f) return;
    const dados: DadosConfirmar = {
      titulo: 'Remover este contato?',
      mensagem: `${NOME_TIPO_CONTATO[contato.tipo]} ${contato.exibicao}. A remoção fica registrada na auditoria.`,
      acao: 'Remover',
    };
    this.dialog
      .open<ConfirmarDialog, DadosConfirmar, boolean>(ConfirmarDialog, { data: dados })
      .afterClosed()
      .pipe(
        filter((ok) => ok === true),
        switchMap(() => this.api.removerContato(f.id, f.versao, contato.id)),
      )
      .subscribe({
        next: () => this.atualizar('Contato removido.'),
        error: (e: unknown) => this.falhou(e),
      });
  }

  protected alterarContrato(contrato: ContratoDaFicha): void {
    this.abrir(ContratoDialog, contrato, 'Contrato atualizado.');
  }

  protected anonimizar(): void {
    this.abrir(AnonimizarDialog, this.ficha(), 'Cliente anonimizado.');
  }


  /** Abre um diálogo que grava sozinho; ao gravar, recarrega a ficha (a versão mudou). */
  private abrir<D>(dialogo: ComponentType<unknown>, dados: D | null, sucesso: string): void {
    if (!dados) return;
    this.dialog
      .open(dialogo, { data: dados, autoFocus: 'first-tabbable' })
      .afterClosed()
      .pipe(filter((ok) => ok === true))
      .subscribe(() => this.atualizar(sucesso));
  }

  private atualizar(mensagem: string): void {
    this.documentoCompleto.set(null);
    this.recarga.update((n) => n + 1);
    this.avisar(mensagem);
  }

  /** RG-06: se outra pessoa alterou a ficha, oferece recarregar. */
  private falhou(erro: unknown): void {
    const p = problemaDe(erro);
    const ref = this.snack.open(p.detail ?? 'Não foi possível concluir.', p.codigo === 'edicao-simultanea' ? 'Recarregar' : 'OK', {
      duration: 8000,
    });
    if (p.codigo === 'edicao-simultanea') {
      ref.onAction().subscribe(() => this.recarga.update((n) => n + 1));
    }
  }

  private avisar(mensagem: string): void {
    this.snack.open(mensagem, 'OK', { duration: 5000 });
  }

  protected voltar(): void {
    // Voltar pelo histórico mantém filtros, ordem e página da carteira (proposta da CAR-01).
    if (history.length > 1) {
      history.back();
    } else {
      this.router.navigate(['/carteira']);
    }
  }
}
