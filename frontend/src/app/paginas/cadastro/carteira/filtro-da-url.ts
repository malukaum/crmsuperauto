import { ParamMap, Params } from '@angular/router';

import {
  Fila,
  FiltroCarteira,
  OrdemCarteira,
  Perfil,
  PotencialIndicacao,
  SituacaoContrato,
  TAMANHOS_PAGINA,
  TamanhoPagina,
} from '../../../core/api/carteira-api.service';

export const TAMANHO_PADRAO: TamanhoPagina = 25;

/** O filtro da carteira mais o nome da cidade escolhida, só para mostrar no campo. */
export interface EstadoCarteira {
  filtro: FiltroCarteira;
  cidadeNome?: string;
}

/**
 * CAR-01 · Filtros, ordenação e página ficam na URL: ao voltar da ficha, a lista volta como estava (proposta),
 * e o endereço pode ser compartilhado.
 */
export function estadoDaUrl(params: ParamMap): EstadoCarteira {
  const texto = (nome: string) => params.get(nome) ?? undefined;
  const numero = (nome: string) => {
    const valor = Number(params.get(nome));
    return params.has(nome) && Number.isInteger(valor) ? valor : undefined;
  };
  const tamanho = numero('tamanho');
  return {
    filtro: {
      busca: texto('busca'),
      fila: texto('fila') as Fila | undefined,
      cidade: numero('cidade'),
      situacao: texto('situacao') as SituacaoContrato | undefined,
      excluirCancelados: params.get('excluirCancelados') === 'true' || undefined,
      etapa: numero('etapa'),
      tipoVeiculo: numero('tipoVeiculo'),
      perfil: texto('perfil') as Perfil | undefined,
      potencial: texto('potencial') as PotencialIndicacao | undefined,
      vendedor: texto('vendedor'),
      ordem: texto('ordem') as OrdemCarteira | undefined,
      decrescente: params.get('decrescente') === 'true' || undefined,
      pagina: numero('pagina') ?? 0,
      tamanho: (TAMANHOS_PAGINA as readonly number[]).includes(tamanho ?? 0)
        ? (tamanho as TamanhoPagina)
        : TAMANHO_PADRAO,
    },
    cidadeNome: texto('cidadeNome'),
  };
}

/** O contrário de {@link estadoDaUrl}: só os valores informados viram parâmetros. */
export function urlDoEstado(estado: EstadoCarteira): Params {
  const params: Params = {};
  const todos: Record<string, unknown> = { ...estado.filtro, cidadeNome: estado.cidadeNome };
  for (const [chave, valor] of Object.entries(todos)) {
    if (valor !== undefined && valor !== null && valor !== '' && valor !== false) {
      params[chave] = String(valor);
    }
  }
  if (params['pagina'] === '0') delete params['pagina'];
  if (params['tamanho'] === String(TAMANHO_PADRAO)) delete params['tamanho'];
  return params;
}

/** Mudar qualquer filtro volta para a primeira página. */
export function comFiltro(
  estado: EstadoCarteira,
  mudanca: Partial<FiltroCarteira>,
  cidadeNome?: string,
): EstadoCarteira {
  return {
    filtro: { ...estado.filtro, ...mudanca, pagina: 0 },
    cidadeNome: 'cidade' in mudanca ? cidadeNome : estado.cidadeNome,
  };
}

/** Clicar na coluna ordena por ela; clicar de novo inverte. */
export function comOrdem(estado: EstadoCarteira, coluna: OrdemCarteira): EstadoCarteira {
  const mesma = (estado.filtro.ordem ?? 'NOME') === coluna;
  return {
    ...estado,
    filtro: {
      ...estado.filtro,
      ordem: coluna,
      decrescente: mesma ? !estado.filtro.decrescente : undefined,
      pagina: 0,
    },
  };
}
