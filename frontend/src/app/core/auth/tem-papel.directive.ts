import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { AuthService } from './auth.service';
import { Papel } from './papel';

/**
 * Mostra o elemento só se o usuário tiver algum dos papéis informados.
 *
 *   <a *temPapel="['gerencial']" routerLink="/configuracoes">Configurações</a>
 *
 * Atenção: isto só esconde a interface. Quem protege os dados é a API (@PreAuthorize).
 */
@Directive({ selector: '[temPapel]' })
export class TemPapelDirective {
  private readonly auth = inject(AuthService);
  private readonly template = inject(TemplateRef<unknown>);
  private readonly container = inject(ViewContainerRef);

  readonly temPapel = input.required<readonly Papel[]>();

  private visivel = false;

  constructor() {
    effect(() => {
      const mostrar = this.auth.temAlgumPapel(this.temPapel());
      if (mostrar && !this.visivel) {
        this.container.createEmbeddedView(this.template);
      } else if (!mostrar && this.visivel) {
        this.container.clear();
      }
      this.visivel = mostrar;
    });
  }
}
