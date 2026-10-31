import {
  descreverFila,
  filaMaisUrgente,
  formatarData,
  formatarTelefone,
  linkWhatsapp,
  normalizarDocumento,
  normalizarPlaca,
} from './formatos';

describe('formatos (RG-02)', () => {
  it('exibe o celular gravado como (37) 9 9869-2950', () => {
    expect(formatarTelefone('5537998692950')).toBe('(37) 9 9869-2950');
  });

  it('exibe o fixo como (37) 3321-1234', () => {
    expect(formatarTelefone('553733211234')).toBe('(37) 3321-1234');
  });

  it('placa em maiúsculas e sem hífen', () => {
    expect(normalizarPlaca('haa-7216')).toBe('HAA7216');
  });

  it('data em dd/mm/aaaa sem mexer no fuso', () => {
    expect(formatarData('2026-03-20')).toBe('20/03/2026');
    expect(formatarData(null)).toBe('');
  });

  it('documento só com letras e dígitos', () => {
    expect(normalizarDocumento('12.abc.345/01de-35')).toBe('12ABC34501DE35');
  });
});

describe('filas', () => {
  it('mostra os dias de atraso', () => {
    expect(descreverFila('ATRASADO', 12)).toBe('Atrasado · 12 dias');
    expect(descreverFila('ATRASADO', 1)).toBe('Atrasado · 1 dia');
    expect(descreverFila('HOJE', 0)).toBe('Hoje');
  });

  it('o cabeçalho da ficha usa a fila mais urgente', () => {
    expect(filaMaisUrgente(['FUTURO', 'ATRASADO', 'CANCELADO'])).toBe('ATRASADO');
    expect(filaMaisUrgente([])).toBeNull();
  });
});

describe('WhatsApp (RG-10)', () => {
  it('abre wa.me com 55 + DDD + número', () => {
    expect(linkWhatsapp('5537998692950')).toBe('https://wa.me/5537998692950');
  });
});
