import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { ContratoDaFicha } from '../../../core/api/associados-api.service';
import { problemaDe } from '../../../core/api/problema';
import { ReferenciasApi } from '../../../core/api/referencias-api.service';
import { RelacionamentoApi } from '../../../core/api/relacionamento-api.service';

/**
 * ATD-04 · O gerencial ajusta a etapa pendente e o retorno manual depois de um atendimento lançado errado.
 * A mudança fica na auditoria.
 */
@Component({
  selector: 'app-ajuste-regua-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Ajustar régua · {{ contrato.placa }}</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="crm-campos">
        <p class="crm-ajuda">
          Use depois de corrigir um atendimento que mudou a régua por engano. Sem retorno manual, o próximo contato
          volta a ser calculado pela etapa.
        </p>
        <mat-form-field>
          <mat-label>Etapa pendente</mat-label>
          <mat-select formControlName="etapaId">
            @for (e of etapas(); track e.id) {
              <mat-option [value]="e.id">{{ e.nome }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Retorno manual (opcional)</mat-label>
          <input matInput type="date" formControlName="retorno" />
        </mat-form-field>
        @if (erro(); as e) {
          <p class="crm-erro" role="alert">{{ e }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="salvando() || form.invalid">Ajustar</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class AjusteReguaDialog {
  protected readonly contrato = inject<ContratoDaFicha>(MAT_DIALOG_DATA);
  private readonly api = inject(RelacionamentoApi);
  private readonly ref = inject<MatDialogRef<AjusteReguaDialog, boolean>>(MatDialogRef);

  protected readonly etapas = toSignal(inject(ReferenciasApi).etapas$, { initialValue: [] });
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    etapaId: [null as number | null, Validators.required],
    retorno: [''],
  });

  constructor() {
    effect(() => {
      const pendente = this.etapaPendenteAtual();
      if (pendente !== null && this.form.controls.etapaId.value === null) {
        this.form.controls.etapaId.setValue(pendente);
      }
    });
  }

  protected salvar(): void {
    const v = this.form.getRawValue();
    if (v.etapaId === null) return;
    this.salvando.set(true);
    this.api
      .ajustarRegua(this.contrato.id, {
        versao: this.contrato.versaoRegua ?? 0,
        etapaId: v.etapaId,
        retorno: v.retorno || null,
      })
      .subscribe({
        next: () => this.ref.close(true),
        error: (e: unknown) => {
          this.salvando.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível ajustar a régua.');
        },
      });
  }

  private etapaPendenteAtual(): number | null {
    const codigo = this.contrato.regua.find((e) => e.situacao === 'PENDENTE')?.codigo;
    return this.etapas().find((e) => e.codigo === codigo)?.id ?? null;
  }
}
