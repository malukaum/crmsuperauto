import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RelacionamentoApi } from './relacionamento-api.service';
import { problemaDe } from './problema';

describe('RelacionamentoApi · contrato das filas', () => {
  let api: RelacionamentoApi;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(RelacionamentoApi);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('envia busca e filtros preenchidos sem tentar definir vendedor pelo navegador', () => {
    api
      .fila('atrasados', { busca: 'José', cidade: 2535, etapa: null, tipoVeiculo: null }, { pagina: 1, tamanho: 25 })
      .subscribe();
    const pedido = http.expectOne((pedido) => pedido.url === '/api/interno/filas/atrasados');
    expect(pedido.request.params.get('busca')).toBe('José');
    expect(pedido.request.params.get('cidade')).toBe('2535');
    expect(pedido.request.params.has('etapa')).toBe(false);
    expect(pedido.request.params.has('vendedor')).toBe(false);
    expect(pedido.request.params.get('pagina')).toBe('1');
    expect(pedido.request.params.get('tamanho')).toBe('25');
    pedido.flush({ content: [], page: { size: 25, number: 1, totalElements: 0, totalPages: 0 } });
  });
  it('mantém código e mensagem de conflito para a fila pedir uma nova leitura', () => {
    let codigo: string | undefined;
    api
      .registrar(10, { versao: 3, resultado: 'CONCLUIDO', retorno: null, descricao: '' })
      .subscribe({ error: (erro) => (codigo = problemaDe(erro).codigo) });
    const pedido = http.expectOne('/api/interno/filas/contratos/10/registro');
    expect(pedido.request.body.versao).toBe(3);
    pedido.flush(
      { codigo: 'edicao-simultanea', detail: 'Recarregue para ver a versão atual.' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(codigo).toBe('edicao-simultanea');
  });
  it('salva a régua com a confirmação obtida na prévia', () => {
    api.salvar({ 1: 7, 2: 25 }, 'confirmacao-da-previa').subscribe();
    const pedido = http.expectOne('/api/gestao/regua');
    expect(pedido.request.method).toBe('PUT');
    expect(pedido.request.body).toEqual({
      prazos: { 1: 7, 2: 25 },
      confirmacao: 'confirmacao-da-previa',
    });
    pedido.flush(null);
  });
  it('ATD-05 · histórico envia só os filtros preenchidos; sem datas o backend usa o mês corrente', () => {
    api
      .historico({
        inicio: null,
        fim: null,
        tipo: 'SUPORTE',
        resultado: null,
        usuario: null,
        cidade: 2535,
        busca: 'haa7216',
      }, { pagina: 0, tamanho: 25 })
      .subscribe();
    const pedido = http.expectOne((pedido) => pedido.url === '/api/interno/atendimentos');
    expect(pedido.request.params.get('tipo')).toBe('SUPORTE');
    expect(pedido.request.params.get('cidade')).toBe('2535');
    expect(pedido.request.params.get('busca')).toBe('haa7216');
    expect(pedido.request.params.has('inicio')).toBe(false);
    expect(pedido.request.params.get('pagina')).toBe('0');
    pedido.flush({ content: [], page: { size: 25, number: 0, totalElements: 0, totalPages: 0 } });
  });
  it('ATD-04 · a correção é um novo registro apontando para o original', () => {
    api.corrigir(55, { resultado: 'SEM_RESPOSTA', retorno: null, descricao: 'Não atendeu' }).subscribe();
    const pedido = http.expectOne('/api/interno/atendimentos/55/correcao');
    expect(pedido.request.method).toBe('POST');
    expect(pedido.request.body.resultado).toBe('SEM_RESPOSTA');
    pedido.flush(null);
  });
  it('ATD-01 · pela ficha usa a rota da ficha, que aceita contrato suspenso', () => {
    api
      .registrarPelaFicha(10, {
        versao: 2,
        resultado: 'CONCLUIDO',
        retorno: null,
        descricao: '',
        realizadoEm: '2026-10-30T15:00',
      })
      .subscribe();
    const pedido = http.expectOne('/api/interno/atendimentos/contratos/10/registro');
    expect(pedido.request.body.realizadoEm).toBe('2026-10-30T15:00');
    pedido.flush(null);
  });
});
