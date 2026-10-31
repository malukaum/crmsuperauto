import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

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
  const token = auth.tokenDeAcesso();
  const comToken = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(comToken).pipe(
    catchError((erro: unknown) => {
      // 401 = token ausente/expirado: volta para o login. 403 é tratado por quem chamou.
      if (erro instanceof HttpErrorResponse && erro.status === 401) {
        auth.login();
      }
      return throwError(() => erro);
    }),
  );
};
