import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { forkJoin } from 'rxjs';

import { ApiService, AreaPing, Ping, Resultado, UsuarioLogado } from '../../core/api/api.service';
import { StatusChip, TipoStatus } from '../status-chip/status-chip';

interface LinhaPing {
  area: AreaPing;
  endpoint: string;
  quemAcessa: string;
  resultado: Resultado<Ping>;
}

const PINGS: { area: AreaPing; quemAcessa: string }[] = [
  { area: 'interno', quemAcessa: 'Gerencial, vendedor e atendente' },
  { area: 'gestao', quemAcessa: 'Somente gerencial' },
  { area: 'cliente', quemAcessa: 'Somente cliente' },
];

/**
 * Prova a integração ponta a ponta: chama /api/me e os três /ping com o token do usuário logado
 * e mostra o status devolvido pela API (200 liberado, 403 negado).
 */
@Component({
  selector: 'app-verificacao-integracao',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, StatusChip],
  templateUrl: './verificacao-integracao.html',
  styleUrl: './verificacao-integracao.scss',
})
export class VerificacaoIntegracao {
  private readonly api = inject(ApiService);

  protected readonly carregando = signal(false);
  protected readonly me = signal<Resultado<UsuarioLogado> | null>(null);
  protected readonly pings = signal<LinhaPing[]>([]);

  constructor() {
    this.testar();
  }

  protected testar(): void {
    this.carregando.set(true);
    forkJoin({
      me: this.api.me(),
      interno: this.api.ping('interno'),
      gestao: this.api.ping('gestao'),
      cliente: this.api.ping('cliente'),
    }).subscribe((r) => {
      this.me.set(r.me);
      this.pings.set(
        PINGS.map((p) => ({
          ...p,
          endpoint: `GET /api/${p.area}/ping`,
          resultado: r[p.area],
        })),
      );
      this.carregando.set(false);
    });
  }

  protected tipoDoStatus(status: number): TipoStatus {
    if (status >= 200 && status < 300) return 'ativo';
    if (status === 403) return 'atrasado';
    return 'neutro';
  }

  protected rotuloDoStatus(status: number): string {
    if (status >= 200 && status < 300) return `${status} · liberado`;
    if (status === 401) return '401 · não autenticado';
    if (status === 403) return '403 · negado';
    if (status === 0) return 'API fora do ar';
    return `${status} · erro`;
  }
}
