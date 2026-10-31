import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Sem uso por 30 minutos, a sessão é encerrada (ACE-03). Mesmo valor do ssoSessionIdleTimeout do realm. */
export const TEMPO_INATIVIDADE_MS = 30 * 60 * 1000;

const EVENTOS_DE_USO = ['pointerdown', 'keydown', 'wheel', 'touchstart', 'scroll', 'mousemove'] as const;
const INTERVALO_VERIFICACAO_MS = 30 * 1000;

/**
 * Conta o tempo sem uso (mouse, teclado, toque, rolagem). A renovação automática do token mantém a sessão
 * do Keycloak viva sozinha; por isso a regra de inatividade precisa ser aplicada aqui, no navegador.
 * Também verifica ao voltar para a aba, porque o navegador congela timers de abas em segundo plano.
 */
@Injectable({ providedIn: 'root' })
export class InatividadeService {
  private readonly documento = inject(DOCUMENT);

  private ultimoUso = Date.now();
  private timer: ReturnType<typeof setInterval> | null = null;
  private aoExpirar: (() => void) | null = null;

  private readonly registrarUso = () => {
    this.ultimoUso = Date.now();
  };
  private readonly verificar = () => {
    if (Date.now() - this.ultimoUso >= TEMPO_INATIVIDADE_MS) {
      const callback = this.aoExpirar;
      this.parar();
      callback?.();
    }
  };
  private readonly aoMudarVisibilidade = () => {
    if (this.documento.visibilityState === 'visible') {
      this.verificar();
    }
  };

  iniciar(aoExpirar: () => void): void {
    this.parar();
    this.aoExpirar = aoExpirar;
    this.ultimoUso = Date.now();
    for (const evento of EVENTOS_DE_USO) {
      this.documento.addEventListener(evento, this.registrarUso, { passive: true, capture: true });
    }
    this.documento.addEventListener('visibilitychange', this.aoMudarVisibilidade);
    this.timer = setInterval(this.verificar, INTERVALO_VERIFICACAO_MS);
  }

  parar(): void {
    for (const evento of EVENTOS_DE_USO) {
      this.documento.removeEventListener(evento, this.registrarUso, { capture: true });
    }
    this.documento.removeEventListener('visibilitychange', this.aoMudarVisibilidade);
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.aoExpirar = null;
  }
}
