import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { mensagemDeErro } from '../../../../common/erros';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { ContaService } from '../../services/conta.service';

/** Comprovante de uma operação, pela autenticação (as duas pontas: quem pagou e quem recebeu). */
@Component({
  selector: 'app-comprovante',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressBarModule, CurrencyPipe, DatePipe, IconeCategoriaPipe],
  templateUrl: './comprovante.html',
  styleUrl: './comprovante.css',
})
export class ComprovanteDialog {
  private readonly autenticacao = inject<string>(MAT_DIALOG_DATA);
  private readonly contas = inject(ContaService);

  protected readonly comprovante = rxResource({ stream: () => this.contas.comprovante(this.autenticacao) });
  protected readonly mensagemDeErro = mensagemDeErro;
}
