import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

/** Valores de `contrato.situacao` (RG-12). */
export type SituacaoContrato = 'A_CONFERIR' | 'ATIVO' | 'SUSPENSO' | 'CANCELADO';
/** Fila do contrato, calculada com o "hoje" de São Paulo (RG-14). */
export type Fila = 'HOJE' | 'ATRASADO' | 'FUTURO' | 'SEM_DATA' | 'SUSPENSO' | 'CANCELADO';
export type Perfil = 'REGULAR' | 'PROMOTOR' | 'ATENCAO';
export type PotencialIndicacao = 'NAO_AVALIADO' | 'ALTO' | 'MEDIO' | 'BAIXO';
export type OrdemCarteira =
  | 'NOME'
  | 'PLACA'
  | 'CIDADE'
  | 'MENSALIDADE'
  | 'SITUACAO'
  | 'ETAPA'
  | 'PROXIMO_CONTATO'
  | 'VENDEDOR';

/** CAR-07 · Formatos do arquivo exportado. */
export type FormatoExportacao = 'XLSX' | 'CSV';

export const EXTENSAO_EXPORTACAO: Record<FormatoExportacao, string> = { XLSX: 'xlsx', CSV: 'csv' };

export const TAMANHOS_PAGINA = [10, 25, 50] as const;
export type TamanhoPagina = (typeof TAMANHOS_PAGINA)[number];

/** Uma linha da carteira: um contrato (placa) com a pessoa. */
export interface LinhaDaCarteira {
  contratoId: number;
  associadoId: number;
  associado: string;
  /** Sempre mascarado na lista (RG-04). */
  documento: string | null;
  placa: string;
  modelo: string;
  tipoVeiculo: string;
  cidade: string | null;
  uf: string | null;
  mensalidade: number;
  situacao: SituacaoContrato;
  etapaPendente: string | null;
  /** aaaa-mm-dd */
  proximoContato: string | null;
  fila: Fila;
  diasAtraso: number;
  vendedorId: string | null;
  vendedorNome: string | null;
  /** 55 + DDD + número. */
  whatsapp: string | null;
  whatsappAceitaContato: boolean;
}

export interface PaginaCarteira {
  linhas: LinhaDaCarteira[];
  pagina: number;
  tamanho: number;
  totalContratos: number;
  /** Valor contratado, não recebido (RG-09). */
  somaMensalidades: number;
}

/** Filtros da carteira (CAR-01); o filtro de vendedor é ignorado pela API quando quem pede é vendedor (RG-03). */
export interface FiltroCarteira {
  busca?: string;
  fila?: Fila;
  cidade?: number;
  situacao?: SituacaoContrato;
  excluirCancelados?: boolean;
  etapa?: number;
  tipoVeiculo?: number;
  perfil?: Perfil;
  potencial?: PotencialIndicacao;
  vendedor?: string;
  ordem?: OrdemCarteira;
  decrescente?: boolean;
  pagina?: number;
  tamanho?: TamanhoPagina;
}

/** CAR-01 e CAR-07 · Carteira de clientes. */
@Injectable({ providedIn: 'root' })
export class CarteiraApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/interno/carteira`;

  buscar(filtro: FiltroCarteira): Observable<PaginaCarteira> {
    return this.http.get<PaginaCarteira>(this.base, { params: parametros(filtro) });
  }

  /** Arquivo com a lista exatamente como filtrada (sem a página). */
  exportar(filtro: FiltroCarteira, formato: FormatoExportacao): Observable<Blob> {
    const { pagina: _pagina, tamanho: _tamanho, ...semPagina } = filtro;
    return this.http.get(`${this.base}/exportacao`, {
      params: parametros({ ...semPagina, formato }),
      responseType: 'blob',
    });
  }
}

export function parametros(filtro: object): HttpParams {
  let params = new HttpParams();
  for (const [chave, valor] of Object.entries(filtro)) {
    if (valor !== undefined && valor !== null && valor !== '') {
      params = params.set(chave, String(valor));
    }
  }
  return params;
}
