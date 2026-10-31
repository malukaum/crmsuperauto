import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';

import { environment } from '../../../environments/environment';

export interface ContadoresMenu {
  contatosHoje: number;
  atrasados: number;
}

/**
 * Quantidades mostradas ao lado de "Contatos de hoje" e "Atrasados" no menu.
 * A API já aplica o escopo do papel (o vendedor só conta os contratos dele).
 */
@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly http = inject(HttpClient);

  readonly contadores = signal<ContadoresMenu | null>(null);

  carregar(): void {
    this.http.get<ContadoresMenu>(`${environment.apiBase}/interno/menu/contadores`).subscribe({
      next: (c) => this.contadores.set(c),
      // Sem contador o menu continua funcionando; não vale a pena incomodar o usuário.
      error: () => this.contadores.set(null),
    });
  }
}
