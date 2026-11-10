import { Clipboard } from '@angular/cdk/clipboard';
import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { mensagemDeErro } from '../../../../common/erros';
import { IniciaisPipe } from '../../../../common/formatos';
import { StatusConta } from '../../../../common/status-conta/status-conta';
import { TrocarSenha } from '../../../acesso/componentes/trocar-senha/trocar-senha';
import { MeusDados } from '../../model/minha-conta.model';
import { MinhaContaService } from '../../services/minha-conta.service';

/** Dados do cliente, da conta e as opções de segurança. */
@Component({
  selector: 'app-perfil',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatListModule, MatProgressBarModule, DatePipe, IniciaisPipe, StatusConta],
  templateUrl: './perfil.html',
  styleUrl: './perfil.css',
})
export class Perfil {
  protected readonly auth = inject(AuthService);
  private readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);
  private readonly clipboard = inject(Clipboard);
  private readonly snack = inject(MatSnackBar);

  protected readonly dados = rxResource({ stream: () => this.minhaConta.dados() });
  protected readonly mensagemDeErro = mensagemDeErro;

  /** Texto pronto para mandar a quem vai te transferir. */
  protected copiarDados(d: MeusDados): void {
    const texto = `${d.nome}\nBanco Hidra\nAgência ${d.agencia}\nConta corrente ${d.numero}`;
    if (this.clipboard.copy(texto)) {
      this.snack.open('Dados para transferência copiados.', 'OK', { duration: 2500 });
    }
  }

  protected trocarSenha(): void {
    this.dialog.open(TrocarSenha);
  }
}
