import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { Marca } from '../../shared/marca/marca';

/** Área do associado: mobile-first, header marinho e navegação inferior. */
@Component({
  selector: 'app-layout-cliente',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule, RouterLink, RouterLinkActive, RouterOutlet, Marca],
  templateUrl: './layout-cliente.html',
  styleUrl: './layout-cliente.scss',
})
export class LayoutCliente {
  protected readonly auth = inject(AuthService);

  protected readonly abas = [
    { rota: '/cliente/inicio', rotulo: 'Início', icone: 'home' },
    { rota: '/cliente/veiculos', rotulo: 'Veículos', icone: 'directions_car' },
    { rota: '/cliente/indicar', rotulo: 'Indicar', icone: 'person_add' },
    { rota: '/cliente/perfil', rotulo: 'Perfil', icone: 'account_circle' },
  ];
}
