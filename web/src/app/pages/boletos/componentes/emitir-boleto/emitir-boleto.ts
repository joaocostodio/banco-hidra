import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../../common/erros';
import { lerValor, valorValidator } from '../../../../common/validadores';
import { MinhaContaService, dataApi } from '../../../minha-conta/services/minha-conta.service';
import { REGRA_ATRASO } from '../../services/boletos';

function daquiA(dias: number): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + dias);
  return d;
}

/** Emitir boleto: valor, vencimento e descrição. Depois de emitido, abre o boleto para copiar/imprimir. */
@Component({
  selector: 'app-emitir-boleto',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
  ],
  templateUrl: './emitir-boleto.html',
  styleUrl: '../pagar-boleto/pagar-boleto.css',
})
export class EmitirBoleto {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly router = inject(Router);

  protected readonly hoje = daquiA(0);
  protected readonly limite = daquiA(365);
  protected readonly regraAtraso = REGRA_ATRASO;

  protected readonly form = inject(NonNullableFormBuilder).group({
    valor: ['', [Validators.required, valorValidator]],
    vencimento: [daquiA(7), Validators.required],
    descricao: ['', Validators.maxLength(100)],
  });

  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');

  protected emitir(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { valor, vencimento, descricao } = this.form.getRawValue();
    this.ocupado.set(true);
    this.erro.set('');
    this.minhaConta
      .emitirBoleto({ valor: lerValor(valor), vencimento: dataApi(vencimento), descricao: descricao.trim() })
      .subscribe({
        next: (b) => this.router.navigate(['/app/boletos', b.id], { queryParams: { novo: 1 } }),
        error: (e) => {
          this.ocupado.set(false);
          this.erro.set(mensagemDeErro(e));
        },
      });
  }
}
