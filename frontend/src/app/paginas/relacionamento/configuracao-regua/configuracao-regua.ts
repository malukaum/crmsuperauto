import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import {
  RelacionamentoApi,
  EtapaRegua,
  PreviaRegua,
} from '../../../core/api/relacionamento-api.service';
import { MenuService } from '../../../core/api/menu.service';
import { problemaDe } from '../../../core/api/problema';
/** FIL-06 · Prévia e confirmação dos prazos da régua pelo gerencial. */
@Component({
  selector: 'app-configuracao-regua',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, RouterLink],
  templateUrl: './configuracao-regua.html',
  styleUrl: './configuracao-regua.scss',
})
export class ConfiguracaoRegua {
  private readonly api = inject(RelacionamentoApi);
  private readonly menu = inject(MenuService);
  private readonly destroy = inject(DestroyRef);
  protected readonly etapas = signal<EtapaRegua[]>([]);
  protected readonly previa = signal<PreviaRegua | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');
  protected readonly prazos = signal<Record<number, number>>({});
  constructor() {
    this.carregar();
  }
  protected carregar(): void {
    this.api
      .etapas()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (etapas) => {
          this.etapas.set(etapas);
          this.prazos.set(Object.fromEntries(etapas.map((e) => [e.id, e.diasAposAnterior])));
        },
        error: (e) =>
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível concluir a operação.'),
      });
  }
  protected alterarPrazo(etapaId: number, dias: number): void {
    this.prazos.update((prazos) => ({ ...prazos, [etapaId]: dias }));
    this.previa.set(null);
  }
  protected prever(): void {
    this.ocupado.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api
      .prever(this.prazos())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (previa) => {
          this.previa.set(previa);
          this.ocupado.set(false);
        },
        error: (e) => {
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível concluir a operação.');
          this.ocupado.set(false);
        },
      });
  }
  protected salvar(): void {
    const previa = this.previa();
    if (!previa || this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.api
      .salvar(this.prazos(), previa.confirmacao)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.ocupado.set(false);
          this.previa.set(null);
          this.sucesso.set('Prazos salvos e próximos contatos recalculados.');
          this.menu.carregar();
        },
        error: (e) => {
          this.ocupado.set(false);
          this.previa.set(null);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível concluir a operação.');
        },
      });
  }
}
