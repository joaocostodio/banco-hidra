import { Clipboard } from '@angular/cdk/clipboard';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { EnderecoTurma } from '../../../../common/endereco-turma.service';
import { Cliente } from '../../model/cliente.model';

export interface DadosSenhaProvisoria {
  cliente: Cliente;
  senha: string;
  /** true: cliente acabou de ser cadastrado; false: senha gerada de novo. */
  novo: boolean;
}

/** Mostra a senha provisória (só desta vez) e ajuda a entregar ao cliente. */
@Component({
  selector: 'app-senha-provisoria',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './senha-provisoria.html',
  styleUrl: './senha-provisoria.css',
})
export class SenhaProvisoriaDialog {
  protected readonly dados = inject<DadosSenhaProvisoria>(MAT_DIALOG_DATA);
  private readonly clipboard = inject(Clipboard);
  private readonly turma = inject(EnderecoTurma);

  protected readonly copiado = signal<'senha' | 'mensagem' | null>(null);

  protected copiarSenha(): void {
    this.copiar('senha', this.dados.senha);
  }

  /** Mensagem pronta para mandar ao cliente. */
  protected copiarMensagem(): void {
    const { cliente, senha } = this.dados;
    const conta = cliente.conta ? `Agência ${cliente.conta.agencia}, conta ${cliente.conta.numero}.\n` : '';
    const texto =
      `Olá, ${cliente.nome.split(' ')[0]}! Sua conta no Banco Hidra está pronta.\n` +
      conta +
      `Acesse ${this.turma.melhorEndereco()} com o e-mail ${cliente.email} e a senha provisória ${senha}.\n` +
      `No primeiro acesso você vai criar a sua própria senha.`;
    this.copiar('mensagem', texto);
  }

  private copiar(oque: 'senha' | 'mensagem', texto: string): void {
    if (this.clipboard.copy(texto)) {
      this.copiado.set(oque);
      setTimeout(() => this.copiado.set(null), 2500);
    }
  }
}
