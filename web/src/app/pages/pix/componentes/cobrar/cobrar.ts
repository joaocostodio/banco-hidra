import { Clipboard } from '@angular/cdk/clipboard';
import { CurrencyPipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { toDataURL } from 'qrcode';
import { mensagemDeErro } from '../../../../common/erros';
import { lerValor, valorValidator } from '../../../../common/validadores';
import { CobrancaPix } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { ChaveLegivelPipe, NOME_TIPO_CHAVE } from '../../services/chaves';

/**
 * Cobrar com QR code: escolho a chave, o valor (opcional) e uma mensagem; o banco devolve o
 * "Pix copia e cola" e a tela desenha o QR code. Quem paga cola o código em "Enviar Pix".
 */
@Component({
  selector: 'app-cobrar',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    CurrencyPipe,
    ChaveLegivelPipe,
  ],
  templateUrl: './cobrar.html',
  styleUrl: './cobrar.css',
})
export class Cobrar {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly clipboard = inject(Clipboard);
  private readonly snack = inject(MatSnackBar);

  protected readonly chaves = rxResource({ stream: () => this.minhaConta.chaves() });
  protected readonly nomes = NOME_TIPO_CHAVE;

  protected readonly form = inject(NonNullableFormBuilder).group({
    chaveId: [0, Validators.min(1)],
    valor: ['', valorValidator],
    descricao: ['', Validators.maxLength(60)],
  });

  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly cobranca = signal<CobrancaPix | null>(null);
  protected readonly qr = signal('');
  protected readonly copiado = signal(false);
  protected readonly semChaves = computed(() => this.chaves.value()?.length === 0);

  constructor() {
    // Já deixa a primeira chave escolhida.
    effect(() => {
      const lista = this.chaves.value();
      if (lista?.length && !this.form.controls.chaveId.value) {
        this.form.controls.chaveId.setValue(lista[0].id);
      }
    });
  }

  protected gerar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { chaveId, valor, descricao } = this.form.getRawValue();
    const numero = valor.trim() ? lerValor(valor) : null;
    this.ocupado.set(true);
    this.erro.set('');
    this.minhaConta.cobrarPix(chaveId, numero, descricao.trim()).subscribe({
      next: async (c) => {
        this.qr.set(await toDataURL(c.codigo, { width: 600, margin: 2, errorCorrectionLevel: 'M' }));
        this.cobranca.set(c);
        this.ocupado.set(false);
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected copiar(c: CobrancaPix): void {
    if (this.clipboard.copy(c.codigo)) {
      this.copiado.set(true);
      this.snack.open('Código copiado. Mande para quem vai te pagar.', 'OK', { duration: 2500 });
      setTimeout(() => this.copiado.set(false), 2500);
    }
  }

  protected novaCobranca(): void {
    this.cobranca.set(null);
    this.qr.set('');
  }
}
