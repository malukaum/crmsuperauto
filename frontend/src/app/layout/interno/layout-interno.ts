import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, viewChild } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, interval, map, merge } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { EQUIPE_INTERNA, GERENCIAL_OU_VENDEDOR, Papel } from '../../core/auth/papel';
import { ContadoresMenu, MenuService } from '../../core/api/menu.service';

type Contador = keyof ContadoresMenu;

export interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
  /** Se informado, o item só aparece para estes papéis (senão, para toda a equipe). */
  papeis?: readonly Papel[];
  /** Quantidade mostrada ao lado do nome. */
  contador?: Contador;
}

export interface GrupoMenu {
  titulo: string;
  itens: ItemMenu[];
}

const SO_GERENCIAL: readonly Papel[] = ['gerencial'];

/**
 * Menu da equipe (ACE-02). Esconder um item não protege nada: as rotas têm guard e a API responde 403.
 * Um grupo sem nenhum item visível para o papel some inteiro.
 */
export const MENU_INTERNO: GrupoMenu[] = [
  {
    titulo: 'Atendimento',
    itens: [
      { rota: '/painel', rotulo: 'Painel', icone: 'dashboard' },
      { rota: '/contatos-hoje', rotulo: 'Contatos de hoje', icone: 'today', contador: 'contatosHoje' },
      { rota: '/atrasados', rotulo: 'Atrasados', icone: 'schedule', contador: 'atrasados' },
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
    itens: [
      { rota: '/inteligencia', rotulo: 'Inteligência', icone: 'insights', papeis: GERENCIAL_OU_VENDEDOR },
      { rota: '/importacao', rotulo: 'Importação', icone: 'upload_file', papeis: SO_GERENCIAL },
      { rota: '/configuracoes', rotulo: 'Configurações', icone: 'settings', papeis: SO_GERENCIAL },
      { rota: '/auditoria', rotulo: 'Auditoria', icone: 'policy', papeis: SO_GERENCIAL },
    ],
  },
];

/** O menu que um conjunto de papéis enxerga. */
export function menuPara(papeis: readonly Papel[]): GrupoMenu[] {
  const pode = (item: ItemMenu) => (item.papeis ?? EQUIPE_INTERNA).some((p) => papeis.includes(p));
  return MENU_INTERNO.map((g) => ({ ...g, itens: g.itens.filter(pode) })).filter((g) => g.itens.length > 0);
}

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
  ],
  templateUrl: './layout-interno.html',
  styleUrl: './layout-interno.scss',
})
export class LayoutInterno {
  protected readonly auth = inject(AuthService);
  private readonly menuService = inject(MenuService);
  protected readonly menu = computed(() => menuPara(this.auth.papeis()));
  protected readonly contadores = this.menuService.contadores;
  private readonly sidenav = viewChild.required(MatSidenav);

  /** Até 959px o menu vira gaveta (recolhível); acima disso fica fixo. */
  protected readonly celular = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 959.98px)')
      .pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  constructor() {
    // Atualiza as quantidades ao abrir o CRM, a cada navegação e a cada 2 minutos.
    this.menuService.carregar();
    merge(inject(Router).events.pipe(filter((e) => e instanceof NavigationEnd)), interval(120_000))
      .pipe(takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.menuService.carregar());
  }

  protected quantidade(item: ItemMenu): number | null {
    const c = this.contadores();
    return item.contador && c ? c[item.contador] : null;
  }

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
