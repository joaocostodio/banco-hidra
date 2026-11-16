import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { mensagemDeErro } from '../../../../common/erros';
import { somenteDigitos } from '../../../../common/validadores';
import { ConsultaBoleto, Transacao } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';

type Passo = 'linha' | 'confirmar' | 'pronto';

/** Pagar boleto: digitar a linha → conferir (com multa e juros, se vencido) → pronto. */
@Component({
  selector: 'app-pagar-boleto',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    CurrencyPipe,
    DatePipe,
  ],
  templateUrl: './pagar-boleto.html',
  styleUrl: './pagar-boleto.css',
})
export class PagarBoleto {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);

  protected readonly minha = rxResource({ stream: () => this.minhaConta.conta() });

  protected readonly form = inject(NonNullableFormBuilder).group({ linha: ['', Validators.required] });
  private readonly linhaDigitada = toSignal(this.form.controls.linha.valueChanges, { initialValue: '' });
  protected readonly digitos = computed(() => somenteDigitos(this.linhaDigitada()).length);

  protected readonly passo = signal<Passo>('linha');
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly boleto = signal<ConsultaBoleto | null>(null);
  protected readonly resultado = signal<Transacao | null>(null);
  protected readonly saldoDepois = computed(() => (this.minha.value()?.saldo ?? 0) - (this.boleto()?.total ?? 0));

  protected consultar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.ocupado.set(true);
    this.erro.set('');
    this.minhaConta.consultarBoleto(this.form.controls.linha.value).subscribe({
      next: (b) => {
        this.ocupado.set(false);
        if (b.status === 'PAGO') {
          this.erro.set('Este boleto já foi pago.');
        } else if (b.status === 'CANCELADO') {
          this.erro.set('Este boleto foi cancelado por quem emitiu.');
        } else if (b.beneficiario.conta === this.minha.value()?.numero) {
          this.erro.set('Este boleto foi emitido por você: mande para quem vai pagar.');
        } else {
          this.boleto.set(b);
          this.minha.reload();
          this.passo.set('confirmar');
        }
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected pagar(): void {
    this.ocupado.set(true);
    this.erro.set('');
    this.minhaConta.pagarBoleto(this.form.controls.linha.value).subscribe({
      next: (t) => {
        this.ocupado.set(false);
        this.resultado.set(t);
        this.passo.set('pronto');
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
        this.minha.reload();
      },
    });
  }

  protected voltar(): void {
    this.erro.set('');
    this.passo.set('linha');
  }

  protected recomecar(): void {
    this.form.reset();
    this.boleto.set(null);
    this.resultado.set(null);
    this.erro.set('');
    this.passo.set('linha');
  }

  protected abrirComprovante(): void {
    const t = this.resultado();
    if (t) {
      this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(t.autenticacao) });
    }
  }
}
