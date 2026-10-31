/**
 * Configuração do front. Vale para o modo dev (ng serve, porta 4200) e para o Docker (nginx, porta 8081):
 * nos dois casos o navegador fala com o Keycloak em localhost:8180 e com a API pelo mesmo host do front (/api).
 * Se mudar a porta do Keycloak no .env, ajuste o issuer aqui também.
 */
export const environment = {
  keycloak: {
    issuer: 'http://localhost:8180/realms/crm',
    clientId: 'crm-web',
  },
  apiBase: '/api',
};
