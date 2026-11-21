import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';

import { AssociadosApi } from '../../../core/api/associados-api.service';
import { ContratosApi } from '../../../core/api/contratos-api.service';
import { ReferenciasApi } from '../../../core/api/referencias-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { NovoContrato } from './novo-contrato';

describe('NovoContrato', () => {
  it('CAR-02 · "Continuar" consulta o documento sem recarregar a página', async () => {
    const associados = { porDocumento: vi.fn().mockReturnValue(of({ id: 7, nome: 'José', cidade: null, uf: null })) };
    await TestBed.configureTestingModule({
      imports: [NovoContrato],
      providers: [
        provideRouter([]),
        { provide: AssociadosApi, useValue: associados },
        { provide: ContratosApi, useValue: {} },
        { provide: ReferenciasApi, useValue: { tiposVeiculo$: of([]), vendedores: () => of([]) } },
        { provide: AuthService, useValue: { temAlgumPapel: () => false } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(NovoContrato);
    fixture.detectChanges();
    const elemento: HTMLElement = fixture.nativeElement;
    const campo = elemento.querySelector('input') as HTMLInputElement;
    campo.value = '529.982.247-25';
    campo.dispatchEvent(new Event('input'));

    const envio = new Event('submit', { cancelable: true });
    elemento.querySelector('form')!.dispatchEvent(envio);

    expect(envio.defaultPrevented).toBe(true);
    expect(associados.porDocumento).toHaveBeenCalledWith('52998224725');
  });
});
