import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { Confirmacao } from '../../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../../common/erros';
import { valorOu } from '../../../../common/recursos';
import { MinhaConta } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { IconeContaPipe, LIMITE_CONTAS, NomeTipoContaPipe } from '../../services/financas';
import { ContaDialog, DadosContaDialog } from '../conta-dialog/conta-dialog';

/** As contas do cliente: saldo de cada uma e o total, abrir, editar e encerrar. */
@Component({
  selector: 'app-minhas-contas',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, CurrencyPipe,
    IconeContaPipe, NomeTipoContaPipe],
  templateUrl: './minhas-contas.html',
  styleUrl: './minhas-contas.css',
})
export class MinhasContas {
  protected readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  protected readonly contas = rxResource({ stream: () => this.minhaConta.contas() });

  protected readonly abertas = computed(() => valorOu(this.contas, []).filter((c) => c.status !== 'ENCERRADA'));
  protected readonly encerradas = computed(() => valorOu(this.contas, []).filter((c) => c.status === 'ENCERRADA'));
  protected readonly total = computed(() => this.abertas().reduce((s, c) => s + c.saldo, 0));
  protected readonly cheio = computed(() => this.abertas().length >= LIMITE_CONTAS);
  protected readonly erro = computed(() => (this.contas.error() ? mensagemDeErro(this.contas.error()) : ''));

  protected abrir(conta: DadosContaDialog = null): void {
    this.dialog
      .open<ContaDialog, DadosContaDialog, MinhaConta>(ContaDialog, { data: conta, width: '420px' })
      .afterClosed()
      .pipe(filter((c): c is MinhaConta => !!c))
      .subscribe((c) => {
        this.snack.open(conta ? 'Conta atualizada.' : `Conta ${c.nome} aberta.`, 'OK', { duration: 2500 });
        this.contas.reload();
      });
  }

  protected encerrar(c: MinhaConta): void {
    if (c.saldo !== 0) {
      this.snack.open('Para encerrar, a conta precisa estar com saldo zero. Transfira o saldo antes.', 'OK', {
        duration: 5000,
      });
      return;
    }
    this.dialog
      .open(Confirmacao, {
        data: {
          titulo: `Encerrar ${c.nome}?`,
          mensagem: 'A conta deixa de receber e enviar dinheiro. As transações dela continuam no extrato.',
          confirmar: 'Encerrar conta',
          icone: 'block',
          perigo: true,
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.minhaConta.encerrarConta(c.id)),
      )
      .subscribe({
        next: () => {
          this.snack.open('Conta encerrada.', 'OK', { duration: 2500 });
          this.contas.reload();
        },
        error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 }),
      });
  }
}
