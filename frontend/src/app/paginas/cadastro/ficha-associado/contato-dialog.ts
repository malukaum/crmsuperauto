import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AssociadosApi, ContatoDaFicha, NovoContato, TipoContato } from '../../../core/api/associados-api.service';
import { problemaDe } from '../../../core/api/problema';
import { NOME_TIPO_CONTATO } from '../../../shared/formatos/formatos';
import { ESTILOS_DIALOGO } from './estilos-dialogo';

export interface DadosContatoDialog {
  associadoId: number;
  versao: number;
  /** Ausente para incluir um contato novo. */
  contato?: ContatoDaFicha;
}

/**
 * CAR-04 · Incluir ou alterar um WhatsApp, telefone ou e-mail. O telefone é aceito com ou sem 55, parênteses,
 * espaços e traços; o DDD é obrigatório. "Não aceita contato" (opt-out) mantém o número visível.
 */
@Component({
  selector: 'app-contato-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ dados.contato ? 'Alterar contato' : 'Novo contato' }}</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="campos">
        <div class="linha">
          <mat-form-field>
            <mat-label>Tipo</mat-label>
            <mat-select formControlName="tipo">
              @for (t of tipos; track t) {
                <mat-option [value]="t">{{ nomeTipo[t] }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>{{ form.controls.tipo.value === 'EMAIL' ? 'E-mail' : 'Número com DDD' }}</mat-label>
            <input matInput formControlName="valor" autocomplete="off" required />
            @if (form.controls.valor.touched && form.controls.valor.invalid) {
              <mat-error>Informe o contato.</mat-error>
            }
          </mat-form-field>
        </div>
        <mat-checkbox formControlName="principal">Principal deste tipo (o WhatsApp principal é o usado nas filas)</mat-checkbox>
        <mat-checkbox formControlName="naoAceitaContato">Não aceita contato (opt-out)</mat-checkbox>
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
export class ContatoDialog {
  protected readonly dados = inject<DadosContatoDialog>(MAT_DIALOG_DATA);
  private readonly api = inject(AssociadosApi);
  private readonly ref = inject<MatDialogRef<ContatoDialog, boolean>>(MatDialogRef);

  protected readonly tipos = Object.keys(NOME_TIPO_CONTATO) as TipoContato[];
  protected readonly nomeTipo = NOME_TIPO_CONTATO;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    tipo: [this.dados.contato?.tipo ?? ('WHATSAPP' as TipoContato)],
    valor: [this.dados.contato?.exibicao ?? '', [Validators.required, Validators.maxLength(254)]],
    principal: [this.dados.contato?.principal ?? false],
    naoAceitaContato: [this.dados.contato ? !this.dados.contato.aceitaContato : false],
  });

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const contato: NovoContato = { tipo: v.tipo, valor: v.valor, principal: v.principal, aceitaContato: !v.naoAceitaContato };
    const { associadoId, versao } = this.dados;
    const chamada = this.dados.contato
      ? this.api.alterarContato(associadoId, versao, this.dados.contato.id, contato)
      : this.api.adicionarContato(associadoId, versao, contato);
    this.salvando.set(true);
    chamada.subscribe({
      next: () => this.ref.close(true),
      error: (e: unknown) => {
        this.salvando.set(false);
        this.erro.set(problemaDe(e).detail ?? 'Não foi possível salvar o contato.');
      },
    });
  }
}
