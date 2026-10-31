import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';

import { ehChamadaDaApi } from './auth.interceptor';
import { AuthService, decodificarJwt } from './auth.service';
import { Papel } from './papel';
import { inicioPorPapelGuard, papelGuard } from './papel.guard';
import { TemPapelDirective } from './tem-papel.directive';

function tokenCom(claims: object): string {
  const b64 = (o: object) =>
    btoa(String.fromCharCode(...new TextEncoder().encode(JSON.stringify(o)))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64({ alg: 'RS256' })}.${b64(claims)}.assinatura`;
}

/** OAuthService falso: só o que o AuthService usa. */
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

async function logarComo(papeis: string[], nome = 'Fulano de Tal'): Promise<{ auth: AuthService; oauth: OAuthFalso }> {
  const oauth = TestBed.inject(OAuthService) as unknown as OAuthFalso;
  oauth.token = tokenCom({ preferred_username: 'fulano', name: nome, realm_access: { roles: papeis } });
  const auth = TestBed.inject(AuthService);
  await auth.inicializar();
  return { auth, oauth };
}

describe('autenticação e papéis', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: OAuthService, useClass: OAuthFalso }],
    });
  });

  it('decodifica o payload do JWT, inclusive acentos', () => {
    const claims = decodificarJwt(tokenCom({ name: 'José Ação', realm_access: { roles: ['vendedor'] } }));
    expect(claims.name).toBe('José Ação');
    expect(decodificarJwt('lixo')).toEqual({});
    expect(decodificarJwt(null)).toEqual({});
  });

  it('só considera os papéis do CRM', async () => {
    const { auth } = await logarComo(['offline_access', 'vendedor', 'default-roles-crm']);
    expect(auth.papeis()).toEqual(['vendedor']);
    expect(auth.nomePapel()).toBe('Vendedor');
  });

  it.each<[string[], string]>([
    [['gerencial'], '/painel'],
    [['vendedor'], '/painel'],
    [['atendente'], '/painel'],
    [['cliente'], '/cliente'],
    [['offline_access'], '/acesso-negado'],
  ])('papéis %j vão para %s após o login', async (papeis, rota) => {
    const { auth } = await logarComo(papeis);
    expect(auth.rotaInicial()).toBe(rota);
  });

  it('o token só vai para chamadas da API', () => {
    expect(ehChamadaDaApi('/api/me')).toBe(true);
    expect(ehChamadaDaApi('/api')).toBe(true);
    expect(ehChamadaDaApi('/apix/coisa')).toBe(false);
    expect(ehChamadaDaApi('https://fonts.googleapis.com/css2')).toBe(false);
    expect(ehChamadaDaApi('http://localhost:8180/realms/crm/protocol/openid-connect/token')).toBe(false);
  });

  describe('papelGuard', () => {
    const rodar = (papeis: Papel[]) =>
      TestBed.runInInjectionContext(() =>
        papelGuard({ data: { papeis } } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      );

    it('sem login dispara o fluxo do Keycloak', () => {
      const oauth = TestBed.inject(OAuthService) as unknown as OAuthFalso;
      expect(rodar(['gerencial'])).toBe(false);
      expect(oauth.initCodeFlow).toHaveBeenCalled();
    });

    it('libera quem tem o papel e manda os demais para /acesso-negado', async () => {
      await logarComo(['vendedor']);
      const router = TestBed.inject(Router);
      expect(rodar(['gerencial', 'vendedor', 'atendente'])).toBe(true);
      const negado = rodar(['gerencial']) as UrlTree;
      expect(router.serializeUrl(negado)).toBe('/acesso-negado');
    });

    it('a raiz redireciona o cliente para /cliente', async () => {
      await logarComo(['cliente']);
      const destino = TestBed.runInInjectionContext(() =>
        inicioPorPapelGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      ) as UrlTree;
      expect(TestBed.inject(Router).serializeUrl(destino)).toBe('/cliente');
    });
  });

  describe('*temPapel', () => {
    @Component({
      imports: [TemPapelDirective],
      template: `
        <span id="gestao" *temPapel="['gerencial']">gestão</span>
        <span id="interno" *temPapel="papeis()">interno</span>
      `,
    })
    class Hospedeiro {
      papeis = signal<Papel[]>(['vendedor', 'atendente']);
    }

    it('mostra e esconde conforme o papel do usuário', async () => {
      await logarComo(['vendedor']);
      const fixture = TestBed.createComponent(Hospedeiro);
      await fixture.whenStable();
      const el: HTMLElement = fixture.nativeElement;
      expect(el.querySelector('#gestao')).toBeNull();
      expect(el.querySelector('#interno')).not.toBeNull();

      fixture.componentInstance.papeis.set(['cliente']);
      await fixture.whenStable();
      expect(el.querySelector('#interno')).toBeNull();
    });
  });
});
