import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../../../core/auth/auth.service';
import { VerificacaoIntegracao } from '../../../shared/verificacao-integracao/verificacao-integracao';

@Component({
  selector: 'app-painel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VerificacaoIntegracao],
  template: `
    <div class="crm-pagina">
      <h1 class="crm-titulo-pagina">Painel</h1>
      <p class="crm-subtitulo">
        Olá, {{ auth.nome() }}. Os indicadores do pós-venda chegam nas próximas etapas; por enquanto,
        esta página confirma que login, token e permissões estão funcionando.
      </p>
      <app-verificacao-integracao />
    </div>
  `,
})
export class Painel {
  protected readonly auth = inject(AuthService);
}
