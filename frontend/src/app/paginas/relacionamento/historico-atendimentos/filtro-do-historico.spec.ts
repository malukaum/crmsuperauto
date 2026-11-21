import { describe, expect, it } from 'vitest';
import { FILTRO_INICIAL, marcaDeCorrecao } from './filtro-do-historico';

describe('Histórico de atendimentos · filtro e marcas', () => {
  it('ATD-05 · começa sem datas, para o backend usar o mês corrente', () => {
    expect(FILTRO_INICIAL.inicio).toBeNull();
    expect(FILTRO_INICIAL.fim).toBeNull();
  });
  it('ATD-04 · marca o original como corrigido e o novo registro como correção', () => {
    expect(marcaDeCorrecao({ corrigido: true, corrigeAtendimentoId: null })).toBe('corrigido');
    expect(marcaDeCorrecao({ corrigido: false, corrigeAtendimentoId: 9 })).toBe('correção');
    expect(marcaDeCorrecao({ corrigido: false, corrigeAtendimentoId: null })).toBeNull();
  });
});
