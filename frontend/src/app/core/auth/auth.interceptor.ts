import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { EMPTY, catchError, from, switchMap, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Marca chamadas automáticas, que não são uso da pessoa (ex.: contadores do menu a cada 2 minutos). Elas nunca
 * renovam o token, para não manter viva uma sessão parada (ACE-03); com o token vencido, simplesmente não saem.
 */
export const REQUISICAO_DE_FUNDO = new HttpContextToken<boolean>(() => false);

/** Só chamadas da nossa API levam o token; nada vai para outros domínios (ex.: Google Fonts). */
export function ehChamadaDaApi(url: string): boolean {
  const base = environment.apiBase;
  return url === base || url.startsWith(`${base}/`);
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!ehChamadaDaApi(req.url)) {
    return next(req);
  }
  const auth = inject(AuthService);
  const comToken = (token: string | null) =>
    token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  if (req.context.get(REQUISICAO_DE_FUNDO)) {
    const token = auth.tokenDeAcesso();
    return token ? next(comToken(token)) : EMPTY;
  }
  return from(auth.renovarSeNecessario()).pipe(
    switchMap(() => next(comToken(auth.tokenDeAcesso()))),
    catchError((erro: unknown) => {
      // 401 = token ausente/expirado: volta para o login. 403 é tratado por quem chamou.
      if (erro instanceof HttpErrorResponse && erro.status === 401) {
        auth.login();
      }
      return throwError(() => erro);
    }),
  );
};
