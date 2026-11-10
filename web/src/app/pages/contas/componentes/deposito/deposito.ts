import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { mensagemDeErro } from '../../../../common/erros';
import { lerValor, valorValidator } from '../../../../common/validadores';
import { Conta, Transacao } from '../../model/conta.model';
import { ContaService } from '../../services/conta.service';

/** Depósito feito pelo banco na conta do cliente. */
@Component({
  selector: 'app-deposito',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, CurrencyPipe],
  templateUrl: './deposito.html',
  styleUrl: './deposito.css',
})
export class DepositoDialog {
  protected readonly conta = inject<Conta>(MAT_DIALOG_DATA);
  private readonly contas = inject(ContaService);
  private readonly ref = inject<MatDialogRef<DepositoDialog, Transacao>>(MatDialogRef);

  protected readonly form = inject(NonNullableFormBuilder).group({
    valor: ['', [Validators.required, valorValidator]],
    descricao: ['', Validators.maxLength(200)],
  });

  private readonly valorDigitado = toSignal(this.form.controls.valor.valueChanges, { initialValue: '' });
  protected readonly saldoDepois = computed(() => {
    const v = lerValor(this.valorDigitado());
    return Number.isNaN(v) || v <= 0 ? null : this.conta.saldo + v;
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');
  protected readonly atalhos = [50, 100, 500, 1000];

  protected usarAtalho(valor: number): void {
    this.form.controls.valor.setValue(valor.toFixed(2).replace('.', ','));
    this.form.controls.valor.markAsTouched();
  }

  protected depositar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { valor, descricao } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set('');
    this.contas.depositar(this.conta.id, { valor: lerValor(valor), descricao: descricao.trim() }).subscribe({
      next: (t) => this.ref.close(t),
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
