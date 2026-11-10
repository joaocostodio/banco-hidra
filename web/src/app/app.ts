import { Component, inject } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { RouterOutlet } from '@angular/router';
import { SVG_PIX } from './common/icone/icone';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
})
export class App {
  constructor() {
    const icones = inject(MatIconRegistry);
    // Os ícones vêm da fonte Material Symbols (carregada no index.html)...
    icones.setDefaultFontSetClass('material-symbols-outlined');
    // ...menos o do Pix, que é um desenho próprio (SVG fixo, escrito aqui no código).
    icones.addSvgIconLiteral('pix', inject(DomSanitizer).bypassSecurityTrustHtml(SVG_PIX));
  }
}
