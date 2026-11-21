import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';

/** Sem uso por 30 minutos, a sessão é encerrada (ACE-03). Mesmo valor do ssoSessionIdleTimeout do realm. */
export const TEMPO_INATIVIDADE_MS = 30 * 60 * 1000;

/**
 * ACE-03 · Quem marca "Confiar neste dispositivo" (Lembrar de mim do Keycloak) recebe uma sessão de 15 dias, e o
 * token de renovação passa a valer bem mais que os 30 minutos da sessão comum. Nesse caso a regra de inatividade
 * não se aplica: quem encerra a sessão é o Keycloak, no fim dos 15 dias.
 */
export function sessaoLembrada(claimsDoTokenDeRenovacao: { iat?: unknown; exp?: unknown }): boolean {
  const { iat, exp } = claimsDoTokenDeRenovacao;
  if (typeof iat !== 'number' || typeof exp !== 'number') return false;
  return (exp - iat) * 1000 > TEMPO_INATIVIDADE_MS;
}

const EVENTOS_DE_USO = ['pointerdown', 'keydown', 'wheel', 'touchstart', 'scroll', 'mousemove'] as const;
const INTERVALO_VERIFICACAO_MS = 30 * 1000;
/** O uso é avisado no máximo a cada 30 s (o mouse dispara dezenas de eventos por segundo). */
const INTERVALO_AVISO_DE_USO_MS = 30 * 1000;

export interface Acompanhamento {
  /** Chamado quando a pessoa usa a tela (no máximo a cada 30 s): é quando o token é renovado. */
  aoUsar: () => void;
  /** Chamado após 30 minutos sem uso; ausente na sessão lembrada, que não expira por inatividade. */
  aoExpirar?: () => void;
}

/**
 * Acompanha o uso da tela (mouse, teclado, toque, rolagem). O token só é renovado quando há uso: sem uso por 30
 * minutos, o Keycloak encerra a sessão e a API recusa o token vencido (ACE-03, regra garantida no servidor). Aqui
 * a tela também sai na hora, sem esperar a próxima chamada. Verifica ao voltar para a aba, porque o navegador
 * congela timers de abas em segundo plano.
 */
@Injectable({ providedIn: 'root' })
export class InatividadeService {
  private readonly documento = inject(DOCUMENT);

  private ultimoUso = Date.now();
  private ultimoAviso = 0;
  private timer: ReturnType<typeof setInterval> | null = null;
  private acompanhamento: Acompanhamento | null = null;

  private readonly registrarUso = () => {
    this.ultimoUso = Date.now();
    if (this.ultimoUso - this.ultimoAviso >= INTERVALO_AVISO_DE_USO_MS) {
      this.ultimoAviso = this.ultimoUso;
      this.acompanhamento?.aoUsar();
    }
  };
  private readonly verificar = () => {
    if (this.acompanhamento?.aoExpirar && Date.now() - this.ultimoUso >= TEMPO_INATIVIDADE_MS) {
      const aoExpirar = this.acompanhamento.aoExpirar;
      this.parar();
      aoExpirar();
    }
  };
  private readonly aoMudarVisibilidade = () => {
    if (this.documento.visibilityState === 'visible') {
      this.verificar();
    }
  };

  iniciar(acompanhamento: Acompanhamento): void {
    this.parar();
    this.acompanhamento = acompanhamento;
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
    this.acompanhamento = null;
  }
}
