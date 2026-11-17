import { PercentPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Observable, of } from 'rxjs';
import { mensagemDeErro } from '../../../../common/erros';
import { valorOu } from '../../../../common/recursos';
import { MinhaConta, ProdutoInvestimentoId, TipoConta } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { DESCRICAO_TIPO_CONTA, ICONE_TIPO_CONTA, NOME_TIPO_CONTA, TIPOS_NOVA_CONTA } from '../../services/financas';

/** Sem conta: abrir uma nova. Com conta: renomear (o tipo não muda depois de aberta). */
export type DadosContaDialog = MinhaConta | null;

/** Abrir ou renomear uma conta. Fecha com a conta salva. */
@Component({
  selector: 'app-conta-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, PercentPipe],
  templateUrl: './conta-dialog.html',
  styleUrl: './conta-dialog.css',
})
export class ContaDialog {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly ref = inject<MatDialogRef<ContaDialog, MinhaConta>>(MatDialogRef);
  protected readonly conta = inject<DadosContaDialog>(MAT_DIALOG_DATA);

  protected readonly tipos = TIPOS_NOVA_CONTA;
  protected readonly nomes = NOME_TIPO_CONTA;
  protected readonly icones = ICONE_TIPO_CONTA;
  protected readonly descricoes = DESCRICAO_TIPO_CONTA;

  /** Os produtos só são buscados na abertura (na edição não há o que escolher). */
  protected readonly produtos = rxResource({
    stream: () => (this.conta ? of([]) : this.minhaConta.produtosInvestimento()),
  });
  protected readonly listaProdutos = computed(() => valorOu(this.produtos, []));

  protected readonly form = inject(NonNullableFormBuilder).group({
    tipo: [this.conta?.tipo ?? ('POUPANCA' as TipoConta), Validators.required],
    produto: ['CDB' as ProdutoInvestimentoId],
    nome: [this.conta?.nome ?? '', Validators.maxLength(40)],
  });

  private readonly tipo = toSignal(this.form.controls.tipo.valueChanges, { initialValue: this.form.controls.tipo.value });
  protected readonly ehInvestimento = computed(() => !this.conta && this.tipo() === 'INVESTIMENTO');

  protected readonly salvando = signal(false);
  protected readonly erro = signal('');

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { tipo, produto, nome } = this.form.getRawValue();
    const pedido: Observable<MinhaConta> = this.conta
      ? this.minhaConta.renomearConta(this.conta.id, nome.trim() || this.conta.nome)
      : this.minhaConta.abrirConta({ tipo, nome: nome.trim(), produto: tipo === 'INVESTIMENTO' ? produto : null });
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
