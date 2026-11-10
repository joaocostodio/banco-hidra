import { Clipboard } from '@angular/cdk/clipboard';
import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { mensagemDeErro } from '../../../../common/erros';
import { saudacao } from '../../../painel/services/painel.service';
import { Transacao } from '../../model/minha-conta.model';
import { MinhaContaService, ultimosDias } from '../../services/minha-conta.service';
import { ItemTransacao } from '../item-transacao/item-transacao';

/** Início do app: saldo, atalhos e últimas transações. */
@Component({
  selector: 'app-inicio-cliente',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule, CurrencyPipe, ItemTransacao],
  templateUrl: './inicio-cliente.html',
  styleUrl: './inicio-cliente.css',
  // Ao voltar para a aba do navegador, atualiza (o colega pode ter mandado um Pix).
  host: { '(window:focus)': 'atualizar()' },
})
export class InicioCliente {
  protected readonly auth = inject(AuthService);
  protected readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);
  private readonly clipboard = inject(Clipboard);
  private readonly snack = inject(MatSnackBar);

  protected readonly conta = rxResource({ stream: () => this.minhaConta.conta() });
  protected readonly recentes = rxResource({ stream: () => this.minhaConta.extrato(ultimosDias(90)) });
  protected readonly chaves = rxResource({ stream: () => this.minhaConta.chaves() });

  protected readonly ultimas = computed(() => (this.recentes.value() ?? []).slice(0, 5));
  protected readonly saudacao = saudacao();

  protected readonly erro = computed(() => {
    const e = this.conta.error() ?? this.recentes.error();
    return e ? mensagemDeErro(e) : '';
  });

  protected atualizar(): void {
    this.conta.reload();
    this.recentes.reload();
    this.chaves.reload();
  }

  protected copiarConta(agencia: string, numero: string): void {
    const texto = `${this.auth.usuario()?.nome}\nBanco Hidra · Agência ${agencia} · Conta ${numero}`;
    if (this.clipboard.copy(texto)) {
      this.snack.open('Dados da conta copiados.', 'OK', { duration: 2500 });
    }
  }

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(t.autenticacao) });
  }
}
