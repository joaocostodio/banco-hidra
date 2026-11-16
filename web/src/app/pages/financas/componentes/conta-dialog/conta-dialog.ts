import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable } from 'rxjs';
import { mensagemDeErro } from '../../../../common/erros';
import { MinhaConta, TipoConta } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { ICONE_TIPO_CONTA, NOME_TIPO_CONTA, TIPOS_CONTA } from '../../services/financas';

/** Sem conta: abrir uma nova. Com conta: renomear / mudar o tipo. */
export type DadosContaDialog = MinhaConta | null;

/** Abrir ou editar uma conta. Fecha com a conta salva. */
@Component({
  selector: 'app-conta-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  templateUrl: './conta-dialog.html',
})
export class ContaDialog {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly ref = inject<MatDialogRef<ContaDialog, MinhaConta>>(MatDialogRef);
  protected readonly conta = inject<DadosContaDialog>(MAT_DIALOG_DATA);

  protected readonly tipos = TIPOS_CONTA;
  protected readonly nomes = NOME_TIPO_CONTA;
  protected readonly icones = ICONE_TIPO_CONTA;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: [this.conta?.nome ?? NOME_TIPO_CONTA.POUPANCA, [Validators.required, Validators.maxLength(40)]],
    tipo: [this.conta?.tipo ?? ('POUPANCA' as TipoConta), Validators.required],
  });

  protected readonly salvando = signal(false);
  protected readonly erro = signal('');

  /** Sugere o nome pelo tipo, enquanto a pessoa não digitou um. */
  protected tipoMudou(tipo: TipoConta): void {
    const nome = this.form.controls.nome;
    if (!nome.value.trim() || Object.values(NOME_TIPO_CONTA).includes(nome.value)) {
      nome.setValue(NOME_TIPO_CONTA[tipo]);
    }
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const dados = { nome: this.form.controls.nome.value.trim(), tipo: this.form.controls.tipo.value };
    const pedido: Observable<MinhaConta> = this.conta
      ? this.minhaConta.editarConta(this.conta.id, dados)
      : this.minhaConta.abrirConta(dados);
    this.salvando.set(true);
    this.erro.set('');
    pedido.subscribe({
      next: (c) => this.ref.close(c),
      error: (e) => {
        this.salvando.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }
}
