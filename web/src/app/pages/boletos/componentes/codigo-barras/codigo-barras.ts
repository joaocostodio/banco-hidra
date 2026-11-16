import { Component, computed, input } from '@angular/core';

/** Barras finas (N) e largas (W) de cada dígito no padrão "2 de 5 intercalado" (ITF). */
const PADROES = ['NNWWN', 'WNNNW', 'NWNNW', 'WWNNN', 'NNWNW', 'WNWNN', 'NWWNN', 'NNNWW', 'WNNWN', 'NWNWN'];
const FINA = 1;
const LARGA = 3;
const ALTURA = 50;

/**
 * Desenha o código de barras de 44 dígitos do boleto, como nos boletos de verdade (ITF, Febraban).
 * Os dígitos vão em pares: o primeiro vira as 5 barras pretas e o segundo os 5 espaços entre elas.
 */
@Component({
  selector: 'app-codigo-barras',
  template: `
    <svg [attr.viewBox]="'0 0 ' + desenho().largura + ' ' + altura" preserveAspectRatio="none" role="img"
         [attr.aria-label]="'Código de barras ' + codigo()">
      <rect [attr.width]="desenho().largura" [attr.height]="altura" fill="#fff" />
      @for (b of desenho().barras; track $index) {
        <rect [attr.x]="b.x" [attr.width]="b.w" [attr.height]="altura" fill="#000" />
      }
    </svg>
  `,
  styles: `
    :host { display: block; }
    svg { display: block; width: 100%; height: 56px; }
  `,
})
export class CodigoBarras {
  readonly codigo = input.required<string>();
  protected readonly altura = ALTURA;

  protected readonly desenho = computed(() => {
    const d = this.codigo().replace(/\D/g, '');
    // Início: barra, espaço, barra, espaço (todos finos). Margem branca de 10 dos dois lados.
    const larguras: number[] = [FINA, FINA, FINA, FINA];
    for (let i = 0; i + 1 < d.length; i += 2) {
      const barras = PADROES[+d[i]];
      const espacos = PADROES[+d[i + 1]];
      for (let j = 0; j < 5; j++) {
        larguras.push(barras[j] === 'W' ? LARGA : FINA, espacos[j] === 'W' ? LARGA : FINA);
      }
    }
    larguras.push(LARGA, FINA, FINA); // fim: barra larga, espaço, barra fina

    const barras: { x: number; w: number }[] = [];
    let x = 10;
    larguras.forEach((w, i) => {
      if (i % 2 === 0) {
        barras.push({ x, w });
      }
      x += w;
    });
    return { barras, largura: x + 10 };
  });
}
