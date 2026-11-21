import { describe, expect, it } from 'vitest';

import { NOME_ASSUNTO } from '../../../core/api/assistente-api.service';
import { fatias } from './tendencias';

describe('Tendências', () => {
  it('IA-03 · dados 10 atendimentos, 4 com guincho, guincho aparece com 40% e em primeiro', () => {
    const resultado = fatias({ MENSALIDADE: 3, GUINCHO: 4 }, NOME_ASSUNTO, 10);
    expect(resultado[0]).toEqual({ rotulo: 'Guincho', quantidade: 4, percentual: 40 });
    expect(resultado[1].percentual).toBe(30);
  });

  it('IA-03 · sem atendimentos, nenhuma fatia divide por zero', () => {
    expect(fatias({ GUINCHO: 0 }, NOME_ASSUNTO, 0)[0].percentual).toBe(0);
  });
});
