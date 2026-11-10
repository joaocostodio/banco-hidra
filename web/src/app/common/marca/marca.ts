import { Component, input } from '@angular/core';

/** Logo da Hidra + "Banco Hidra" (o "Hidra" em prata). */
@Component({
  selector: 'app-marca',
  templateUrl: './marca.html',
  styleUrl: './marca.css',
  host: { '[class.pequena]': 'tamanho() === "pequena"' },
})
export class Marca {
  readonly tamanho = input<'normal' | 'pequena'>('normal');
}
