import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';
import { Papel } from './papel';

/**
 * Exige login e pelo menos um dos papéis em {@code data.papeis} da rota.
 * Sem login → redireciona para o Keycloak. Logado sem o papel → /acesso-negado.
 */
export const papelGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.logado()) {
    auth.login();
    return false;
  }
  const exigidos = (route.data['papeis'] as Papel[] | undefined) ?? [];
  if (exigidos.length === 0 || auth.temAlgumPapel(exigidos)) {
    return true;
  }
  return router.createUrlTree(['/acesso-negado']);
};

/** Rota raiz: manda cada papel para a sua área (cliente → /cliente, demais → /painel). */
export const inicioPorPapelGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.logado()) {
    auth.login();
    return false;
  }
  return router.parseUrl(auth.rotaInicial());
};
