import { Injectable, inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Observable, catchError, filter, map, switchMap } from 'rxjs';
import { Confirmacao, DadosConfirmacao } from '../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../common/erros';
import { EditarClienteDialog } from '../componentes/editar-cliente/editar-cliente';
import { NovoClienteDialog } from '../componentes/novo-cliente/novo-cliente';
import { SenhaProvisoriaDialog } from '../componentes/senha-provisoria/senha-provisoria';
import { Cliente, NovoClienteResposta } from '../model/cliente.model';
import { ClienteService } from './cliente.service';

/**
 * Fluxos com diálogos usados na lista de clientes e na tela da conta.
 * Cada método emite uma vez quando algo mudou (para a tela recarregar) e não emite se a pessoa cancelar.
 */
@Injectable({ providedIn: 'root' })
export class AcoesCliente {
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly clientes = inject(ClienteService);

  /** Cadastro → mostra a senha provisória. */
  cadastrar(): Observable<Cliente> {
    return this.dialog
      .open<NovoClienteDialog, void, NovoClienteResposta>(NovoClienteDialog, { disableClose: true })
      .afterClosed()
      .pipe(
        filter((r): r is NovoClienteResposta => !!r),
        switchMap((r) =>
          this.dialog
            .open(SenhaProvisoriaDialog, {
              data: { cliente: r.cliente, senha: r.senhaProvisoria, novo: true },
              disableClose: true,
            })
            .afterClosed()
            .pipe(map(() => r.cliente)),
        ),
      );
  }

  editar(cliente: Cliente): Observable<Cliente> {
    return this.dialog
      .open<EditarClienteDialog, Cliente, Cliente>(EditarClienteDialog, { data: cliente })
      .afterClosed()
      .pipe(
        filter((c): c is Cliente => !!c),
        map((c) => {
          this.snack.open('Dados do cliente salvos.', 'OK', { duration: 3000 });
          return c;
        }),
      );
  }

  /** Confirma, gera a senha e mostra. */
  novaSenhaProvisoria(cliente: Cliente): Observable<string> {
    const dados: DadosConfirmacao = {
      titulo: 'Gerar nova senha provisória?',
      mensagem: `A senha atual de ${cliente.nome} deixa de funcionar na hora. No próximo acesso ele vai criar uma senha nova. Use quando o cliente esquecer a senha ou estiver com o acesso bloqueado.`,
      confirmar: 'Gerar senha',
      icone: 'key',
    };
    return this.dialog
      .open(Confirmacao, { data: dados })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() =>
          this.clientes.novaSenhaProvisoria(cliente.id).pipe(
            catchError((e) => {
              this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 });
              return EMPTY;
            }),
          ),
        ),
        switchMap((senha) =>
          this.dialog
            .open(SenhaProvisoriaDialog, { data: { cliente, senha, novo: false }, disableClose: true })
            .afterClosed()
            .pipe(map(() => senha)),
        ),
      );
  }
}
