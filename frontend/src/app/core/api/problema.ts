import { HttpErrorResponse } from '@angular/common/http';

import { Problema } from './api.service';

/** ProblemDetail com os extras de regra de negócio: `codigo` decide o fluxo, `campos` traz a validação por campo. */
export interface ProblemaApi extends Problema {
  codigo?: string;
  campos?: Record<string, string>;
  [extra: string]: unknown;
}

export const SEM_CONEXAO = 'Não foi possível falar com o servidor. Verifique a conexão e tente novamente.';
const INESPERADO = 'Ocorreu um erro inesperado. Tente novamente.';

/** Traduz qualquer erro de chamada HTTP no ProblemDetail da API, com mensagem em português. */
export function problemaDe(erro: unknown): ProblemaApi {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return { status: 0, detail: SEM_CONEXAO };
    }
    if (erro.error && typeof erro.error === 'object' && !(erro.error instanceof Blob)) {
      return { status: erro.status, ...(erro.error as ProblemaApi) };
    }
    return { status: erro.status, detail: INESPERADO };
  }
  return { detail: INESPERADO };
}
