import { describe, expect, it } from 'vitest';
import { sessaoLembrada } from './inatividade.service';

describe('ACE-03 · sessão lembrada no dispositivo', () => {
  it('com "Confiar neste dispositivo" (renovação de 15 dias), a inatividade de 30 minutos não encerra a sessão', () => {
    const quinzeDias = 15 * 24 * 60 * 60;
    expect(sessaoLembrada({ iat: 1_700_000_000, exp: 1_700_000_000 + quinzeDias })).toBe(true);
  });

  it('sessão comum (renovação de 30 minutos) continua expirando por inatividade', () => {
    expect(sessaoLembrada({ iat: 1_700_000_000, exp: 1_700_000_000 + 30 * 60 })).toBe(false);
    expect(sessaoLembrada({})).toBe(false);
  });
});
