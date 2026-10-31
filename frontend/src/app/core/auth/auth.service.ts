import { Injectable, computed, inject, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

import { criarAuthConfig } from './auth.config';
import { EQUIPE_INTERNA, NOME_PAPEL, Papel, ehPapel } from './papel';

interface ClaimsToken {
  preferred_username?: string;
  name?: string;
  given_name?: string;
  email?: string;
  realm_access?: { roles?: unknown[] };
  [claim: string]: unknown;
}

/**
 * Estado de autenticação exposto como signals. O token de acesso é a fonte da verdade para papéis
 * (claim realm_access.roles); o ID token não traz esse claim no Keycloak.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);

  private readonly accessToken = signal<string | null>(null);
  /** Mensagem amigável quando o Keycloak não responde na inicialização. */
  readonly erroInicializacao = signal<string | null>(null);

  readonly logado = computed(() => this.accessToken() !== null);
  private readonly claims = computed<ClaimsToken>(() => decodificarJwt(this.accessToken()));

  readonly username = computed(() => this.claims().preferred_username ?? '');
  readonly nome = computed(() => this.claims().name ?? this.claims().given_name ?? this.username());
  readonly email = computed(() => this.claims().email ?? '');
  readonly papeis = computed<Papel[]>(() => (this.claims().realm_access?.roles ?? []).filter(ehPapel));

  /** Papel "principal" para exibição (o mais amplo que o usuário tiver). */
  readonly papelPrincipal = computed<Papel | null>(() => {
    const ordem: Papel[] = ['gerencial', 'vendedor', 'atendente', 'cliente'];
    return ordem.find((p) => this.papeis().includes(p)) ?? null;
  });
  readonly nomePapel = computed(() => {
    const p = this.papelPrincipal();
    return p ? NOME_PAPEL[p] : 'Sem perfil';
  });

  /** Chamado uma única vez, antes da primeira navegação (provideAppInitializer). */
  async inicializar(): Promise<void> {
    this.oauth.configure(criarAuthConfig());

    this.oauth.events.subscribe((evento) => {
      switch (evento.type) {
        case 'token_received':
        case 'token_refreshed':
          this.sincronizar();
          break;
        case 'token_refresh_error':
        case 'session_terminated':
        case 'session_error':
          // Refresh token expirou ou sessão encerrada no Keycloak: limpa e pede login de novo.
          this.oauth.logOut(true);
          this.sincronizar();
          this.login();
          break;
        case 'logout':
          this.sincronizar();
          break;
      }
    });

    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
      if (this.oauth.hasValidAccessToken()) {
        this.oauth.setupAutomaticSilentRefresh();
      } else if (this.oauth.getRefreshToken()) {
        // Voltou à aba com o access token vencido, mas o refresh ainda vale.
        await this.oauth.refreshToken().catch(() => this.oauth.logOut(true));
        if (this.oauth.hasValidAccessToken()) {
          this.oauth.setupAutomaticSilentRefresh();
        }
      }
    } catch (erro) {
      console.error('Falha ao inicializar a autenticação', erro);
      this.erroInicializacao.set('Não foi possível conectar ao servidor de login. Tente novamente em instantes.');
    }
    this.sincronizar();
  }

  login(): void {
    if (this.erroInicializacao()) {
      return;
    }
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }

  tokenDeAcesso(): string | null {
    return this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null;
  }

  temAlgumPapel(papeis: readonly Papel[]): boolean {
    return papeis.some((p) => this.papeis().includes(p));
  }

  /** Para onde mandar o usuário depois do login, conforme o papel. */
  rotaInicial(): string {
    if (this.temAlgumPapel(EQUIPE_INTERNA)) {
      return '/painel';
    }
    if (this.papeis().includes('cliente')) {
      return '/cliente';
    }
    return '/acesso-negado';
  }

  private sincronizar(): void {
    this.accessToken.set(this.oauth.hasValidAccessToken() ? this.oauth.getAccessToken() : null);
  }
}

/** Lê o payload do JWT (sem validar assinatura: isso é papel da API). */
export function decodificarJwt(token: string | null): ClaimsToken {
  if (!token) {
    return {};
  }
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = new TextDecoder().decode(Uint8Array.from(atob(base64), (c) => c.charCodeAt(0)));
    return JSON.parse(json) as ClaimsToken;
  } catch {
    return {};
  }
}
