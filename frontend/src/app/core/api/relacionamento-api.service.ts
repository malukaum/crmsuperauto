import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { FormatoExportacao, SituacaoContrato, parametros } from './carteira-api.service';
import { environment } from '../../../environments/environment';
import { PaginaApi, PaginaSolicitada } from './pagina';

export interface LinhaTrabalho {
  contratoId: number;
  associadoId: number;
  nome: string;
  whatsapp: string | null;
  aceitaWhatsapp: boolean;
  modelo: string;
  placa: string;
  tipoVeiculo: string;
  mensalidade: number;
  cidade: string | null;
  situacao: SituacaoContrato;
  etapa: string | null;
  data: string | null;
  diasAtraso: number;
  versao: number;
  mensagemEnviada: boolean;
}
export interface PainelResumo {
  carteira: number;
  porTipo: Record<string, number>;
  contatosHoje: number;
  atrasados: number;
  aniversariantesHoje: number;
  aniversariantes7Dias: number;
  mensalidadesAtivas: number;
  atendimentosMes: number;
  aConferir: number;
  situacoes: Record<string, number>;
  proximosContatos: LinhaTrabalho[];
  proximosAniversariantes: LinhaTrabalho[];
}
export type TipoAtendimento =
  | 'POS_VENDA'
  | 'ANIVERSARIO'
  | 'SUPORTE'
  | 'COMERCIAL'
  | 'REAPROXIMACAO'
  | 'INDICACAO';
export type TipoAvulso = 'SUPORTE' | 'COMERCIAL' | 'REAPROXIMACAO' | 'INDICACAO';
export const NOME_TIPO_ATENDIMENTO: Record<TipoAtendimento, string> = {
  POS_VENDA: 'Pós-venda',
  ANIVERSARIO: 'Aniversário',
  SUPORTE: 'Suporte',
  COMERCIAL: 'Comercial',
  REAPROXIMACAO: 'Reaproximação',
  INDICACAO: 'Indicação',
};
export const TIPOS_AVULSOS: readonly TipoAvulso[] = ['SUPORTE', 'COMERCIAL', 'REAPROXIMACAO', 'INDICACAO'];
/** ATD-05 · Uma linha do histórico de atendimentos. */
export interface LinhaHistorico {
  id: number;
  associadoId: number;
  associado: string;
  cidade: string | null;
  placa: string | null;
  tipoCodigo: TipoAtendimento;
  tipo: string;
  etapa: string | null;
  resultado: ResultadoContato;
  descricao: string | null;
  proximaAcao: string;
  retornoAgendadoPara: string | null;
  realizadoEm: string;
  registradoPorId: string;
  registradoPor: string;
  corrigido: boolean;
  corrigeAtendimentoId: number | null;
}
export interface FiltroHistorico {
  inicio: string | null;
  fim: string | null;
  tipo: TipoAtendimento | null;
  resultado: ResultadoContato | null;
  usuario: string | null;
  cidade: number | null;
  busca: string;
}
export interface AutorDeAtendimento {
  id: string;
  nome: string;
}
export interface RegistroAvulso {
  associadoId: number;
  contratoId: number | null;
  tipo: TipoAvulso;
  resultado: ResultadoContato;
  retorno: string | null;
  descricao: string;
  realizadoEm: string | null;
}
export interface CorrecaoAtendimento {
  resultado: ResultadoContato;
  retorno: string | null;
  descricao: string;
}
export interface AjusteRegua {
  versao: number;
  etapaId: number;
  retorno: string | null;
}
export interface EtapaRegua {
  id: number;
  nome: string;
  codigo: string;
  diasAposAnterior: number;
  proximaEtapaId: number;
  inicial: boolean;
}
export interface PreviaRegua {
  contratosAfetados: number;
  confirmacao: string;
}
export interface FiltroDaFila {
  busca: string;
  cidade: number | null;
  etapa: number | null;
  tipoVeiculo: number | null;
}
export type FilaDeContatos = 'hoje' | 'atrasados';
export type ResultadoContato = 'CONCLUIDO' | 'SEM_RESPOSTA' | 'REAGENDADO' | 'PENDENTE';
export interface PreviaContato {
  etapaConcluida: string | null;
  proximaEtapa: string;
  proximoContato: string | null;
}
export interface RegistroContato {
  versao: number;
  resultado: ResultadoContato;
  retorno: string | null;
  descricao: string;
  /** Vazio = agora; senão até 7 dias atrás, como data e hora de São Paulo (ATD-01). */
  realizadoEm?: string | null;
}

