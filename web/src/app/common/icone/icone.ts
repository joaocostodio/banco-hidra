import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Desenho do ícone do Pix (registrado no App). A fonte Material Symbols não tem esse ícone. */
export const SVG_PIX = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2.4 8.9 5.5l2.4 2.4a1 1 0 0 0 1.4 0l2.4-2.4zM5.5 8.9 2.4 12l3.1 3.1 2.4-2.4a1 1 0 0 0 0-1.4zm13 0-2.4 2.4a1 1 0 0 0 0 1.4l2.4 2.4 3.1-3.1zM12 15.8a1 1 0 0 0-.7.3l-2.4 2.4 3.1 3.1 3.1-3.1-2.4-2.4a1 1 0 0 0-.7-.3z"/></svg>`;

/** Ícone pelo nome: usa o desenho próprio para "pix" e a fonte Material Symbols para o resto. */
@Component({
  selector: 'app-icone',
  imports: [MatIconModule],
  templateUrl: './icone.html',
  host: { style: 'display: contents' },
})
export class Icone {
  readonly nome = input.required<string>();
  /** Versão preenchida (item ativo do menu). */
  readonly cheio = input(false);
}
