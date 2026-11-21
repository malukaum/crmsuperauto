import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { environment } from '../../../environments/environment';
import { REQUISICAO_DE_FUNDO, authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

/** OAuthService falso com o token de acesso vencendo em `venceEmMs`. */
function oauthFalso(venceEmMs: number) {
  return {
    events: new Subject<{ type: string }>(),
    getRefreshToken: () => 'renovacao',
    hasValidAccessToken: () => venceEmMs > 0,
    getAccessToken: () => 'token',
    getAccessTokenExpiration: () => Date.now() + venceEmMs,
    refreshToken: vi.fn(async () => undefined),
  };
}

function preparar(venceEmMs: number) {
  const oauth = oauthFalso(venceEmMs);
  TestBed.configureTestingModule({
    providers: [
      { provide: OAuthService, useValue: oauth },
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  return { oauth, auth: TestBed.inject(AuthService) };
}

describe('ACE-03 · renovação do token só com uso', () => {
  it('renova quando falta menos de 1 minuto para o token vencer', async () => {
    const { oauth, auth } = preparar(30_000);
    await auth.renovarSeNecessario();
    expect(oauth.refreshToken).toHaveBeenCalledTimes(1);
  });

  it('não renova enquanto o token ainda vale por mais de 1 minuto', async () => {
    const { oauth, auth } = preparar(4 * 60_000);
    await auth.renovarSeNecessario();
    expect(oauth.refreshToken).not.toHaveBeenCalled();
  });

  it('chamada de fundo (contadores do menu) com o token vencido não sai e não renova', () => {
    const { oauth } = preparar(0);
    const http = TestBed.inject(HttpClient);
    const controle = TestBed.inject(HttpTestingController);

    http
      .get(`${environment.apiBase}/interno/menu/contadores`, {
        context: new HttpContext().set(REQUISICAO_DE_FUNDO, true),
      })
      .subscribe();

    controle.expectNone(`${environment.apiBase}/interno/menu/contadores`);
    expect(oauth.refreshToken).not.toHaveBeenCalled();
  });
});
