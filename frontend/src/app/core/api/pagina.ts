/**
 * Página de uma tabela paginada no servidor: o `PagedModel` do Spring. Os parâmetros da requisição são `pagina`
 * (começa em 0) e `tamanho` (até 100).
 */
export interface PaginaApi<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface PaginaSolicitada {
  pagina: number;
  tamanho: number;
}
