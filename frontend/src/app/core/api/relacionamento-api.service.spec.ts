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
      .fila('atrasados', { busca: 'José', cidade: 2535, etapa: null, tipoVeiculo: null })
      .subscribe();
    const pedido = http.expectOne((pedido) => pedido.url === '/api/interno/filas/atrasados');
    expect(pedido.request.params.get('busca')).toBe('José');
    expect(pedido.request.params.get('cidade')).toBe('2535');
    expect(pedido.request.params.has('etapa')).toBe(false);
    expect(pedido.request.params.has('vendedor')).toBe(false);
    pedido.flush([]);
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
});
