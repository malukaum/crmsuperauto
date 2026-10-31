import { convertToParamMap } from '@angular/router';

import { comFiltro, comOrdem, estadoDaUrl, urlDoEstado } from './filtro-da-url';

describe('CAR-01 · filtros da carteira na URL', () => {
  it('lê os filtros, a ordem e a página da URL', () => {
    const estado = estadoDaUrl(
      convertToParamMap({ fila: 'ATRASADO', cidade: '2535', cidadeNome: 'Formiga / MG', pagina: '2', tamanho: '50' }),
    );

    expect(estado.filtro.fila).toBe('ATRASADO');
    expect(estado.filtro.cidade).toBe(2535);
    expect(estado.filtro.pagina).toBe(2);
    expect(estado.filtro.tamanho).toBe(50);
    expect(estado.cidadeNome).toBe('Formiga / MG');
  });

  it('tamanho fora de 10, 25 ou 50 volta para 25', () => {
    expect(estadoDaUrl(convertToParamMap({ tamanho: '30' })).filtro.tamanho).toBe(25);
  });

  it('ida e volta mantém o estado (ao voltar da ficha a lista fica como estava)', () => {
    const original = estadoDaUrl(convertToParamMap({ busca: 'haa-7216', situacao: 'ATIVO', ordem: 'PLACA', decrescente: 'true' }));

    expect(estadoDaUrl(convertToParamMap(urlDoEstado(original)))).toEqual(original);
  });

  it('padrões não poluem a URL', () => {
    expect(urlDoEstado(estadoDaUrl(convertToParamMap({})))).toEqual({});
  });

  it('mudar um filtro volta para a primeira página', () => {
    const naPagina3 = estadoDaUrl(convertToParamMap({ pagina: '3' }));

    expect(comFiltro(naPagina3, { fila: 'HOJE' }).filtro.pagina).toBe(0);
  });

  it('clicar na mesma coluna inverte a ordem', () => {
    const porPlaca = comOrdem(estadoDaUrl(convertToParamMap({})), 'PLACA');

    expect(porPlaca.filtro.decrescente).toBeUndefined();
    expect(comOrdem(porPlaca, 'PLACA').filtro.decrescente).toBe(true);
  });
});
