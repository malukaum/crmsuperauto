import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { AssociadosApi, FichaAssociado } from '../../../core/api/associados-api.service';
import { problemaDe } from '../../../core/api/problema';
import { Cidade } from '../../../core/api/referencias-api.service';
import { CampoCidade } from '../../../shared/campo-cidade/campo-cidade';
import { ESTILOS_DIALOGO } from './estilos-dialogo';

/** Corrigir nome, nascimento (só pessoa física) e cidade. Gerencial e vendedor; o atendente só altera contatos. */
@Component({
  selector: 'app-dados-pessoais-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, CampoCidade],
  template: `
    <h2 mat-dialog-title>Editar dados do cliente</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="campos">
        <mat-form-field>
          <mat-label>Nome</mat-label>
          <input matInput formControlName="nome" required />
          @if (form.controls.nome.touched && form.controls.nome.invalid) {
            <mat-error>Informe o nome, com até 200 caracteres.</mat-error>
          }
        </mat-form-field>
        @if (ficha.pessoaFisica) {
          <mat-form-field>
            <mat-label>Nascimento</mat-label>
            <input matInput type="date" formControlName="dataNascimento" />
          </mat-form-field>
        }
        <app-campo-cidade [obrigatorio]="true" [cidade]="cidade()" (escolhida)="cidade.set($event)" />
        @if (erro(); as e) {
          <p class="erro" role="alert">{{ e }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="salvando()">Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: ESTILOS_DIALOGO,
})
export class DadosPessoaisDialog {
  protected readonly ficha = inject<FichaAssociado>(MAT_DIALOG_DATA);
  private readonly api = inject(AssociadosApi);
  private readonly ref = inject<MatDialogRef<DadosPessoaisDialog, boolean>>(MatDialogRef);

  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly cidade = signal<Cidade | null>(
    this.ficha.municipioId && this.ficha.cidade
      ? { id: this.ficha.municipioId, nome: this.ficha.cidade, uf: this.ficha.uf ?? '' }
      : null,
  );

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: [this.ficha.nome, [Validators.required, Validators.maxLength(200)]],
    dataNascimento: [this.ficha.dataNascimento ?? ''],
  });

  protected salvar(): void {
    const cidade = this.cidade();
    if (this.form.invalid || !cidade) {
      this.form.markAllAsTouched();
      this.erro.set(cidade ? null : 'Escolha a cidade na lista.');
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.api
      .corrigirDados(this.ficha.id, this.ficha.versao, {
        nome: v.nome.trim(),
        dataNascimento: this.ficha.pessoaFisica && v.dataNascimento ? v.dataNascimento : null,
        municipioId: cidade.id,
      })
      .subscribe({
        next: () => this.ref.close(true),
        error: (e: unknown) => {
          this.salvando.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível salvar os dados.');
        },
      });
  }
}
