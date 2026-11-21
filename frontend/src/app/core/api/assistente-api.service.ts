import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export type SituacaoDoResumo = 'AGUARDANDO' | 'RESUMIDO' | 'FALHOU';
export type Satisfacao = 'POSITIVA' | 'NEUTRA' | 'NEGATIVA';
export type ChanceDeIndicar = 'BAIXA' | 'MEDIA' | 'ALTA';
export type Assunto =
  | 'ASSISTENCIA_24H'
  | 'GUINCHO'
  | 'MENSALIDADE'
  | 'SINISTRO'
  | 'CANCELAMENTO'
  | 'INDICACAO'
  | 'VEICULO'
  | 'DOCUMENTACAO'
  | 'ATENDIMENTO'
  | 'OUTRO';
export type ProvedorDeIa = 'CLAUDE' | 'GEMINI' | 'CHATGPT';
export type TarefaDeIa = 'RESUMO' | 'TENDENCIAS' | 'RECOMENDACOES';

export const NOME_SITUACAO_DO_RESUMO: Record<SituacaoDoResumo, string> = {
  AGUARDANDO: 'Aguardando resumo',
  RESUMIDO: 'Resumido pela IA',
  FALHOU: 'Não foi possível resumir',
};
export const NOME_SATISFACAO: Record<Satisfacao, string> = {
  POSITIVA: 'Positiva',
  NEUTRA: 'Neutra',
  NEGATIVA: 'Negativa',
};
export const NOME_CHANCE_DE_INDICAR: Record<ChanceDeIndicar, string> = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta' };
export const NOME_ASSUNTO: Record<Assunto, string> = {
  ASSISTENCIA_24H: 'Assistência 24h',
  GUINCHO: 'Guincho',
  MENSALIDADE: 'Mensalidade',
  SINISTRO: 'Sinistro',
  CANCELAMENTO: 'Cancelamento',
  INDICACAO: 'Indicação',
  VEICULO: 'Veículo',
  DOCUMENTACAO: 'Documentação',
  ATENDIMENTO: 'Qualidade do atendimento',
  OUTRO: 'Outro',
};

/** IA-01 · As mesmas regras do servidor (NovaAnotacao / NovoAnexo), para avisar antes de enviar. */
export const REGRAS_DA_ANOTACAO = {
  maximoDeCaracteres: 10_000,
  maximoDeAnexos: 5,
  tamanhoMaximo: 10 * 1024 * 1024,
  tiposAceitos: ['application/pdf', 'image/jpeg', 'image/png', 'text/plain'],
  extensoesAceitas: '.pdf,.jpg,.jpeg,.png,.txt',
} as const;

/** Devolve o motivo de recusa dos anexos, ou null se estão dentro das regras. */
export function problemaDosAnexos(anexos: readonly File[]): string | null {
  if (anexos.length > REGRAS_DA_ANOTACAO.maximoDeAnexos) return 'Anexe no máximo 5 arquivos por atendimento.';
  const tipos: readonly string[] = REGRAS_DA_ANOTACAO.tiposAceitos;
  const recusado = anexos.find((a) => !tipos.includes(a.type));
  if (recusado) return `"${recusado.name}": aceitos só PDF, JPG, PNG e texto.`;
  const grande = anexos.find((a) => a.size === 0 || a.size > REGRAS_DA_ANOTACAO.tamanhoMaximo);
  if (grande) return `"${grande.name}": o arquivo precisa ter até 10 MB e não pode estar vazio.`;
  return null;
}

export interface AnexoAnotado {
  nome: string;
  tipoConteudo: string;
  tamanho: number;
  disponivel: boolean;
}

/** IA-01 / IA-02 · A anotação de um atendimento e o resumo da IA (espelha ResumoDaAnotacao). */
export interface AnotacaoDoAtendimento {
  atendimentoId: number;
  situacao: SituacaoDoResumo;
  texto: string | null;
  anexos: AnexoAnotado[];
  resumoCurto: string | null;
  assuntos: string[];
  satisfacao: Satisfacao | null;
  objecoes: string[];
  interesses: string[];
  riscoDeCancelamento: boolean;
  chanceDeIndicar: ChanceDeIndicar | null;
  proximaAcao: string | null;
  provedor: string | null;
  modelo: string | null;
  resumidoEm: string | null;
  erro: string | null;
}

export interface Tendencias {
  atendimentosResumidos: number;
  porAssunto: Partial<Record<Assunto, number>>;
  porSatisfacao: Partial<Record<Satisfacao, number>>;
  comRiscoDeCancelamento: number;
  comChanceAltaDeIndicar: number;
}

export interface LeituraDasTendencias {
  texto: string;
  provedor: string | null;
  modelo: string | null;
}

