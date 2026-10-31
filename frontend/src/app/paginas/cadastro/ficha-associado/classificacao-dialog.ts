import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { AssociadosApi, FichaAssociado } from '../../../core/api/associados-api.service';
import { Perfil, PotencialIndicacao } from '../../../core/api/carteira-api.service';
import { problemaDe } from '../../../core/api/problema';
import { NOME_PERFIL, NOME_POTENCIAL } from '../../../shared/formatos/formatos';

/** CAR-06 · Perfil e potencial de indicação. Uso interno: o cliente nunca vê. */
@Component({
  selector: 'app-classificacao-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Classificar cliente</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="crm-campos">
        <p class="crm-ajuda">Uso interno: o cliente nunca vê esta classificação.</p>
        <div class="crm-linha">
          <mat-form-field>
            <mat-label>Perfil</mat-label>
            <mat-select formControlName="perfil">
              @for (p of perfis; track p) {
                <mat-option [value]="p">{{ nomePerfil[p] }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Potencial de indicação</mat-label>
            <mat-select formControlName="potencial">
              @for (p of potenciais; track p) {
                <mat-option [value]="p">{{ nomePotencial[p] }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </div>
        @if (erro(); as e) {
          <p class="crm-erro" role="alert">{{ e }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="salvando()">Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class ClassificacaoDialog {
  protected readonly ficha = inject<FichaAssociado>(MAT_DIALOG_DATA);
  private readonly api = inject(AssociadosApi);
  private readonly ref = inject<MatDialogRef<ClassificacaoDialog, boolean>>(MatDialogRef);

  protected readonly perfis = Object.keys(NOME_PERFIL) as Perfil[];
  protected readonly potenciais = Object.keys(NOME_POTENCIAL) as PotencialIndicacao[];
  protected readonly nomePerfil = NOME_PERFIL;
  protected readonly nomePotencial = NOME_POTENCIAL;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    perfil: [this.ficha.perfil],
    potencial: [this.ficha.potencial],
  });

  protected salvar(): void {
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.api.classificar(this.ficha.id, this.ficha.versao, v.perfil, v.potencial).subscribe({
      next: () => this.ref.close(true),
      error: (e: unknown) => {
        this.salvando.set(false);
        this.erro.set(problemaDe(e).detail ?? 'Não foi possível salvar a classificação.');
      },
    });
  }
}
