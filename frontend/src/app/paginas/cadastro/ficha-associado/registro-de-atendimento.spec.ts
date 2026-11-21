import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { ContratoDaFicha, FichaAssociado } from '../../../core/api/associados-api.service';
import { WritableSignal } from '@angular/core';
import { RelacionamentoApi, ResultadoContato, TipoAvulso } from '../../../core/api/relacionamento-api.service';
import { AssistenteApi } from '../../../core/api/assistente-api.service';
import { RegistroDeAtendimento } from './registro-de-atendimento';

/** O que o teste usa do formulário: a mesma interação que o template faz. */
interface FormularioEmTeste {
  contratos(): ContratoDaFicha[];
  contratoId(): number | null;
  podeConfirmar(): boolean;
  resultado: WritableSignal<ResultadoContato>;
  retorno: WritableSignal<string>;
  anotacoes: WritableSignal<string>;
  escolherTipo(tipo: 'POS_VENDA' | TipoAvulso): void;
  escolherAnexos(lista: File[]): void;
  prever(): void;
  registrar(): void;
}

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
    registrarAvulso: vi.fn().mockReturnValue(of({ id: 55 })),
  };
  const assistente = { anotar: vi.fn().mockReturnValue(of({ atendimentoId: 55, situacao: 'AGUARDANDO' })) };
  await TestBed.configureTestingModule({
    imports: [RegistroDeAtendimento],
    providers: [
      { provide: RelacionamentoApi, useValue: api },
      { provide: AssistenteApi, useValue: assistente },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(RegistroDeAtendimento);
  fixture.componentRef.setInput('ficha', ficha);
  fixture.detectChanges();
  return { api, assistente, componente: fixture.componentInstance as unknown as FormularioEmTeste };
}

describe('RegistroDeAtendimento · ficha', () => {
  it('ATD-01 · pós-venda oferece o suspenso e esconde o cancelado; com um só, já vem escolhido', async () => {
    const { componente } = await preparar();
    expect(componente.contratos().map((c) => c.id)).toEqual([1]);
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
    expect(componente.contratos().map((c) => c.id)).toEqual([1, 2]);
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

  it('IA-01 · as anotações vão para o atendimento recém-gravado, depois do registro', async () => {
    const { assistente, componente } = await preparar();
    componente.escolherTipo('SUPORTE');
    componente.anotacoes.set('Cliente quer indicar o irmão');
    componente.registrar();
    expect(assistente.anotar).toHaveBeenCalledWith(55, 'Cliente quer indicar o irmão', []);
  });

  it('IA-01 · sem anotação nem anexo, nada vai para a IA', async () => {
    const { assistente, componente } = await preparar();
    componente.escolherTipo('SUPORTE');
    componente.registrar();
    expect(assistente.anotar).not.toHaveBeenCalled();
  });

  it('IA-01 · anexo de tipo não aceito impede o registro, como o servidor recusaria', async () => {
    const { componente } = await preparar();
    componente.escolherTipo('SUPORTE');
    componente.escolherAnexos([new File(['x'], 'planilha.xlsx', { type: 'application/vnd.ms-excel' })]);
    expect(componente.podeConfirmar()).toBe(false);
  });
});
