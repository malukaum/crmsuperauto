import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';

import {
  AjusteDoProvedor,
  AssistenteApi,
  ProvedorConfigurado,
  ProvedorDeIa,
  RotaConfigurada,
  UsoPorProvedor,
} from '../../../core/api/assistente-api.service';
import { problemaDe } from '../../../core/api/problema';

/** O formulário de um provedor: a chave digitada nunca vem do servidor e some depois de salvar. */
interface ProvedorEmEdicao {
  original: ProvedorConfigurado;
  ajuste: AjusteDoProvedor;
  teste: string;
  testeOk: boolean | null;
}

/** IA-05 · Mesmas regras do servidor: ligar exige modelo e chave (nova ou já guardada). */
export function podeLigar(edicao: Pick<ProvedorEmEdicao, 'original' | 'ajuste'>): boolean {
  return !!edicao.ajuste.modelo?.trim() && (!!edicao.ajuste.chave?.trim() || edicao.original.comChave);
}

/** IA-05 · O reserva precisa ser outro provedor, como o servidor exige. */
export function reservaValida(rota: Pick<RotaConfigurada, 'principal' | 'reserva'>): boolean {
  return rota.reserva === null || rota.reserva !== rota.principal;
}

/**
 * IA-05 · Configuração do assistente pelo gerencial: Claude, Gemini e ChatGPT (modelo, chave, ligado, limite e preços),
 * o principal e o reserva de cada tarefa, o teste de conexão e o uso do mês. A chave só vai do navegador para o
 * servidor; nunca volta.
 */
@Component({
  selector: 'app-configuracao-ia',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CurrencyPipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    RouterLink,
  ],
  templateUrl: './configuracao-ia.html',
  styleUrl: './configuracao-ia.scss',
})
export class ConfiguracaoIa {
  private readonly api = inject(AssistenteApi);
  private readonly destroy = inject(DestroyRef);

  protected readonly podeLigar = podeLigar;
  protected readonly reservaValida = reservaValida;
  protected readonly provedores = signal<ProvedorEmEdicao[]>([]);
  protected readonly rotas = signal<RotaConfigurada[]>([]);
  protected readonly uso = signal<UsoPorProvedor[]>([]);
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');

  constructor() {
    this.carregar();
  }

  protected nomeDo(provedor: ProvedorDeIa | null): string {
    return this.provedores().find((p) => p.original.provedor === provedor)?.original.nome ?? '—';
  }

  protected salvarProvedor(edicao: ProvedorEmEdicao): void {
    this.executar(this.api.ajustarProvedor(edicao.original.provedor, edicao.ajuste), `${edicao.original.nome} salvo.`);
  }

  protected testar(edicao: ProvedorEmEdicao): void {
    edicao.teste = 'Testando...';
    edicao.testeOk = null;
    this.provedores.update((lista) => [...lista]);
    this.api
      .testar(edicao.original.provedor, edicao.ajuste.modelo, edicao.ajuste.chave)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => this.mostrarTeste(edicao, r.mensagem, r.conectou),
        error: (e: unknown) => this.mostrarTeste(edicao, problemaDe(e).detail ?? 'Não foi possível testar.', false),
      });
  }

  protected salvarRota(rota: RotaConfigurada): void {
    if (!reservaValida(rota)) return;
    this.executar(this.api.definirTarefa(rota), `${rota.nome}: provedores salvos.`);
  }

  protected usoDe(provedor: ProvedorDeIa): UsoPorProvedor | undefined {
    return this.uso().find((u) => u.provedor === provedor);
  }

  private mostrarTeste(edicao: ProvedorEmEdicao, mensagem: string, conectou: boolean): void {
    edicao.teste = mensagem;
    edicao.testeOk = conectou;
    this.provedores.update((lista) => [...lista]);
  }

  private executar(pedido: Observable<unknown>, mensagem: string): void {
    this.ocupado.set(true);
    this.erro.set('');
    this.sucesso.set('');
    pedido.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.sucesso.set(mensagem);
        this.carregar();
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.erro.set(problemaDe(e).detail ?? 'Não foi possível salvar.');
      },
    });
  }

  private carregar(): void {
    this.ocupado.set(true);
    forkJoin({ provedores: this.api.provedores(), rotas: this.api.tarefas(), uso: this.api.uso() })
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: ({ provedores, rotas, uso }) => {
          this.provedores.set(provedores.map((p) => this.emEdicao(p)));
          this.rotas.set(rotas.map((r) => ({ ...r })));
          this.uso.set(uso);
          this.ocupado.set(false);
        },
        error: (e: unknown) => {
          this.ocupado.set(false);
          this.erro.set(problemaDe(e).detail ?? 'Não foi possível carregar a configuração.');
        },
      });
  }

  private emEdicao(p: ProvedorConfigurado): ProvedorEmEdicao {
    return {
      original: p,
      ajuste: {
        versao: p.versao,
        modelo: p.modelo,
        chave: null,
        ativo: p.ativo,
        limiteMensal: p.limiteMensal,
        precoEntrada: p.precoEntrada,
        precoSaida: p.precoSaida,
      },
      teste: '',
      testeOk: null,
    };
  }
}
