import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatRippleModule } from '@angular/material/core';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { Icone } from '../../../../common/icone/icone';
import { DetalheTransacaoPipe, SinalPipe, TituloTransacaoPipe } from '../../../contas/componentes/transacao.pipes';
import { Transacao } from '../../model/minha-conta.model';

/** Uma linha do extrato do cliente. Clicar abre o comprovante. */
@Component({
  selector: 'app-item-transacao',
  imports: [MatIconModule, MatRippleModule, CurrencyPipe, DatePipe, IconeCategoriaPipe,
    Icone, TituloTransacaoPipe, DetalheTransacaoPipe, SinalPipe],
  templateUrl: './item-transacao.html',
  styleUrl: './item-transacao.css',
})
export class ItemTransacao {
  readonly transacao = input.required<Transacao>();
  /** Esconde o valor (saldo oculto). */
  readonly oculto = input(false);
  /** Mostra a data (início) ou só a hora (extrato, que já agrupa por dia). */
  readonly formatoData = input<'data' | 'hora'>('data');
  readonly abrir = output<Transacao>();
}
