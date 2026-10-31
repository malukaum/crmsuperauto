import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface Cidade {
  id: number;
  nome: string;
  uf: string;
}

export interface Opcao {
  id: number;
  codigo: string;
  nome: string;
}

export interface VendedorDaEquipe {
  id: string;
  nome: string;
}

/** Listas de apoio dos formulários e filtros. Tipos e etapas quase não mudam: ficam em memória na sessão. */
@Injectable({ providedIn: 'root' })
export class ReferenciasApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/interno`;

  readonly tiposVeiculo$ = this.http.get<Opcao[]>(`${this.base}/referencias/tipos-veiculo`).pipe(shareReplay(1));
  readonly etapas$ = this.http.get<Opcao[]>(`${this.base}/referencias/etapas`).pipe(shareReplay(1));

  cidades(nome: string): Observable<Cidade[]> {
    return this.http.get<Cidade[]>(`${this.base}/referencias/cidades`, { params: { nome } });
  }

  /** Gerencial e atendente (o vendedor não filtra nem escolhe vendedor). */
  vendedores(): Observable<VendedorDaEquipe[]> {
    return this.http.get<VendedorDaEquipe[]>(`${this.base}/equipe/vendedores`);
  }
}
