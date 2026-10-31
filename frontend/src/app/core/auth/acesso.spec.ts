import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Route, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';

import { routes } from '../../app.routes';
import { menuPara } from '../../layout/interno/layout-interno';
import { AuthService } from './auth.service';
import { InatividadeService, TEMPO_INATIVIDADE_MS } from './inatividade.service';
import { Papel } from './papel';
import { papelGuard } from './papel.guard';

/** ACE-01 a ACE-03: menu por papel, rotas protegidas, saída e inatividade. */

function tokenCom(claims: object): string {
  const b64 = (o: object) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(o))))
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  return `${b64({ alg: 'RS256' })}.${b64(claims)}.assinatura`;
}

class OAuthFalso {
  token: string | null = null;
  events = new Subject<{ type: string }>();
  initCodeFlow = vi.fn();
  logOut = vi.fn();
  configure = vi.fn();
  setupAutomaticSilentRefresh = vi.fn();
  loadDiscoveryDocumentAndTryLogin = vi.fn(async () => true);
  getRefreshToken = vi.fn(() => null);
  refreshToken = vi.fn();
  hasValidAccessToken = () => this.token !== null;
  getAccessToken = () => this.token ?? '';
}

async function logarComo(papeis: string[]) {
  const oauth = TestBed.inject(OAuthService) as unknown as OAuthFalso;
  oauth.token = tokenCom({ preferred_username: 'fulano', name: 'Fulano', realm_access: { roles: papeis } });
  const auth = TestBed.inject(AuthService);
  await auth.inicializar();
  return { auth, oauth };
}

/** Acha a rota pelo caminho (ex.: "configuracoes/usuarios") e devolve a cadeia de rotas até ela. */
function cadeia(caminho: string): Route[] {
  const partes = caminho.split('/');
  const procurar = (lista: Route[], i: number): Route[] | null => {
    for (const r of lista) {
      if (r.path === '' && r.children && !r.pathMatch) {
        const dentro = procurar(r.children, i);
        if (dentro) return [r, ...dentro];
      }
      if (r.path === partes[i]) {
        if (i === partes.length - 1) return [r];
        const dentro = r.children ? procurar(r.children, i + 1) : null;
        if (dentro) return [r, ...dentro];
      }
    }
    return null;
  };
  const achou = procurar(routes, 0);
  if (!achou) throw new Error(`rota ${caminho} não encontrada`);
  return achou;
}

