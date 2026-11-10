import { Clipboard } from '@angular/cdk/clipboard';
import { CurrencyPipe, DatePipe, formatCurrency, formatDate } from '@angular/common';
import { Component, LOCALE_ID, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Observable } from 'rxjs';
import { mensagemDeErro } from '../erros';
import { IconeCategoriaPipe } from '../formatos';
import { Icone } from '../icone/icone';
import { Comprovante } from './comprovante.model';

/**
 * Comprovante de uma operação. Recebe o Observable que busca o comprovante,
 * porque o ADM e o cliente usam rotas diferentes da API.
 */
@Component({
  selector: 'app-comprovante',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressBarModule, CurrencyPipe, DatePipe, IconeCategoriaPipe, Icone],
  templateUrl: './comprovante.html',
  styleUrl: './comprovante.css',
})
export class ComprovanteDialog {
  private readonly carregar = inject<Observable<Comprovante>>(MAT_DIALOG_DATA);
  private readonly clipboard = inject(Clipboard);
  private readonly locale = inject(LOCALE_ID);

  protected readonly comprovante = rxResource({ stream: () => this.carregar });
  protected readonly copiado = signal(false);
  protected readonly mensagemDeErro = mensagemDeErro;

  /** Comprovante em texto, para colar numa conversa. */
  protected copiar(c: Comprovante): void {
    const parte = (rotulo: string, p: Comprovante['origem']) =>
      p ? `${rotulo}: ${p.nome} (Banco Hidra · Ag ${p.agencia} · CC ${p.conta})` : `${rotulo}: Banco Hidra (depósito)`;
    const linhas = [
      `Comprovante Banco Hidra · ${c.categoria}`,
      `Valor: ${formatCurrency(c.valor, this.locale, 'R$', 'BRL')}`,
      `Data: ${formatDate(c.dataHora, "dd/MM/yyyy 'às' HH:mm:ss", this.locale)}`,
      parte('De', c.origem),
      ...(c.destino ? [parte('Para', c.destino)] : []),
      ...(c.descricao && c.descricao !== c.categoria ? [`Mensagem: ${c.descricao}`] : []),
      `Autenticação: ${c.autenticacao}`,
    ];
    if (this.clipboard.copy(linhas.join('\n'))) {
      this.copiado.set(true);
      setTimeout(() => this.copiado.set(false), 2500);
    }
  }
}
