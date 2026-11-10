import { Component } from '@angular/core';
import { Marca } from '../../../../common/marca/marca';
import { Rodape } from '../../../../common/rodape/rodape';

/**
 * Moldura das telas de acesso: arte da Hidra à esquerda e o formulário à direita.
 * O título vai no slot [titulo]; o formulário, no conteúdo normal.
 */
@Component({
  selector: 'app-vitrine',
  imports: [Marca, Rodape],
  templateUrl: './vitrine.html',
  styleUrl: './vitrine.css',
})
export class Vitrine {}
