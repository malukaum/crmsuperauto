import { ChangeDetectionStrategy, Component } from '@angular/core';

/** Logo completa (símbolo, SUPERAUTO, Linha Truck e Proteção Veicular) para fundos azuis. */
@Component({
  selector: 'app-logo-completa',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<img src="marca/logo.png" alt="Super Auto · Linha Truck · Proteção Veicular" width="720" height="219" />`,
  styles: `
    :host {
      display: block;
    }
    img {
      display: block;
      width: 100%;
      height: auto;
    }
  `,
})
export class LogoCompleta {}
