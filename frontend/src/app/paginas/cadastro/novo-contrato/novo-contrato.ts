import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { AssociadosApi, PessoaCadastrada } from '../../../core/api/associados-api.service';
import { ContratosApi, NovoContrato as NovoContratoDto } from '../../../core/api/contratos-api.service';
import { ProblemaApi, problemaDe } from '../../../core/api/problema';
import { Cidade, ReferenciasApi } from '../../../core/api/referencias-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { AvisoMensalidade } from '../../../shared/aviso-mensalidade/aviso-mensalidade';
import { CampoCidade } from '../../../shared/campo-cidade/campo-cidade';
import { normalizarDocumento, normalizarPlaca } from '../../../shared/formatos/formatos';

const TAMANHO_CPF = 11;
const PLACA = /^[A-Za-z]{3}-?[0-9][A-Za-z0-9][0-9]{2}$/;
const NAO_ENCONTRADO = 404;

/** Etapa do cadastro: primeiro o documento, depois o resto (com ou sem os dados da pessoa). */
type Etapa = 'documento' | 'dados';

/** CAR-02 · Cadastrar cliente, veículo e contrato. O cadastro começa pelo CPF/CNPJ. */
@Component({
  selector: 'app-novo-contrato',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    AvisoMensalidade,
    CampoCidade,
  ],
  templateUrl: './novo-contrato.html',
  styleUrl: './novo-contrato.scss',
})
export class NovoContrato {
  private readonly associados = inject(AssociadosApi);
  private readonly contratos = inject(ContratosApi);
  private readonly referencias = inject(ReferenciasApi);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);

  /** O gerencial escolhe o vendedor; para o vendedor, é ele mesmo. */
  protected readonly gerencial = computed(() => this.auth.temAlgumPapel(['gerencial']));
  protected readonly hoje = new Date().toLocaleDateString('sv-SE', { timeZone: 'America/Sao_Paulo' });

  protected readonly etapa = signal<Etapa>('documento');
  protected readonly ocupado = signal(false);
  protected readonly erro = signal<string | null>(null);
  /** Placa recusada por já ter contrato vigente: link para a ficha existente. */
  protected readonly fichaDaPlaca = signal<number | null>(null);
  protected readonly errosServidor = signal<Record<string, string>>({});
  protected readonly pessoaExistente = signal<PessoaCadastrada | null>(null);
  protected readonly cidade = signal<Cidade | null>(null);

  protected readonly tiposVeiculo = toSignal(this.referencias.tiposVeiculo$.pipe(catchError(() => of([]))), {
    initialValue: [],
  });
  protected readonly vendedores = toSignal(
    this.gerencial() ? this.referencias.vendedores().pipe(catchError(() => of([]))) : of([]),
    { initialValue: [] },
  );

  protected readonly documento = this.fb.control('', [Validators.required, Validators.pattern(/^[0-9A-Za-z./\-\s]{11,18}$/)]);

  protected readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    whatsapp: ['', Validators.required],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    dataNascimento: [''],
    placa: ['', [Validators.required, Validators.pattern(PLACA)]],
    modelo: ['', [Validators.required, Validators.maxLength(150)]],
    tipoVeiculoId: [0, Validators.min(1)],
    mensalidade: [null as number | null, [Validators.required, Validators.min(0)]],
    dataAdesao: ['', Validators.required],
    dataVenda: [''],
    codigoSga: ['', Validators.maxLength(30)],
    vendedorId: [''],
  });

  protected readonly pessoaFisica = computed(() => normalizarDocumento(this.documentoInformado()).length === TAMANHO_CPF);
  private readonly documentoInformado = signal('');

  protected continuar(): void {
    if (this.documento.invalid) {
      this.documento.markAsTouched();
      return;
    }
    const documento = normalizarDocumento(this.documento.value);
    this.ocupado.set(true);
    this.erro.set(null);
    this.associados.porDocumento(documento).subscribe({
      next: (pessoa) => this.irParaDados(documento, pessoa),
      error: (e: unknown) => {
        const p = problemaDe(e);
        if (p.status === NAO_ENCONTRADO) {
          this.irParaDados(documento, null);
        } else {
          this.ocupado.set(false);
          this.erro.set(p.detail ?? 'Não foi possível consultar o documento.');
        }
      },
    });
  }

  protected trocarDocumento(): void {
    this.etapa.set('documento');
    this.pessoaExistente.set(null);
    this.erro.set(null);
  }

  protected salvar(): void {
    this.errosServidor.set({});
    this.fichaDaPlaca.set(null);
    if (this.form.invalid || (!this.pessoaExistente() && !this.cidade())) {
      this.form.markAllAsTouched();
      this.erro.set('Confira os campos destacados.');
      return;
    }
    this.ocupado.set(true);
    this.erro.set(null);
    this.contratos.cadastrar(this.montar()).subscribe({
      next: (r) => this.router.navigate(['/carteira/associados', r.associadoId]),
      error: (e: unknown) => this.mostrarErro(problemaDe(e)),
    });
  }

  protected erroCampo(campo: keyof typeof this.form.controls): string | null {
    const servidor = this.errosServidor()[campo];
    if (servidor) return servidor;
    const c = this.form.controls[campo];
    if (!c.errors || !c.touched) return null;
    if (c.errors['required'] || c.errors['min']) return 'Campo obrigatório.';
    if (c.errors['email']) return 'Informe um e-mail válido.';
    if (c.errors['pattern']) return 'Use o padrão antigo (ABC1234) ou Mercosul (ABC1D23).';
    if (c.errors['maxlength']) return 'Texto longo demais.';
    return null;
  }

  private irParaDados(documento: string, pessoa: PessoaCadastrada | null): void {
    this.ocupado.set(false);
    this.documentoInformado.set(documento);
    this.pessoaExistente.set(pessoa);
    // Pessoa já cadastrada: só o novo veículo e contrato são informados.
    for (const campo of ['nome', 'whatsapp'] as const) {
      const controle = this.form.controls[campo];
      if (pessoa) controle.disable();
      else controle.enable();
    }
    this.form.controls.vendedorId.setValidators(this.gerencial() ? Validators.required : null);
    this.form.controls.vendedorId.updateValueAndValidity();
    this.etapa.set('dados');
  }

  private montar(): NovoContratoDto {
    const v = this.form.getRawValue();
    const cidade = this.cidade();
    return {
      documento: this.documentoInformado(),
      pessoa:
        this.pessoaExistente() || !cidade
          ? null
          : {
              nome: v.nome.trim(),
              whatsapp: v.whatsapp,
              email: v.email.trim() || null,
              dataNascimento: this.pessoaFisica() && v.dataNascimento ? v.dataNascimento : null,
              municipioId: cidade.id,
            },
      veiculo: { placa: normalizarPlaca(v.placa), modelo: v.modelo.trim(), tipoVeiculoId: v.tipoVeiculoId },
      contrato: {
        mensalidade: Number(v.mensalidade),
        dataAdesao: v.dataAdesao,
        dataVenda: v.dataVenda || null,
        codigoSga: v.codigoSga.trim() || null,
        vendedorId: this.gerencial() ? v.vendedorId : null,
      },
    };
  }

  private mostrarErro(p: ProblemaApi): void {
    this.ocupado.set(false);
    if (p.codigo === 'placa-com-contrato-vigente') {
      this.fichaDaPlaca.set(Number(p['associadoId']));
    }
    if (p.campos) {
      this.errosServidor.set(Object.fromEntries(Object.entries(p.campos).map(([k, m]) => [k.split('.').pop()!, m])));
    }
    this.erro.set(p.detail ?? 'Não foi possível cadastrar o contrato.');
  }
}
