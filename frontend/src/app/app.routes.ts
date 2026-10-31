import { Routes } from '@angular/router';

import { inicioPorPapelGuard, papelGuard } from './core/auth/papel.guard';
import { EQUIPE_INTERNA, GERENCIAL_OU_VENDEDOR, Papel } from './core/auth/papel';
import { AcessoNegado } from './paginas/acesso/acesso-negado/acesso-negado';
import { EmConstrucao } from './shared/em-construcao/em-construcao';

const SUFIXO = ' · CRM Super Auto';

function emConstrucao(path: string, titulo: string, papeis?: readonly Papel[]) {
  return {
    path,
    component: EmConstrucao,
    title: titulo + SUFIXO,
    data: { titulo, ...(papeis ? { papeis } : {}) },
    ...(papeis ? { canActivate: [papelGuard] } : {}),
  };
}

export const routes: Routes = [
  // Raiz: decide a área pelo papel (cliente → /cliente, demais → /painel).
  { path: '', pathMatch: 'full', canActivate: [inicioPorPapelGuard], children: [] },

  { path: 'acesso-negado', component: AcessoNegado, title: 'Acesso negado' + SUFIXO },

  // Área do cliente
  {
    path: 'cliente',
    loadComponent: () => import('./layout/cliente/layout-cliente').then((m) => m.LayoutCliente),
    canActivate: [papelGuard],
    data: { papeis: ['cliente'] satisfies Papel[] },
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./paginas/areacliente/cliente-inicio/cliente-inicio').then((m) => m.ClienteInicio),
        title: 'Início' + SUFIXO,
      },
      emConstrucao('veiculos', 'Veículos'),
      emConstrucao('indicar', 'Indicar'),
      emConstrucao('perfil', 'Perfil'),
    ],
  },

  // Área interna (gerencial, vendedor, atendente)
  {
    path: '',
    loadComponent: () => import('./layout/interno/layout-interno').then((m) => m.LayoutInterno),
    canActivate: [papelGuard],
    data: { papeis: [...EQUIPE_INTERNA] },
    children: [
      {
        path: 'painel',
        loadComponent: () => import('./paginas/relacionamento/painel/painel').then((m) => m.Painel),
        title: 'Painel' + SUFIXO,
      },
      emConstrucao('contatos-hoje', 'Contatos de hoje'),
      emConstrucao('atrasados', 'Atrasados'),
      emConstrucao('aniversariantes', 'Aniversariantes'),
      {
        path: 'carteira',
        children: [
          {
            path: '',
            loadComponent: () => import('./paginas/cadastro/carteira/carteira').then((m) => m.Carteira),
            title: 'Carteira' + SUFIXO,
          },
          {
            path: 'novo',
            loadComponent: () => import('./paginas/cadastro/novo-contrato/novo-contrato').then((m) => m.NovoContrato),
            title: 'Novo contrato' + SUFIXO,
            canActivate: [papelGuard],
            data: { papeis: [...GERENCIAL_OU_VENDEDOR] },
          },
          {
            path: 'associados/:id',
            loadComponent: () =>
              import('./paginas/cadastro/ficha-associado/ficha-associado').then((m) => m.FichaAssociado),
            title: 'Ficha do cliente' + SUFIXO,
          },
        ],
      },
      emConstrucao('atendimentos', 'Atendimentos'),
      emConstrucao('indicacoes', 'Indicações'),
      // Gestão: o guard repete a regra do menu (ACE-02); a API recusa com 403 de qualquer forma.
      emConstrucao('inteligencia', 'Inteligência', GERENCIAL_OU_VENDEDOR),
      emConstrucao('importacao', 'Importação', ['gerencial']),
      emConstrucao('auditoria', 'Auditoria', ['gerencial']),
      {
        path: 'configuracoes',
        canActivate: [papelGuard],
        data: { papeis: ['gerencial'] satisfies Papel[] },
        children: [
          { path: '', pathMatch: 'full', redirectTo: 'usuarios' },
          {
            path: 'usuarios',
            loadComponent: () =>
              import('./paginas/acesso/usuarios-equipe/usuarios-equipe').then((m) => m.UsuariosEquipe),
            title: 'Usuários da equipe' + SUFIXO,
          },
        ],
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
