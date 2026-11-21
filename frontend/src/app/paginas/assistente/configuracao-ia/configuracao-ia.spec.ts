import { describe, expect, it } from 'vitest';

import { ProvedorConfigurado } from '../../../core/api/assistente-api.service';
import { podeLigar, reservaValida } from './configuracao-ia';

const gemini = { provedor: 'GEMINI', comChave: false } as ProvedorConfigurado;
const ajuste = { versao: 0, modelo: 'gemini-modelo', chave: null, ativo: true, limiteMensal: 20, precoEntrada: null, precoSaida: null };

describe('Configuração do assistente', () => {
  it('IA-05 · ligar exige modelo e chave, como o servidor exige', () => {
    expect(podeLigar({ original: gemini, ajuste })).toBe(false);
    expect(podeLigar({ original: gemini, ajuste: { ...ajuste, chave: 'nova' } })).toBe(true);
    expect(podeLigar({ original: { ...gemini, comChave: true }, ajuste })).toBe(true);
    expect(podeLigar({ original: { ...gemini, comChave: true }, ajuste: { ...ajuste, modelo: ' ' } })).toBe(false);
  });

  it('IA-05 · o reserva precisa ser outro provedor', () => {
    expect(reservaValida({ principal: 'CLAUDE', reserva: 'CLAUDE' })).toBe(false);
    expect(reservaValida({ principal: 'CLAUDE', reserva: 'GEMINI' })).toBe(true);
    expect(reservaValida({ principal: 'CLAUDE', reserva: null })).toBe(true);
  });
});