@Injectable({ providedIn: 'root' })
export class RelacionamentoApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;
  /** ATD-05 · Sem datas, o backend usa o mês corrente. */
  historico(filtro: FiltroHistorico, pagina: PaginaSolicitada): Observable<PaginaApi<LinhaHistorico>> {
    return this.http.get<PaginaApi<LinhaHistorico>>(`${this.base}/interno/atendimentos`, {
      params: parametros({ ...filtro, ...pagina }),
    });
  }
  autores(): Observable<AutorDeAtendimento[]> {
    return this.http.get<AutorDeAtendimento[]>(`${this.base}/interno/atendimentos/autores`);
  }
  exportarHistorico(filtro: FiltroHistorico, formato: FormatoExportacao): Observable<Blob> {
    return this.http.get(`${this.base}/interno/atendimentos/exportacao`, {
      params: parametros({ ...filtro, formato }),
      responseType: 'blob',
    });
  }
  /** ATD-01 pela ficha: aceita contrato suspenso; o cancelado é recusado. */
  preverPelaFicha(contratoId: number, registro: RegistroContato): Observable<PreviaContato> {
    return this.http.post<PreviaContato>(
      `${this.base}/interno/atendimentos/contratos/${contratoId}/previa`,
      registro,
    );
  }
  registrarPelaFicha(contratoId: number, registro: RegistroContato): Observable<void> {
    return this.http.post<void>(
      `${this.base}/interno/atendimentos/contratos/${contratoId}/registro`,
      registro,
    );
  }
  /** ATD-02 · Nunca mexe na régua. */
  registrarAvulso(registro: RegistroAvulso): Observable<void> {
    return this.http.post<void>(`${this.base}/interno/atendimentos/avulsos`, registro);
  }
  /** ATD-04 · Grava um novo registro que aponta para o original. */
  corrigir(atendimentoId: number, correcao: CorrecaoAtendimento): Observable<void> {
    return this.http.post<void>(
      `${this.base}/interno/atendimentos/${atendimentoId}/correcao`,
      correcao,
    );
  }
  /** ATD-04 · Só o gerencial. */
  ajustarRegua(contratoId: number, ajuste: AjusteRegua): Observable<void> {
    return this.http.put<void>(`${this.base}/gestao/regua/contratos/${contratoId}`, ajuste);
  }
  painel(): Observable<PainelResumo> {
    return this.http.get<PainelResumo>(`${this.base}/interno/painel`);
  }
  fila(
    fila: FilaDeContatos,
    filtros: FiltroDaFila,
    pagina: PaginaSolicitada,
  ): Observable<PaginaApi<LinhaTrabalho>> {
    return this.http.get<PaginaApi<LinhaTrabalho>>(`${this.base}/interno/filas/${fila}`, {
      params: parametros({ ...filtros, ...pagina }),
    });
  }
  aniversarios(dias: number, pagina: PaginaSolicitada): Observable<PaginaApi<LinhaTrabalho>> {
    return this.http.get<PaginaApi<LinhaTrabalho>>(`${this.base}/interno/aniversariantes`, {
      params: parametros({ dias, ...pagina }),
    });
  }
  preverContato(id: number, registro: RegistroContato): Observable<PreviaContato> {
    return this.http.post<PreviaContato>(
      `${this.base}/interno/filas/contratos/${id}/previa`,
      registro,
    );
  }
  registrar(id: number, registro: RegistroContato): Observable<void> {
    return this.http.post<void>(`${this.base}/interno/filas/contratos/${id}/registro`, registro);
  }
  /** ATD-03 · Uma mensagem por pessoa no ano: a segunda é recusada (409). */
  registrarAniversario(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/interno/aniversariantes/contratos/${id}/registro`, null);
  }
  etapas(): Observable<EtapaRegua[]> {
    return this.http.get<EtapaRegua[]>(`${this.base}/gestao/regua`);
  }
  prever(prazos: Record<number, number>): Observable<PreviaRegua> {
    return this.http.post<PreviaRegua>(`${this.base}/gestao/regua/previa`, { prazos });
  }
  salvar(prazos: Record<number, number>, confirmacao: string): Observable<void> {
    return this.http.put<void>(`${this.base}/gestao/regua`, { prazos, confirmacao });
  }
}
