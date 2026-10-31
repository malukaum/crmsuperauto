import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CarteiraApi } from './carteira-api.service';
import { problemaDe } from './problema';

describe('CarteiraApi (CAR-01, CAR-07)', () => {
  let api: CarteiraApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(CarteiraApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('manda só os filtros informados', () => {
    api.buscar({ busca: 'haa-7216', fila: 'ATRASADO', cidade: 2535, vendedor: undefined, pagina: 0, tamanho: 25 }).subscribe();

    const req = http.expectOne((r) => r.url === '/api/interno/carteira');
    expect(req.request.params.get('busca')).toBe('haa-7216');
    expect(req.request.params.get('fila')).toBe('ATRASADO');
    expect(req.request.params.get('cidade')).toBe('2535');
    expect(req.request.params.has('vendedor')).toBe(false);
    req.flush({ linhas: [], pagina: 0, tamanho: 25, totalContratos: 0, somaMensalidades: 0 });
  });

  it('exporta a lista inteira do filtro, sem a página', () => {
    api.exportar({ fila: 'HOJE', pagina: 3, tamanho: 10 }, 'XLSX').subscribe();

    const req = http.expectOne((r) => r.url === '/api/interno/carteira/exportacao');
    expect(req.request.params.get('fila')).toBe('HOJE');
    expect(req.request.params.get('formato')).toBe('XLSX');
    expect(req.request.params.has('pagina')).toBe(false);
    expect(req.request.params.has('tamanho')).toBe(false);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });

  it('erro de regra chega com detail e codigo (ProblemDetail)', () => {
    let codigo: string | undefined;
    api.buscar({ tamanho: 10 }).subscribe({ error: (e: unknown) => (codigo = problemaDe(e).codigo) });

    http
      .expectOne(() => true)
      .flush({ detail: 'Escolha páginas de 10, 25 ou 50 linhas.', codigo: 'pagina-invalida' }, { status: 422, statusText: 'x' });
    expect(codigo).toBe('pagina-invalida');
  });
});
