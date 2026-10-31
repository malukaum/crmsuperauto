import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PapelEquipe } from '../auth/papel';
import { Problema } from './api.service';

export interface UsuarioEquipe {
  id: string;
  username: string;
  nome: string | null;
  sobrenome: string | null;
  email: string | null;
  papel: PapelEquipe;
  ativo: boolean;
  /** Ainda não definiu a senha pelo link do e-mail. */
  aguardandoSenha: boolean;
  /** Contratos não cancelados em que é o vendedor. */
  contratosVigentes: number;
}

export interface NovoUsuarioEquipe {
  username: string;
  nome: string;
  sobrenome: string;
  email: string;
  papel: PapelEquipe;
}

export interface UsuarioCriado {
  usuario: UsuarioEquipe;
  emailEnviado: boolean;
}

/** ProblemDetail com os extras que a gestão de usuários devolve. */
export interface ProblemaUsuarios extends Problema {
  codigo?: string;
  contratosVigentes?: number;
  campos?: Record<string, string>;
}

/** ACE-04 · API de gestão de usuários da equipe (só gerencial). */
@Injectable({ providedIn: 'root' })
export class UsuariosEquipeApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/gestao/usuarios`;

  listar(): Observable<UsuarioEquipe[]> {
    return this.http.get<UsuarioEquipe[]>(this.base);
  }

  criar(novo: NovoUsuarioEquipe): Observable<UsuarioCriado> {
    return this.http.post<UsuarioCriado>(this.base, novo);
  }

  alterarPapel(id: string, papel: PapelEquipe): Observable<UsuarioEquipe> {
    return this.http.put<UsuarioEquipe>(`${this.base}/${id}/papel`, { papel });
  }

  desativar(id: string, transferirParaId?: string): Observable<UsuarioEquipe> {
    return this.http.post<UsuarioEquipe>(`${this.base}/${id}/desativar`, { transferirParaId: transferirParaId ?? null });
  }

  reativar(id: string): Observable<UsuarioEquipe> {
    return this.http.post<UsuarioEquipe>(`${this.base}/${id}/reativar`, {});
  }

  reenviarConvite(id: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/convite`, {});
  }
}

export function problemaDe(erro: unknown): ProblemaUsuarios {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return { status: 0, detail: 'Não foi possível falar com o servidor. Verifique a conexão e tente novamente.' };
    }
    if (erro.error && typeof erro.error === 'object') {
      return { status: erro.status, ...(erro.error as ProblemaUsuarios) };
    }
    return { status: erro.status, detail: 'Ocorreu um erro inesperado. Tente novamente.' };
  }
  return { detail: 'Ocorreu um erro inesperado. Tente novamente.' };
}

export function nomeCompleto(u: Pick<UsuarioEquipe, 'nome' | 'sobrenome' | 'username'>): string {
  return [u.nome, u.sobrenome].filter(Boolean).join(' ').trim() || u.username;
}
