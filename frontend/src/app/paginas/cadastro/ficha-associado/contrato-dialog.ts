import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, of } from 'rxjs';

import { ContratoDaFicha } from '../../../core/api/associados-api.service';
import { SituacaoContrato } from '../../../core/api/carteira-api.service';
import { ContratosApi } from '../../../core/api/contratos-api.service';
import { problemaDe } from '../../../core/api/problema';
import { ReferenciasApi } from '../../../core/api/referencias-api.service';
import { AvisoMensalidade } from '../../../shared/aviso-mensalidade/aviso-mensalidade';
import { NOME_SITUACAO } from '../../../shared/formatos/formatos';
import { ESTILOS_DIALOGO } from './estilos-dialogo';

/** Mudanças permitidas pela CAR-05; Cancelado é definitivo (proposta). */
const DESTINOS: Record<SituacaoContrato, SituacaoContrato[]> = {
  A_CONFERIR: ['A_CONFERIR', 'ATIVO', 'SUSPENSO', 'CANCELADO'],
  ATIVO: ['ATIVO', 'SUSPENSO', 'CANCELADO'],
  SUSPENSO: ['SUSPENSO', 'ATIVO', 'CANCELADO'],
  CANCELADO: ['CANCELADO'],
};

export function situacoesPermitidas(atual: SituacaoContrato): SituacaoContrato[] {
  return DESTINOS[atual];
}

/** CAR-05 · Atualizar o contrato conforme o SGA: situação, mensalidade, vendedor e código. Só o gerencial. */
@Component({
  selector: 'app-contrato-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    AvisoMensalidade,
  ],
  template: `
    <h2 mat-dialog-title>Contrato {{ contrato.placa }}</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="campos">
        <div class="linha">
          <mat-form-field>
            <mat-label>Situação</mat-label>
            <mat-select formControlName="situacao">
              @for (s of situacoes; track s) {
                <mat-option [value]="s">{{ nomeSituacao[s] }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Mensalidade (R$)</mat-label>
            <input matInput type="number" min="0" step="0.01" formControlName="mensalidade" required />
          </mat-form-field>
        </div>
        <div class="linha">
          <mat-form-field>
            <mat-label>Vendedor</mat-label>
            <mat-select formControlName="vendedorId" required>
              @for (v of vendedores(); track v.id) {
                <mat-option [value]="v.id">{{ v.nome }}</mat-option>
              }
              @if (contrato.vendedorId && !vendedorAtivo()) {
                <mat-option [value]="contrato.vendedorId">{{ contrato.vendedorNome }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Código do SGA</mat-label>
            <input matInput formControlName="codigoSga" />
          </mat-form-field>
        </div>
        <app-aviso-mensalidade />
        @if (cancelando()) {
          <p class="ajuda">
            Cancelar é definitivo: grava a data de encerramento e tira o contrato das filas. O histórico continua.
            Para voltar, cadastre um novo contrato.
          </p>
          <mat-checkbox formControlName="confirmaCancelamento">Confirmo o cancelamento</mat-checkbox>
        }
        @if (erro(); as e) {
          <p class="erro" role="alert">{{ e }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="salvando() || (cancelando() && !confirmou())">Salvar</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: ESTILOS_DIALOGO,
})
export class ContratoDialog {
  protected readonly contrato = inject<ContratoDaFicha>(MAT_DIALOG_DATA);
  private readonly api = inject(ContratosApi);
  private readonly ref = inject<MatDialogRef<ContratoDialog, boolean>>(MatDialogRef);

  protected readonly situacoes = situacoesPermitidas(this.contrato.situacao);
  protected readonly nomeSituacao = NOME_SITUACAO;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly vendedores = toSignal(inject(ReferenciasApi).vendedores().pipe(catchError(() => of([]))), {
    initialValue: [],
  });
  protected readonly vendedorAtivo = computed(() => this.vendedores().some((v) => v.id === this.contrato.vendedorId));

  protected readonly form = inject(NonNullableFormBuilder).group({
    situacao: [this.contrato.situacao],
    mensalidade: [this.contrato.mensalidade, [Validators.required, Validators.min(0)]],
    vendedorId: [this.contrato.vendedorId ?? '', Validators.required],
    codigoSga: [this.contrato.codigoSga ?? '', Validators.maxLength(30)],
    confirmaCancelamento: [false],
  });

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly cancelando = computed(
    () => this.valores().situacao === 'CANCELADO' && this.contrato.situacao !== 'CANCELADO',
  );
  protected readonly confirmou = computed(() => this.valores().confirmaCancelamento === true);

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.api
      .alterar(this.contrato.id, {
        versao: this.contrato.versao,
        situacao: v.situacao,
        mensalidade: Number(v.mensalidade),
        vendedorId: v.vendedorId,
        codigoSga: v.codigoSga.trim() || null,
      })
      .subscribe({
        next: () => this.ref.close(true),
        error: (e: unknown) => {
          this.salvando.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível salvar o contrato.');
        },
      });
  }
}
