import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../../../common/auth/auth.service';
import { mensagemDeErro } from '../../../../common/erros';
import { Marca } from '../../../../common/marca/marca';
import { Rodape } from '../../../../common/rodape/rodape';
import { TrocarSenha } from '../../../acesso/componentes/trocar-senha/trocar-senha';
import { MinhaContaService } from '../../services/minha-conta.service';

/** Início do cliente. Por enquanto mostra o saldo; o app completo (extrato, Pix) é a etapa 4. */
@Component({
  selector: 'app-inicio-cliente',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, CurrencyPipe, Marca, Rodape],
  templateUrl: './inicio-cliente.html',
  styleUrl: './inicio-cliente.css',
})
export class InicioCliente {
  protected readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly minhaConta = inject(MinhaContaService);

  protected readonly conta = rxResource({ stream: () => this.minhaConta.conta() });
  protected readonly ocultar = signal(false);
  protected readonly mensagemDeErro = mensagemDeErro;

  protected trocarSenha(): void {
    this.dialog.open(TrocarSenha);
  }
}
