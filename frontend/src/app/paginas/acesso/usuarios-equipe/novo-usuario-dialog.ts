import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';

import { UsuarioCriado, UsuariosEquipeApi, problemaDe } from '../../../core/api/usuarios-equipe-api.service';
import { NOME_PAPEL, PAPEIS_EQUIPE, PapelEquipe } from '../../../core/auth/papel';

/** Cadastro de uma pessoa da equipe. A senha é definida por ela, pelo link enviado por e-mail. */
@Component({
  selector: 'app-novo-usuario-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Novo usuário da equipe</h2>
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <mat-dialog-content class="crm-campos">
        <p class="crm-ajuda">A pessoa recebe um e-mail com o link para definir a própria senha.</p>
        <div class="crm-linha">
          <mat-form-field>
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome" autocomplete="off" required />
            @if (erroCampo('nome'); as e) {
              <mat-error>{{ e }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field>
            <mat-label>Sobrenome</mat-label>
            <input matInput formControlName="sobrenome" autocomplete="off" required />
            @if (erroCampo('sobrenome'); as e) {
              <mat-error>{{ e }}</mat-error>
            }
          </mat-form-field>
        </div>
        <mat-form-field>
          <mat-label>E-mail</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="off" required />
          @if (erroCampo('email'); as e) {
            <mat-error>{{ e }}</mat-error>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Usuário (login)</mat-label>
          <input matInput formControlName="username" autocomplete="off" required />
          <mat-hint>Letras sem acento, números, ponto, hífen ou sublinhado.</mat-hint>
          @if (erroCampo('username'); as e) {
            <mat-error>{{ e }}</mat-error>
          }
        </mat-form-field>
        <mat-form-field>
          <mat-label>Papel</mat-label>
          <mat-select formControlName="papel" required>
            @for (p of papeis; track p) {
              <mat-option [value]="p">{{ nomePapel[p] }}</mat-option>
            }
          </mat-select>
          @if (erroCampo('papel'); as e) {
            <mat-error>{{ e }}</mat-error>
          }
        </mat-form-field>
        @if (erro(); as e) {
          <p class="crm-erro" role="alert">{{ e }}</p>
        }
      </mat-dialog-content>
      @if (salvando()) {
        <mat-progress-bar mode="indeterminate" />
      }
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="salvando()">Cancelar</button>
        <button mat-flat-button type="submit" [disabled]="salvando()">Criar usuário</button>
      </mat-dialog-actions>
    </form>
  `,
})
export class NovoUsuarioDialog {
  private readonly api = inject(UsuariosEquipeApi);
  private readonly ref = inject<MatDialogRef<NovoUsuarioDialog, UsuarioCriado>>(MatDialogRef);

  protected readonly papeis = PAPEIS_EQUIPE;
  protected readonly nomePapel = NOME_PAPEL;
  protected readonly salvando = signal(false);
  protected readonly erro = signal<string | null>(null);
  /** Mensagens de campo devolvidas pela API (validação do servidor). */
  private readonly errosServidor = signal<Record<string, string>>({});

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    sobrenome: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    username: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9._-]{3,50}$/)]],
    papel: ['' as PapelEquipe | '', Validators.required],
  });

  protected erroCampo(campo: keyof typeof this.form.controls): string | null {
    const c = this.form.controls[campo];
    const servidor = this.errosServidor()[campo];
    if (servidor) return servidor;
    if (!c.errors || !(c.touched || c.dirty)) return null;
    if (c.errors['required']) return campo === 'papel' ? 'Escolha o papel.' : 'Campo obrigatório.';
    if (c.errors['email']) return 'Informe um e-mail válido.';
    if (c.errors['pattern']) return 'De 3 a 50 caracteres, sem acentos nem espaços.';
    if (c.errors['maxlength']) return 'Texto longo demais.';
    return null;
  }

  protected salvar(): void {
    this.errosServidor.set({});
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set(null);
    this.api
      .criar({ ...v, username: v.username.trim().toLowerCase(), papel: v.papel as PapelEquipe })
      .subscribe({
        next: (criado) => this.ref.close(criado),
        error: (e: unknown) => {
          const p = problemaDe(e);
          this.salvando.set(false);
          if (p.campos) {
            this.errosServidor.set(p.campos);
            this.form.markAllAsTouched();
          }
          this.erro.set(p.detail ?? 'Não foi possível criar o usuário.');
        },
      });
  }
}
