import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface UsuarioLogado {
  username: string;
  nome: string | null;
  email: string | null;
  papeis: string[];
  cpf: string | null;
}

export interface Ping {
  area: string;
  mensagem: string;
  usuario: string;
  dataHora: string;
}

/** Erro no formato ProblemDetail (RFC 9457) devolvido pela API. */
export interface Problema {
  title?: string;
  detail?: string;
  status?: number;
  instance?: string;
}

export interface Resultado<T> {
  status: number;
  corpo?: T;
  problema?: Problema;
}

export type AreaPing = 'interno' | 'gestao' | 'cliente';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBase;

  me(): Observable<Resultado<UsuarioLogado>> {
    return this.chamar<UsuarioLogado>(`${this.base}/me`);
  }

  ping(area: AreaPing): Observable<Resultado<Ping>> {
    return this.chamar<Ping>(`${this.base}/${area}/ping`);
  }

  /** Nunca lança erro: devolve o status e o corpo (ou o ProblemDetail), para a tela decidir o que mostrar. */
  private chamar<T>(url: string): Observable<Resultado<T>> {
    return this.http.get<T>(url, { observe: 'response' }).pipe(
      map((resp) => ({ status: resp.status, corpo: resp.body ?? undefined })),
      catchError((erro: HttpErrorResponse) =>
        of({
          status: erro.status,
          problema: typeof erro.error === 'object' && erro.error ? (erro.error as Problema) : undefined,
        }),
      ),
    );
  }
}