/** Roda todos os papelGuard da cadeia, como o roteador faria. */
function navegar(caminho: string): boolean | string {
  const router = TestBed.inject(Router);
  for (const rota of cadeia(caminho)) {
    if (!rota.canActivate?.includes(papelGuard)) continue;
    const r = TestBed.runInInjectionContext(() =>
      papelGuard({ data: rota.data ?? {} } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    if (r !== true) return r instanceof UrlTree ? router.serializeUrl(r) : (r as boolean);
  }
  return true;
}

describe('acesso e perfis', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: OAuthService, useClass: OAuthFalso }],
    });
  });

  afterEach(() => TestBed.inject(InatividadeService).parar());

  describe('ACE-02 · menu conforme o papel', () => {
    const rotulos = (papeis: Papel[]) => menuPara(papeis).flatMap((g) => g.itens.map((i) => i.rotulo));
    const grupos = (papeis: Papel[]) => menuPara(papeis).map((g) => g.titulo);

    it('atendente não vê Inteligência, Importação, Configurações nem Auditoria', () => {
      const itens = rotulos(['atendente']);
      for (const proibido of ['Inteligência', 'Importação', 'Configurações', 'Auditoria']) {
        expect(itens).not.toContain(proibido);
      }
      expect(grupos(['atendente'])).toEqual(['Atendimento', 'Relacionamento']);
    });

    it('vendedor vê a Gestão só com Inteligência', () => {
      const gestao = menuPara(['vendedor']).find((g) => g.titulo === 'Gestão');
      expect(gestao?.itens.map((i) => i.rotulo)).toEqual(['Inteligência']);
    });

    it('gerencial vê os três grupos completos', () => {
      expect(rotulos(['gerencial'])).toEqual([
        'Painel',
        'Contatos de hoje',
        'Atrasados',
        'Aniversariantes',
        'Carteira',
        'Atendimentos',
        'Indicações',
        'Inteligência',
        'Importação',
        'Configurações',
        'Auditoria',
      ]);
    });

    it('Contatos de hoje e Atrasados mostram a quantidade', () => {
      const comContador = menuPara(['atendente'])
        .flatMap((g) => g.itens)
        .filter((i) => i.contador);
      expect(comContador.map((i) => [i.rotulo, i.contador])).toEqual([
        ['Contatos de hoje', 'contatosHoje'],
        ['Atrasados', 'atrasados'],
      ]);
    });

    it.each(['configuracoes/usuarios', 'importacao', 'auditoria', 'inteligencia'])(
      'atendente que digita /%s vê "Acesso negado"',
      async (caminho) => {
        await logarComo(['atendente']);
        expect(navegar(caminho)).toBe('/acesso-negado');
      },
    );

    it('vendedor abre Inteligência, mas não Configurações', async () => {
      await logarComo(['vendedor']);
      expect(navegar('inteligencia')).toBe(true);
      expect(navegar('configuracoes/usuarios')).toBe('/acesso-negado');
    });

    it('gerencial abre a gestão de usuários', async () => {
      await logarComo(['gerencial']);
      expect(navegar('configuracoes/usuarios')).toBe(true);
    });
  });

  describe('ACE-01 · entrar', () => {
    it('cliente não abre nenhuma rota interna', async () => {
      await logarComo(['cliente']);
      for (const caminho of ['painel', 'carteira', 'contatos-hoje', 'configuracoes/usuarios']) {
        expect(navegar(caminho)).toBe('/acesso-negado');
      }
      expect(navegar('cliente')).toBe(true);
    });

    it('usuário sem nenhum dos quatro papéis vê "Acesso negado"', async () => {
      const { auth } = await logarComo(['offline_access', 'default-roles-crm']);
      expect(auth.rotaInicial()).toBe('/acesso-negado');
      expect(navegar('painel')).toBe('/acesso-negado');
      expect(navegar('cliente')).toBe('/acesso-negado');
    });
  });

  describe('ACE-03 · sair', () => {
    it('sair encerra a sessão no Keycloak (end_session), não só no navegador', async () => {
      const { auth, oauth } = await logarComo(['vendedor']);
      auth.logout();
      // logOut() sem "noRedirectToLogoutUrl" redireciona para o end_session_endpoint do Keycloak.
      expect(oauth.logOut).toHaveBeenCalledWith();
    });

    it('sem login, qualquer rota interna manda de volta para o login', () => {
      const oauth = TestBed.inject(OAuthService) as unknown as OAuthFalso;
      expect(navegar('painel')).toBe(false);
      expect(oauth.initCodeFlow).toHaveBeenCalled();
    });
  });

  describe('ACE-03 · inatividade', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('sem uso por 30 minutos encerra a sessão', () => {
      const servico = TestBed.inject(InatividadeService);
      const expirou = vi.fn();
      servico.iniciar(expirou);

      vi.advanceTimersByTime(TEMPO_INATIVIDADE_MS - 60_000);
      expect(expirou).not.toHaveBeenCalled();
      vi.advanceTimersByTime(60_000);
      expect(expirou).toHaveBeenCalledTimes(1);

      // Depois de expirar não dispara de novo.
      vi.advanceTimersByTime(TEMPO_INATIVIDADE_MS);
      expect(expirou).toHaveBeenCalledTimes(1);
    });

    it('qualquer uso (tecla, clique) reinicia a contagem', () => {
      const servico = TestBed.inject(InatividadeService);
      const expirou = vi.fn();
      servico.iniciar(expirou);

      vi.advanceTimersByTime(20 * 60_000);
      document.dispatchEvent(new KeyboardEvent('keydown'));
      vi.advanceTimersByTime(20 * 60_000);
      expect(expirou).not.toHaveBeenCalled();
      vi.advanceTimersByTime(10 * 60_000);
      expect(expirou).toHaveBeenCalledTimes(1);
    });

    it('o login liga a contagem e ela chama o logout de verdade', async () => {
      const { oauth } = await logarComo(['atendente']);
      vi.advanceTimersByTime(TEMPO_INATIVIDADE_MS + 30_000);
      expect(oauth.logOut).toHaveBeenCalledWith();
    });
  });
});
