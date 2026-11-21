import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { AtendimentoDoHistorico } from '../../../core/api/associados-api.service';
import { problemaDe } from '../../../core/api/problema';
import { RelacionamentoApi, ResultadoContato } from '../../../core/api/relacionamento-api.service';
import { NOME_RESULTADO } from '../../../shared/formatos/formatos';

/**
 * ATD-04 · Corrigir um atendimento: grava um novo registro que aponta para o original, sem apagar nada (RG-07).
 * A régua não é desfeita: se o erro mudou a régua, o gerencial a ajusta no contrato.
 */
@Component({
  selector: 'app-corrigir-atendimento-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Corrigir atendimento</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="crm-campos">
        <p class="crm-ajuda">
          O registro original continua no histórico, marcado como corrigido. A correção não muda a régua.
        </p>
        <mat-form-field>
          <mat-label>Resultado correto</mat-label>
          <mat-select formControlName="resultado">
            @for (r of resultados; track r) {
              <mat-option [value]="r">{{ nomeResultado[r] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>{{ form.controls.resultado.value === 'REAGENDADO' ? 'Data combinada' : 'Data de retorno (opcional)' }}</mat-label>
          <input matInput type="date" formControlName="retorno" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Resumo</mat-label>
          <textarea matInput maxlength="2000" formControlName="descricao"></textarea>
        </mat-form-field>
        @if (erro(); as e) {
          <p class="crm-erro" role="alert">{{ e }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="salvando() || form.invalid">Gravar correção</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class CorrigirAtendimentoDialog {
  protected readonly atendimento = inject<AtendimentoDoHistorico>(MAT_DIALOG_DATA);
  private readonly api = inject(RelacionamentoApi);
  private readonly ref = inject<MatDialogRef<CorrigirAtendimentoDialog, boolean>>(MatDialogRef);

  protected readonly resultados = Object.keys(NOME_RESULTADO) as ResultadoContato[];
  protected readonly nomeResultado = NOME_RESULTADO;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    resultado: [this.atendimento.resultado as ResultadoContato, Validators.required],
    retorno: [this.atendimento.retornoAgendadoPara ?? ''],
    descricao: [this.atendimento.descricao ?? '', Validators.maxLength(2000)],
  });

  protected salvar(): void {
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.api
      .corrigir(this.atendimento.id, { resultado: v.resultado, retorno: v.retorno || null, descricao: v.descricao })
      .subscribe({
        next: () => this.ref.close(true),
        error: (e: unknown) => {
          this.salvando.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível gravar a correção.');
        },
      });
  }
}
