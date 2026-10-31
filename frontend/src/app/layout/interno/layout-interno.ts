import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { EQUIPE_INTERNA, Papel } from '../../core/auth/papel';
import { TemPapelDirective } from '../../core/auth/tem-papel.directive';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
}

interface GrupoMenu {
  titulo: string;
  itens: ItemMenu[];
  /** Se informado, o grupo inteiro só aparece para estes papéis. */
  papeis?: Papel[];
}

export const MENU_INTERNO: GrupoMenu[] = [
  {
    titulo: 'Atendimento',
    itens: [
      { rota: '/painel', rotulo: 'Painel', icone: 'dashboard' },
      { rota: '/contatos-hoje', rotulo: 'Contatos de hoje', icone: 'today' },
      { rota: '/atrasados', rotulo: 'Atrasados', icone: 'schedule' },
      { rota: '/aniversariantes', rotulo: 'Aniversariantes', icone: 'cake' },
    ],
  },
  {
    titulo: 'Relacionamento',
    itens: [
      { rota: '/carteira', rotulo: 'Carteira', icone: 'groups' },
      { rota: '/atendimentos', rotulo: 'Atendimentos', icone: 'forum' },
      { rota: '/indicacoes', rotulo: 'Indicações', icone: 'handshake' },
    ],
  },
  {
    titulo: 'Gestão',
    papeis: ['gerencial'],
    itens: [
      { rota: '/inteligencia', rotulo: 'Inteligência', icone: 'insights' },
      { rota: '/configuracoes', rotulo: 'Configurações', icone: 'settings' },
    ],
  },
];

@Component({
  selector: 'app-layout-interno',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatToolbarModule,
    MatTooltipModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    TemPapelDirective,
  ],
  templateUrl: './layout-interno.html',
  styleUrl: './layout-interno.scss',
})
export class LayoutInterno {
  protected readonly auth = inject(AuthService);
  protected readonly menu = MENU_INTERNO;
  protected readonly equipeInterna = EQUIPE_INTERNA;
  private readonly sidenav = viewChild.required(MatSidenav);

  /** Até 959px o menu vira gaveta (recolhível); acima disso fica fixo. */
  protected readonly celular = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 959.98px)')
      .pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  protected aoNavegar(): void {
    if (this.celular()) {
      this.sidenav().close();
    }
  }

  protected iniciais(): string {
    const partes = this.auth.nome().trim().split(/\s+/).filter(Boolean);
    const letras = partes.length > 1 ? partes[0][0] + partes[partes.length - 1][0] : (partes[0] ?? '?').slice(0, 2);
    return letras.toUpperCase();
  }
}
