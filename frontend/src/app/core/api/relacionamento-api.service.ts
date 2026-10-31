import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SituacaoContrato } from './carteira-api.service';
import { environment } from '../../../environments/environment';

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
export interface ContatoDoMes {
  id: number;
  associadoId: number;
  nome: string;
  placa: string | null;
  tipo: string;
  etapa: string | null;
  resultado: string;
  descricao: string | null;
  realizadoEm: string;
  registradoPor: string;
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
}

@Injectable({ providedIn: 'root' })
export class RelacionamentoApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;
  atendimentosMes(): Observable<ContatoDoMes[]> {
    return this.http.get<ContatoDoMes[]>(`${this.base}/interno/atendimentos`);
  }
  painel(): Observable<PainelResumo> {
    return this.http.get<PainelResumo>(`${this.base}/interno/painel`);
  }
  fila(fila: FilaDeContatos, filtros: FiltroDaFila): Observable<LinhaTrabalho[]> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([nome, valor]) => {
      if (valor !== null && valor !== '') params = params.set(nome, valor);
    });
    return this.http.get<LinhaTrabalho[]>(`${this.base}/interno/filas/${fila}`, { params });
  }
  aniversarios(dias: number): Observable<LinhaTrabalho[]> {
    return this.http.get<LinhaTrabalho[]>(`${this.base}/interno/aniversariantes`, {
      params: { dias },
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
  registrarAniversario(id: number, confirmarRepeticao: boolean): Observable<void> {
    return this.http.post<void>(`${this.base}/interno/aniversariantes/contratos/${id}/registro`, {
      confirmarRepeticao,
    });
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
