import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { AssociadosApi, FichaAssociado } from '../../../core/api/associados-api.service';
import { problemaDe } from '../../../core/api/problema';

export const PALAVRA_DE_CONFIRMACAO = 'ANONIMIZAR';

/** CAR-08 · Anonimizar cliente (LGPD). Irreversível: a confirmação exige digitar ANONIMIZAR. */
@Component({
  selector: 'app-anonimizar-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Anonimizar {{ ficha.nome }}?</h2>
    <mat-dialog-content class="crm-campos">
      <p class="crm-ajuda">
        Nome, documento, nascimento e contatos são apagados e a conta da área do cliente é desativada. Contratos e
        atendimentos continuam nos relatórios, mas a descrição dos atendimentos é apagada. Não há como desfazer.
      </p>
      <mat-form-field>
        <mat-label>Digite {{ palavra }} para confirmar</mat-label>
        <input matInput [formControl]="confirmacao" autocomplete="off" />
      </mat-form-field>
      @if (erro(); as e) {
        <p class="crm-erro" role="alert">{{ e }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
      <button
        mat-flat-button
        type="button"
        [disabled]="salvando() || confirmacao.value !== palavra"
        (click)="anonimizar()"
      >
        Anonimizar
      </button>
    </mat-dialog-actions>
  `,
})
export class AnonimizarDialog {
  protected readonly ficha = inject<FichaAssociado>(MAT_DIALOG_DATA);
  private readonly api = inject(AssociadosApi);
  private readonly ref = inject<MatDialogRef<AnonimizarDialog, boolean>>(MatDialogRef);

  protected readonly palavra = PALAVRA_DE_CONFIRMACAO;
  protected readonly confirmacao = new FormControl('', { nonNullable: true });
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected anonimizar(): void {
    this.salvando.set(true);
    this.api.anonimizar(this.ficha.id, this.ficha.versao, this.confirmacao.value).subscribe({
      next: () => this.ref.close(true),
      error: (e: unknown) => {
        this.salvando.set(false);
        this.erro.set(problemaDe(e).detail ?? 'Não foi possível anonimizar.');
      },
    });
  }
}
