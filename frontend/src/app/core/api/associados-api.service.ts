import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Fila, Perfil, PotencialIndicacao, SituacaoContrato } from './carteira-api.service';

export type TipoContato = 'WHATSAPP' | 'TELEFONE' | 'EMAIL';

export interface ContatoDaFicha {
  id: number;
  tipo: TipoContato;
  /** Telefone como 55 + DDD + número; e-mail como gravado. */
  valor: string;
  /** No formato da RG-02, ex.: (37) 9 9869-2950. */
  exibicao: string;
  principal: boolean;
  /** Falso = opt-out: o número aparece, mas não é oferecido para mensagem (RG-11). */
  aceitaContato: boolean;
}

export interface EtapaPrevista {
  codigo: string;
  nome: string;
  situacao: 'CONCLUIDA' | 'PENDENTE' | 'FUTURA';
  prevista: string | null;
}

export interface ContratoDaFicha {
  id: number;
  versao: number;
  placa: string;
  modelo: string;
  tipoVeiculoId: number;
  tipoVeiculo: string;
  mensalidade: number;
  situacao: SituacaoContrato;
  codigoSga: string | null;
  aderidoEm: string;
  vendidoEm: string | null;
  encerradoEm: string | null;
  vendedorId: string | null;
  vendedorNome: string | null;
  fila: Fila;
  diasAtraso: number;
  etapaPendente: string | null;
  proximoContato: string | null;
  regua: EtapaPrevista[];
}

export interface AtendimentoDoHistorico {
  id: number;
  contratoId: number | null;
  placa: string | null;
  tipo: string;
  etapa: string | null;
  resultado: 'CONCLUIDO' | 'PENDENTE' | 'SEM_RESPOSTA' | 'REAGENDADO';
  descricao: string | null;
  retornoAgendadoPara: string | null;
  realizadoEm: string;
  registradoPor: string;
  /** Já existe um registro de correção para este atendimento (ATD-04). */
  corrigido: boolean;
  corrigeAtendimentoId: number | null;
}

export interface IndicacaoFeita {
  id: number;
  indicadoNome: string;
  status: string;
  registradaEm: string;
}

/** CAR-03 · Ficha do associado. */
export interface FichaAssociado {
  id: number;
  nome: string;
  /** Sempre mascarado; o completo vem de `documento()` (RG-04). */
  documento: string | null;
  pessoaFisica: boolean;
  dataNascimento: string | null;
  municipioId: number | null;
  cidade: string | null;
  uf: string | null;
  perfil: Perfil;
  potencial: PotencialIndicacao;
  anonimizado: boolean;
  /** Para a edição simultânea (RG-06). */
  versao: number;
  contatos: ContatoDaFicha[];
  contratos: ContratoDaFicha[];
  atendimentos: AtendimentoDoHistorico[];
  indicacoes: IndicacaoFeita[];
}

export interface PessoaCadastrada {
  id: number;
  nome: string;
  cidade: string | null;
  uf: string | null;
}

export interface DadosPessoais {
  nome: string;
  dataNascimento: string | null;
  municipioId: number;
}

export interface NovoContato {
  tipo: TipoContato;
  valor: string;
  principal: boolean;
  aceitaContato: boolean;
}

/** Associados: busca pelo documento, ficha, contatos, classificação e anonimização. */
@Injectable({ providedIn: 'root' })
export class AssociadosApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/interno/associados`;
  private readonly gestao = `${environment.apiBase}/gestao/associados`;

  /** CAR-02 · 404 quando ninguém tem o documento. */
  porDocumento(documento: string): Observable<PessoaCadastrada> {
    return this.http.get<PessoaCadastrada>(this.base, { params: { documento } });
  }

  ficha(id: number): Observable<FichaAssociado> {
    return this.http.get<FichaAssociado>(`${this.base}/${id}`);
  }

  /** RG-04 · Cada chamada fica registrada na auditoria. */
  documento(id: number): Observable<{ documento: string }> {
    return this.http.get<{ documento: string }>(`${this.base}/${id}/documento`);
  }

  corrigirDados(id: number, versao: number, dados: DadosPessoais): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/dados-pessoais`, { versao, ...dados });
  }

  classificar(id: number, versao: number, perfil: Perfil, potencial: PotencialIndicacao): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/classificacao`, { versao, perfil, potencial });
  }

  adicionarContato(id: number, versao: number, contato: NovoContato): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/contatos`, { versao, ...contato });
  }

  alterarContato(id: number, versao: number, contatoId: number, contato: NovoContato): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/contatos/${contatoId}`, { versao, ...contato });
  }

  removerContato(id: number, versao: number, contatoId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}/contatos/${contatoId}`, { params: { versao } });
  }

  /** CAR-08 · Só o gerencial; `confirmacao` precisa ser ANONIMIZAR. */
  anonimizar(id: number, versao: number, confirmacao: string): Observable<void> {
    return this.http.post<void>(`${this.gestao}/${id}/anonimizacao`, { versao, confirmacao });
  }
}
