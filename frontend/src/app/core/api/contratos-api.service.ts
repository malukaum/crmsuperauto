import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SituacaoContrato } from './carteira-api.service';

/** CAR-02 · `pessoa` só é usada quando o documento ainda não está na carteira. */
export interface NovoContrato {
  documento: string;
  pessoa: {
    nome: string;
    whatsapp: string;
    email: string | null;
    dataNascimento: string | null;
    municipioId: number;
  } | null;
  veiculo: { placa: string; modelo: string; tipoVeiculoId: number };
  contrato: {
    mensalidade: number;
    dataAdesao: string;
    dataVenda: string | null;
    codigoSga: string | null;
    /** Só o gerencial escolhe; para o vendedor, é ele mesmo. */
    vendedorId: string | null;
  };
}

export interface ContratoRegistrado {
  contratoId: number;
  associadoId: number;
  associadoNovo: boolean;
}

/** CAR-05 · O contrato como o gerencial quer deixá-lo. */
export interface AlteracaoContrato {
  versao: number;
  situacao: SituacaoContrato;
  mensalidade: number;
  vendedorId: string;
  codigoSga: string | null;
}

export interface ResultadoAtivacao {
  ativados: number;
  ignorados: number;
}

@Injectable({ providedIn: 'root' })
export class ContratosApi {
  private readonly http = inject(HttpClient);
  private readonly interno = `${environment.apiBase}/interno/contratos`;
  private readonly gestao = `${environment.apiBase}/gestao/contratos`;

  cadastrar(novo: NovoContrato): Observable<ContratoRegistrado> {
    return this.http.post<ContratoRegistrado>(this.interno, novo);
  }

  alterar(id: number, alteracao: AlteracaoContrato): Observable<void> {
    return this.http.put<void>(`${this.gestao}/${id}`, alteracao);
  }

  /** Marca vários contratos "a conferir" como ativos (proposta). */
  ativarEmLote(contratoIds: number[]): Observable<ResultadoAtivacao> {
    return this.http.post<ResultadoAtivacao>(`${this.gestao}/ativacao-em-lote`, { contratoIds });
  }
}
