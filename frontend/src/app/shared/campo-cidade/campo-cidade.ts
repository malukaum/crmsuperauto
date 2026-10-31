import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { Cidade, ReferenciasApi } from '../../core/api/referencias-api.service';

const ESPERA_DIGITACAO_MS = 250;
const MINIMO_PARA_BUSCAR = 2;

/** Escolha da cidade pelo nome, sem acento (RG-05), entre os municípios do IBGE: a UF vem junto e nunca contradiz. */
@Component({
  selector: 'app-campo-cidade',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-form-field [subscriptSizing]="subscript()" class="campo">
      <mat-label>{{ rotulo() }}</mat-label>
      <input
        matInput
        [formControl]="texto"
        [matAutocomplete]="lista"
        autocomplete="off"
        [required]="obrigatorio()"
        (blur)="aoSair()"
      />
      <mat-autocomplete #lista="matAutocomplete" [displayWith]="exibir" (optionSelected)="escolher($event)">
        @for (c of sugestoes(); track c.id) {
          <mat-option [value]="c">{{ c.nome }} / {{ c.uf }}</mat-option>
        }
      </mat-autocomplete>
      @if (erro()) {
        <mat-error>{{ erro() }}</mat-error>
      }
    </mat-form-field>
  `,
  styles: `
    .campo {
      width: 100%;
    }
  `,
})
export class CampoCidade {
  private readonly api = inject(ReferenciasApi);

  readonly rotulo = input('Cidade / UF');
  readonly obrigatorio = input(false);
  readonly erro = input<string | null>(null);
  readonly subscript = input<'fixed' | 'dynamic'>('fixed');
  /** A cidade já escolhida (ex.: ao editar ou ao voltar com filtros). */
  readonly cidade = input<Cidade | null>(null);
  readonly escolhida = output<Cidade | null>();

  protected readonly texto = new FormControl<string | Cidade>('', { nonNullable: true });
  protected readonly sugestoes = signal<Cidade[]>([]);

  constructor() {
    effect(() => this.texto.setValue(this.cidade() ?? '', { emitEvent: false }));
    this.texto.valueChanges
      .pipe(
        debounceTime(ESPERA_DIGITACAO_MS),
        distinctUntilChanged(),
        switchMap((v) =>
          typeof v === 'string' && v.trim().length >= MINIMO_PARA_BUSCAR
            ? this.api.cidades(v.trim()).pipe(catchError(() => of([])))
            : of([]),
        ),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((lista) => this.sugestoes.set(lista));
  }

  protected exibir(c: Cidade | string | null): string {
    return typeof c === 'string' ? c : c ? `${c.nome} / ${c.uf}` : '';
  }

  protected escolher(evento: MatAutocompleteSelectedEvent): void {
    this.escolhida.emit(evento.option.value as Cidade);
  }

  /** Texto apagado limpa a escolha. */
  protected aoSair(): void {
    if (this.texto.value === '') {
      this.escolhida.emit(null);
    }
  }
}
