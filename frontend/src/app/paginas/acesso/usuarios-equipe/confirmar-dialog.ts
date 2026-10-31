import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface DadosConfirmar {
  titulo: string;
  mensagem: string;
  acao: string;
}

@Component({
  selector: 'app-confirmar-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content>
      <p class="mensagem">{{ dados.mensagem }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button type="button" [mat-dialog-close]="true">{{ dados.acao }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    .mensagem {
      max-width: 480px;
    }
  `,
})
export class ConfirmarDialog {
  protected readonly dados = inject<DadosConfirmar>(MAT_DIALOG_DATA);
}
