import { Clipboard } from '@angular/cdk/clipboard';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { Confirmacao } from '../../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../../common/erros';
import { Boleto } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { REGRA_ATRASO, situacao } from '../../services/boletos';
import { CodigoBarras } from '../codigo-barras/codigo-barras';

/** O boleto emitido, com linha digitável e código de barras: para copiar, mandar ou imprimir. */
@Component({
  selector: 'app-ver-boleto',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, CurrencyPipe, DatePipe, CodigoBarras],
  templateUrl: './ver-boleto.html',
  styleUrl: './ver-boleto.css',
})
export class VerBoleto {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly clipboard = inject(Clipboard);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  /** Da rota: /app/boletos/:id e ?novo=1 logo depois de emitir. */
  readonly id = input.required<string>();
  readonly novo = input<string>();

  protected readonly boleto = rxResource({ params: () => +this.id(), stream: ({ params }) => this.minhaConta.boleto(params) });
  protected readonly erro = computed(() => (this.boleto.error() ? mensagemDeErro(this.boleto.error()) : ''));
  protected readonly situacao = computed(() => {
    const b = this.boleto.value();
    return b ? situacao(b) : null;
  });
  protected readonly copiado = signal(false);
  protected readonly regraAtraso = REGRA_ATRASO;

  protected copiar(b: Boleto): void {
    if (this.clipboard.copy(b.linhaDigitavel)) {
      this.copiado.set(true);
      this.snack.open('Linha digitável copiada. Mande para quem vai pagar.', 'OK', { duration: 2500 });
      setTimeout(() => this.copiado.set(false), 2500);
    }
  }

  protected imprimir(): void {
    window.print();
  }

  protected comprovante(b: Boleto): void {
    if (b.autenticacao) {
      this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(b.autenticacao) });
    }
  }

  protected cancelar(b: Boleto): void {
    this.dialog
      .open(Confirmacao, {
        data: {
          titulo: 'Cancelar boleto?',
          mensagem: 'Ninguém mais vai conseguir pagar este boleto. Para cobrar de novo, emita outro.',
          confirmar: 'Cancelar boleto',
          icone: 'block',
          perigo: true,
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.minhaConta.cancelarBoleto(b.id)),
      )
      .subscribe({
        next: (atual) => {
          this.boleto.set(atual);
          this.snack.open('Boleto cancelado.', 'OK', { duration: 2500 });
        },
        error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 }),
      });
  }
}
