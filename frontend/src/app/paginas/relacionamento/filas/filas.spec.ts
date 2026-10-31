import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';
import {
  RelacionamentoApi,
  LinhaTrabalho,
  PreviaContato,
} from '../../../core/api/relacionamento-api.service';
import { ReferenciasApi } from '../../../core/api/referencias-api.service';
import { MenuService } from '../../../core/api/menu.service';
import { Filas } from './filas';

const associado: LinhaTrabalho = {
  contratoId: 10,
  associadoId: 7,
  nome: 'José',
  whatsapp: '5537998692950',
  aceitaWhatsapp: true,
  modelo: 'Onix',
  placa: 'ABC1234',
  tipoVeiculo: 'Carro',
  mensalidade: 100,
  cidade: 'Formiga',
  situacao: 'ATIVO',
  etapa: 'D+7',
  data: '2026-10-31',
  diasAtraso: 0,
  versao: 3,
  mensagemEnviada: false,
};

async function preparar(linha = associado) {
  const api = {
    fila: vi
      .fn()
      .mockReturnValueOnce(of([linha]))
      .mockReturnValue(of([])),
    preverContato: vi
      .fn()
      .mockReturnValue(
        of({ etapaConcluida: 'D+7', proximaEtapa: 'D+30', proximoContato: '2026-11-23' }),
      ),
    registrar: vi.fn().mockReturnValue(of(undefined)),
  };
  const menu = { carregar: vi.fn() };
  await TestBed.configureTestingModule({
    imports: [Filas],
    providers: [
      provideRouter([]),
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { data: { fila: 'hoje' }, queryParamMap: convertToParamMap({}) } },
      },
      { provide: RelacionamentoApi, useValue: api },
      { provide: MenuService, useValue: menu },
      { provide: ReferenciasApi, useValue: { tiposVeiculo$: of([]), etapas$: of([]) } },
    ],
  }).compileComponents();
  const fixture = TestBed.createComponent(Filas);
  fixture.detectChanges();
  await fixture.whenStable();
  const botao = (rotulo: string): HTMLButtonElement =>
    [...fixture.nativeElement.querySelectorAll('button')].find(
      (b: Element) => b.textContent?.trim() === rotulo,
    );
  return { fixture, api, menu, botao };
}

describe('FIL · trabalhar na fila', () => {
  it('mostra a prévia com data e etapa, grava a versão vista e atualiza a fila e o menu', async () => {
    const { fixture, api, menu, botao } = await preparar();
    botao('Registrar').click();
    fixture.detectChanges();
    expect(botao('Confirmar registro').disabled).toBe(true);
    botao('Visualizar próximo contato').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('23/11/2026');
    expect(fixture.nativeElement.textContent).toContain('D+30');
    botao('Confirmar registro').click();
    fixture.detectChanges();
    expect(api.registrar).toHaveBeenCalledWith(10, {
      versao: 3,
      resultado: 'CONCLUIDO',
      retorno: null,
      descricao: '',
    });
    expect(menu.carregar).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Nenhum associado nesta fila');
  });
  it('recusa conflito, fecha o formulário e pede recarregar sem repetir a gravação', async () => {
    const { fixture, api, botao } = await preparar();
    api.registrar.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: {
              codigo: 'edicao-simultanea',
              detail:
                'Este cadastro foi alterado por outra pessoa. Recarregue para ver a versão atual.',
            },
          }),
      ),
    );
    botao('Registrar').click();
    fixture.detectChanges();
    botao('Visualizar próximo contato').click();
    fixture.detectChanges();
    botao('Confirmar registro').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain(
      'Este cadastro foi alterado por outra pessoa.',
    );
    expect(botao('Confirmar registro')).toBeUndefined();
    expect(api.registrar).toHaveBeenCalledOnce();
  });
  it('abre wa.me em nova aba e deixa o opt-out sem link', async () => {
    const { fixture } = await preparar({ ...associado, aceitaWhatsapp: false });
    expect(fixture.nativeElement.querySelector('a[href^="https://wa.me"]')).toBeNull();
    expect(
      [...fixture.nativeElement.querySelectorAll('button')].find((b: Element) =>
        b.textContent?.includes('opt-out'),
      )?.disabled,
    ).toBe(true);
  });
  it('usa o número principal e não registra atendimento ao abrir a conversa', async () => {
    const { fixture, api } = await preparar();
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('a[href^="https://wa.me"]');
    expect(link.getAttribute('href')).toBe('https://wa.me/5537998692950');
    expect(link.target).toBe('_blank');
    expect(api.registrar).not.toHaveBeenCalled();
  });
  it('descarta uma prévia em andamento quando o retorno manual é alterado', async () => {
    const { fixture, api, botao } = await preparar();
    const previaAntiga = new Subject<PreviaContato>();
    api.preverContato.mockReturnValue(previaAntiga.asObservable());
    botao('Registrar').click();
    fixture.detectChanges();
    botao('Visualizar próximo contato').click();
    fixture.detectChanges();
    const data: HTMLInputElement = fixture.nativeElement.querySelector('input[type="date"]');
    data.value = '2026-11-05';
    data.dispatchEvent(new Event('input', { bubbles: true }));
    await fixture.whenStable();
    previaAntiga.next({
      etapaConcluida: 'D+7',
      proximaEtapa: 'D+30',
      proximoContato: '2026-11-23',
    });
    fixture.detectChanges();
    expect(botao('Confirmar registro').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).not.toContain('23/11/2026');
    expect(api.registrar).not.toHaveBeenCalled();
  });
});
