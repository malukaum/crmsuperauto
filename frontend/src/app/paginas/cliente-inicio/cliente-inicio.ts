import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { VerificacaoIntegracao } from '../../shared/verificacao-integracao/verificacao-integracao';

@Component({
  selector: 'app-cliente-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [VerificacaoIntegracao],
  template: `
    <div class="crm-pagina">
      <h1 class="crm-titulo-pagina">Olá, {{ auth.nome() }}</h1>
      <p class="crm-subtitulo">A área do associado está em construção.</p>
      <app-verificacao-integracao />
    </div>
  `,
})
export class ClienteInicio {
  protected readonly auth = inject(AuthService);
}