export interface Recomendacao {
  abordagem: string;
  base: string;
}

export interface Sugestoes {
  recomendacoes: Recomendacao[];
  provedor: string;
  modelo: string;
}

export interface ProvedorConfigurado {
  provedor: ProvedorDeIa;
  nome: string;
  modelo: string | null;
  ativo: boolean;
  comChave: boolean;
  chaveFinal: string | null;
  limiteMensal: number;
  precoEntrada: number | null;
  precoSaida: number | null;
  gastoDoMes: number;
  versao: number;
}

export interface AjusteDoProvedor {
  versao: number;
  modelo: string | null;
  /** Vazio mantém a chave guardada. */
  chave: string | null;
  ativo: boolean;
  limiteMensal: number;
  precoEntrada: number | null;
  precoSaida: number | null;
}

export interface RotaConfigurada {
  tarefa: TarefaDeIa;
  nome: string;
  principal: ProvedorDeIa;
  reserva: ProvedorDeIa | null;
  versao: number;
}

export interface UsoPorProvedor {
  provedor: ProvedorDeIa;
  chamadas: number;
  falhas: number;
  comoReserva: number;
  custo: number;
}

export interface ResultadoDoTeste {
  conectou: boolean;
  mensagem: string;
}

export interface PeriodoDasTendencias {
  inicio?: string;
  fim?: string;
}

/** Cliente HTTP do assistente de IA (IA-01 a IA-05). As chamadas à IA saem só do servidor. */
@Injectable({ providedIn: 'root' })
export class AssistenteApi {
  private readonly http = inject(HttpClient);
  private readonly interno = `${environment.apiBase}/interno`;
  private readonly gestao = `${environment.apiBase}/gestao/ia`;

  anotar(atendimentoId: number, texto: string, anexos: readonly File[]): Observable<AnotacaoDoAtendimento> {
    const corpo = new FormData();
    if (texto.trim()) corpo.append('texto', texto);
    anexos.forEach((a) => corpo.append('anexos', a, a.name));
    return this.http.post<AnotacaoDoAtendimento>(`${this.interno}/atendimentos/${atendimentoId}/anotacao`, corpo);
  }

  anotacoesDoAssociado(associadoId: number): Observable<AnotacaoDoAtendimento[]> {
    return this.http.get<AnotacaoDoAtendimento[]>(`${this.interno}/associados/${associadoId}/anotacoes`);
  }

  recomendar(associadoId: number): Observable<Sugestoes> {
    return this.http.post<Sugestoes>(`${this.interno}/associados/${associadoId}/recomendacoes`, null);
  }

  tendencias(periodo: PeriodoDasTendencias): Observable<Tendencias> {
    return this.http.get<Tendencias>(`${this.interno}/assistente/tendencias`, { params: this.parametros(periodo) });
  }

  leitura(periodo: PeriodoDasTendencias): Observable<LeituraDasTendencias> {
    return this.http.post<LeituraDasTendencias>(`${this.interno}/assistente/tendencias/leitura`, null, {
      params: this.parametros(periodo),
    });
  }

  provedores(): Observable<ProvedorConfigurado[]> {
    return this.http.get<ProvedorConfigurado[]>(`${this.gestao}/provedores`);
  }

  ajustarProvedor(provedor: ProvedorDeIa, ajuste: AjusteDoProvedor): Observable<ProvedorConfigurado> {
    return this.http.put<ProvedorConfigurado>(`${this.gestao}/provedores/${provedor}`, ajuste);
  }

  testar(provedor: ProvedorDeIa, modelo: string | null, chave: string | null): Observable<ResultadoDoTeste> {
    return this.http.post<ResultadoDoTeste>(`${this.gestao}/provedores/${provedor}/teste`, { modelo, chave });
  }

  tarefas(): Observable<RotaConfigurada[]> {
    return this.http.get<RotaConfigurada[]>(`${this.gestao}/tarefas`);
  }

  definirTarefa(rota: RotaConfigurada): Observable<RotaConfigurada> {
    return this.http.put<RotaConfigurada>(`${this.gestao}/tarefas/${rota.tarefa}`, {
      versao: rota.versao,
      principal: rota.principal,
      reserva: rota.reserva,
    });
  }

  uso(): Observable<UsoPorProvedor[]> {
    return this.http.get<UsoPorProvedor[]>(`${this.gestao}/uso`);
  }

  private parametros(periodo: PeriodoDasTendencias): HttpParams {
    let params = new HttpParams();
    if (periodo.inicio) params = params.set('inicio', periodo.inicio);
    if (periodo.fim) params = params.set('fim', periodo.fim);
    return params;
  }
}
