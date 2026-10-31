import { AuthConfig } from 'angular-oauth2-oidc';

import { environment } from '../../../environments/environment';

/** Authorization Code + PKCE (S256) com o client público crm-web do Keycloak. */
export function criarAuthConfig(): AuthConfig {
  const origem = window.location.origin;
  return {
    issuer: environment.keycloak.issuer,
    clientId: environment.keycloak.clientId,
    responseType: 'code',
    redirectUri: `${origem}/`,
    postLogoutRedirectUri: `${origem}/`,
    scope: 'openid profile email',
    // localhost em http é aceito; qualquer host remoto exige https.
    requireHttps: 'remoteOnly',
    strictDiscoveryDocumentValidation: true,
    // Renovação pelo refresh_token (sem iframe), quando 75% do tempo de vida do token tiver passado.
    useSilentRefresh: false,
    timeoutFactor: 0.75,
    sessionChecksEnabled: false,
    clearHashAfterLogin: true,
    showDebugInformation: false,
  };
}
