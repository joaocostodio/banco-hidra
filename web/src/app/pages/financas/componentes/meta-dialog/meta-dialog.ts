import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Observable } from 'rxjs';
import { mensagemDeErro } from '../../../../common/erros';
import { lerValor, valorValidator } from '../../../../common/validadores';
import { MinhaConta } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService, dataApi } from '../../../minha-conta/services/minha-conta.service';

function amanha(): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + 1);
  return d;
}

/** Meta da poupança: quanto guardar, para quê e até quando (opcional). Fecha com a conta atualizada. */
@Component({
  selector: 'app-meta-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatDatepickerModule,
    MatButtonModule, MatIconModule],
  templateUrl: './meta-dialog.html',
  styleUrl: './meta-dialog.css',
})
export class MetaDialog {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly ref = inject<MatDialogRef<MetaDialog, MinhaConta>>(MatDialogRef);
  protected readonly conta = inject<MinhaConta>(MAT_DIALOG_DATA);

  protected readonly minimo = amanha();

  protected readonly form = inject(NonNullableFormBuilder).group({
    valor: [this.conta.meta ? this.conta.meta.valor.toFixed(2).replace('.', ',') : '', [Validators.required, valorValidator]],
    descricao: [this.conta.meta?.descricao ?? '', Validators.maxLength(60)],
    data: [this.conta.meta?.data ? new Date(this.conta.meta.data + 'T00:00') : (null as Date | null)],
  });

  protected readonly salvando = signal(false);
  protected readonly erro = signal('');

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { valor, descricao, data } = this.form.getRawValue();
    this.enviar(
      this.minhaConta.definirMeta(this.conta.id, {
        valor: lerValor(valor),
        descricao: descricao.trim(),
        data: data ? dataApi(data) : null,
      }),
    );
  }

  protected remover(): void {
    this.enviar(this.minhaConta.removerMeta(this.conta.id));
  }

  private enviar(pedido: Observable<MinhaConta>): void {
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
