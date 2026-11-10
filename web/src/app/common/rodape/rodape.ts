import { Component } from '@angular/core';
import { AUTOR, VERSAO } from '../config';
import { Marca } from '../marca/marca';

/** Barra fixa no rodapé: a marca à esquerda e a marca d'água do autor à direita. */
@Component({
  selector: 'app-rodape',
  imports: [Marca],
  templateUrl: './rodape.html',
  styleUrl: './rodape.css',
  host: { role: 'contentinfo' },
})
export class Rodape {
  protected readonly autor = AUTOR;
  protected readonly versao = VERSAO;
}
