import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ContratoDaFicha, FichaAssociado } from '../../../core/api/associados-api.service';
import { RelacionamentoApi } from '../../../core/api/relacionamento-api.service';
import { RegistroDeAtendimento } from './registro-de-atendimento';

function contrato(id: number, situacao: ContratoDaFicha['situacao'], versaoRegua: number | null): ContratoDaFicha {
  return {
    id,
    versao: 0,
    placa: `ABC${id}`,
    modelo: 'Onix',
    tipoVeiculoId: 1,
    tipoVeiculo: 'Carro',
    mensalidade: 100,
    situacao,
    codigoSga: null,
    aderidoEm: '2026-03-13',
    vendidoEm: null,
    encerradoEm: null,
    vendedorId: null,
    vendedorNome: null,
    fila: 'HOJE',
    diasAtraso: 0,
    etapaPendente: 'D+7',
    proximoContato: '2026-03-20',
    regua: [],
    versaoRegua,
  };
}

const ficha = {
  id: 7,
  contratos: [contrato(1, 'SUSPENSO', 4), contrato(2, 'CANCELADO', 1)],
} as unknown as FichaAssociado;

async function preparar() {
  const api = {
    preverPelaFicha: vi.fn().mockReturnValue(of({ etapaConcluida: 'D+7', proximaEtapa: 'D+30', proximoContato: '2026-04-17' })),
    registrarPelaFicha: vi.fn().mockReturnValue(of(undefined)),
    registrarAvulso: vi.fn().mockReturnValue(of(undefined)),
  };
  await TestBed.configureTestingModule({
    imports: [RegistroDeAtendimento],
    providers: [{ provide: RelacionamentoApi, useValue: api }],
  }).compileComponents();
  const fixture = TestBed.createComponent(RegistroDeAtendimento);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return { api, componente: fixture.componentInstance as any };
}

describe('RegistroDeAtendimento · ficha', () => {
  it('ATD-01 · pós-venda oferece o suspenso e esconde o cancelado; com um só, já vem escolhido', async () => {
    const { componente } = await preparar();
    expect(componente.contratos().map((c: ContratoDaFicha) => c.id)).toEqual([1]);
    expect(componente.contratoId()).toBe(1);
  });

  it('ATD-01 · só confirma depois da prévia e envia a versão da régua', async () => {
    const { api, componente } = await preparar();
    expect(componente.podeConfirmar()).toBe(false);
    componente.prever();
    componente.registrar();
    expect(api.registrarPelaFicha).toHaveBeenCalledWith(1, expect.objectContaining({ versao: 4 }));
  });

  it('ATD-02 · avulso aceita contrato cancelado ou nenhum e não pede prévia', async () => {
    const { api, componente } = await preparar();
    componente.escolherTipo('REAPROXIMACAO');
    expect(componente.contratos().map((c: ContratoDaFicha) => c.id)).toEqual([1, 2]);
    expect(componente.contratoId()).toBeNull();
    componente.registrar();
    expect(api.registrarAvulso).toHaveBeenCalledWith(
      expect.objectContaining({ associadoId: 7, contratoId: null, tipo: 'REAPROXIMACAO' }),
    );
  });

  it('ATD-02 · Reagendado exige a data combinada', async () => {
    const { componente } = await preparar();
    componente.escolherTipo('SUPORTE');
    componente.resultado.set('REAGENDADO');
    expect(componente.podeConfirmar()).toBe(false);
    componente.retorno.set('2026-11-05');
    expect(componente.podeConfirmar()).toBe(true);
  });
});
