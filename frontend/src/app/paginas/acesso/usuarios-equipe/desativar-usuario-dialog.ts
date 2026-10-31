import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';

import {
  UsuarioEquipe,
  UsuariosEquipeApi,
  nomeCompleto,
  problemaDe,
} from '../../../core/api/usuarios-equipe-api.service';

export interface DadosDesativar {
  usuario: UsuarioEquipe;
  /** Vendedores ativos que podem receber os contratos (já sem o próprio usuário). */
  vendedores: UsuarioEquipe[];
}

/**
 * Confirma a desativação. Se a pessoa for vendedora de contratos vigentes, pede para escolher quem
 * fica com eles antes (a API também exige isso e responde 409 se a lista estiver desatualizada).
 */
@Component({
  selector: 'app-desativar-usuario-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatFormFieldModule, MatProgressBarModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Desativar {{ nome }}?</h2>
    <mat-dialog-content class="conteudo">
      <p>
        A pessoa não consegue mais entrar no CRM e as sessões abertas são encerradas. Nada é excluído: os
        atendimentos registrados continuam no histórico com o nome dela.
      </p>
      @if (contratos() > 0) {
        <div class="transferencia">
          <p>
            <strong>{{ nome }}</strong> é vendedor de
            <strong>{{ contratos() }} {{ contratos() === 1 ? 'contrato vigente' : 'contratos vigentes' }}</strong>.
            Escolha quem fica com {{ contratos() === 1 ? 'ele' : 'eles' }}:
          </p>
          @if (dados.vendedores.length) {
            <mat-form-field class="crm-largura-total">
              <mat-label>Transferir contratos para</mat-label>
              <mat-select [value]="destino()" (valueChange)="destino.set($event)" required>
                @for (v of dados.vendedores; track v.id) {
                  <mat-option [value]="v.id">{{ nomeDe(v) }} ({{ v.username }})</mat-option>
                }
              </mat-select>
            </mat-form-field>
          } @else {
            <p class="crm-erro">Não há outro vendedor ativo. Cadastre ou reative um vendedor antes de desativar este.</p>
          }
        </div>
      }
      @if (erro(); as e) {
        <p class="crm-erro" role="alert">{{ e }}</p>
      }
    </mat-dialog-content>
    @if (salvando()) {
      <mat-progress-bar mode="indeterminate" />
    }
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
      <button mat-flat-button type="button" (click)="confirmar()" [disabled]="!podeConfirmar()">
        {{ contratos() > 0 ? 'Transferir e desativar' : 'Desativar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .conteudo {
      max-width: 520px;
    }
    .transferencia {
      margin-top: 8px;
      padding: 12px 16px 4px;
      border-radius: var(--crm-raio-botao);
      background: var(--crm-laranja-fundo);
      color: var(--crm-laranja-texto);
    }
  `,
})
export class DesativarUsuarioDialog {
  protected readonly dados = inject<DadosDesativar>(MAT_DIALOG_DATA);
  private readonly api = inject(UsuariosEquipeApi);
  private readonly ref = inject<MatDialogRef<DesativarUsuarioDialog, UsuarioEquipe>>(MatDialogRef);

  protected readonly nome = nomeCompleto(this.dados.usuario);
  protected readonly contratos = signal(this.dados.usuario.contratosVigentes);
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly destino = signal<string | null>(null);

  protected readonly podeConfirmar = computed(
    () => !this.salvando() && (this.contratos() === 0 || this.destino() !== null),
  );

  protected nomeDe(u: UsuarioEquipe): string {
    return nomeCompleto(u);
  }

  protected confirmar(): void {
    this.salvando.set(true);
    this.erro.set(null);
    const destino = this.contratos() > 0 ? (this.destino() ?? undefined) : undefined;
    this.api.desativar(this.dados.usuario.id, destino).subscribe({
      next: (u) => this.ref.close(u),
      error: (e: unknown) => {
        const p = problemaDe(e);
        this.salvando.set(false);
        if (p.codigo === 'contratos-a-transferir' && p.contratosVigentes) {
          // A lista estava desatualizada: agora sabemos que há contratos a transferir.
          this.contratos.set(p.contratosVigentes);
        }
        this.erro.set(p.detail ?? 'Não foi possível desativar o usuário.');
      },
    });
  }
}
