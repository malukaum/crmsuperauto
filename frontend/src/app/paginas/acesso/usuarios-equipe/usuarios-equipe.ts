import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EMPTY, Observable, catchError, defer, filter, finalize, switchMap } from 'rxjs';

import {
  UsuarioCriado,
  UsuarioEquipe,
  UsuariosEquipeApi,
  nomeCompleto,
  problemaDe,
} from '../../../core/api/usuarios-equipe-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NOME_PAPEL, PAPEIS_EQUIPE, PapelEquipe } from '../../../core/auth/papel';
import { StatusChip } from '../../../shared/status-chip/status-chip';
import { ConfirmarDialog, DadosConfirmar } from './confirmar-dialog';
import { DadosDesativar, DesativarUsuarioDialog } from './desativar-usuario-dialog';
import { NovoUsuarioDialog } from './novo-usuario-dialog';

/** ACE-04 · Gerenciar usuários da equipe (Configurações → Usuários). */
@Component({
  selector: 'app-usuarios-equipe',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, MatTooltipModule, StatusChip],
  templateUrl: './usuarios-equipe.html',
  styleUrl: './usuarios-equipe.scss',
})
export class UsuariosEquipe {
  private readonly api = inject(UsuariosEquipeApi);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly auth = inject(AuthService);

  protected readonly papeis = PAPEIS_EQUIPE;
  protected readonly nomePapel = NOME_PAPEL;

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly usuarios = signal<UsuarioEquipe[]>([]);
  /** Ação em andamento (id do usuário), para desabilitar o menu daquela linha. */
  protected readonly ocupado = signal<string | null>(null);

  protected readonly resumo = computed(() => {
    const lista = this.usuarios();
    const ativos = lista.filter((u) => u.ativo).length;
    return { ativos, inativos: lista.length - ativos };
  });

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(problemaDe(e).detail ?? 'Não foi possível carregar os usuários.');
        this.carregando.set(false);
      },
    });
  }

  protected nome(u: UsuarioEquipe): string {
    return nomeCompleto(u);
  }

  protected ehVoce(u: UsuarioEquipe): boolean {
    return u.username === this.auth.username();
  }

  protected novo(): void {
    this.dialog
      .open<NovoUsuarioDialog, void, UsuarioCriado>(NovoUsuarioDialog, { autoFocus: 'first-tabbable' })
      .afterClosed()
      .pipe(filter((r): r is UsuarioCriado => !!r))
      .subscribe((criado) => {
        this.carregar();
        this.avisar(
          criado.emailEnviado
            ? `Usuário ${criado.usuario.username} criado. O link para definir a senha foi enviado para ${criado.usuario.email}.`
            : `Usuário ${criado.usuario.username} criado, mas o e-mail não pôde ser enviado. Use "Reenviar convite".`,
        );
      });
  }

  protected trocarPapel(u: UsuarioEquipe, papel: PapelEquipe): void {
    if (papel === u.papel) return;
    const dados: DadosConfirmar = {
      titulo: `Trocar o papel de ${this.nome(u)}?`,
      mensagem: `De ${NOME_PAPEL[u.papel]} para ${NOME_PAPEL[papel]}. As sessões abertas dessa pessoa são encerradas e o novo papel vale a partir do próximo login.`,
      acao: 'Trocar papel',
    };
    this.confirmar(dados)
      .pipe(switchMap(() => this.executar(u, this.api.alterarPapel(u.id, papel))))
      .subscribe((atualizado) => {
        this.substituir(atualizado);
        this.avisar(`${this.nome(u)} agora é ${NOME_PAPEL[papel]}.`);
      });
  }

  protected desativar(u: UsuarioEquipe): void {
    const vendedores = this.usuarios().filter((v) => v.ativo && v.papel === 'vendedor' && v.id !== u.id);
    this.dialog
      .open<DesativarUsuarioDialog, DadosDesativar, UsuarioEquipe>(DesativarUsuarioDialog, {
        data: { usuario: u, vendedores },
      })
      .afterClosed()
      .pipe(filter((r): r is UsuarioEquipe => !!r))
      .subscribe(() => {
        // Recarrega: a transferência muda a contagem de contratos de outro vendedor.
        this.carregar();
        this.avisar(`${this.nome(u)} foi desativado.`);
      });
  }

  protected reativar(u: UsuarioEquipe): void {
    this.confirmar({
      titulo: `Reativar ${this.nome(u)}?`,
      mensagem: 'A pessoa volta a conseguir entrar no CRM com o mesmo usuário e senha.',
      acao: 'Reativar',
    })
      .pipe(switchMap(() => this.executar(u, this.api.reativar(u.id))))
      .subscribe((atualizado) => {
        this.substituir(atualizado);
        this.avisar(`${this.nome(u)} foi reativado.`);
      });
  }

  protected reenviarConvite(u: UsuarioEquipe): void {
    this.executar(u, this.api.reenviarConvite(u.id)).subscribe(() =>
      this.avisar(`Link para definir a senha reenviado para ${u.email}.`),
    );
  }

  // ---------- apoio ----------

  private confirmar(dados: DadosConfirmar) {
    return this.dialog
      .open<ConfirmarDialog, DadosConfirmar, boolean>(ConfirmarDialog, { data: dados })
      .afterClosed()
      .pipe(filter((ok): ok is true => ok === true));
  }

  /** Roda a chamada marcando a linha como ocupada; erros viram aviso e encerram o fluxo. */
  private executar<T>(u: UsuarioEquipe, chamada: Observable<T>): Observable<T> {
    return defer(() => {
      this.ocupado.set(u.id);
      return chamada;
    }).pipe(
      catchError((e: unknown) => {
        this.avisar(problemaDe(e).detail ?? 'Não foi possível concluir a operação.');
        return EMPTY;
      }),
      finalize(() => this.ocupado.set(null)),
    );
  }

  private substituir(atualizado: UsuarioEquipe): void {
    this.usuarios.update((lista) => lista.map((u) => (u.id === atualizado.id ? atualizado : u)));
  }

  private avisar(mensagem: string): void {
    this.snack.open(mensagem, 'OK', { duration: 6000 });
  }
}
