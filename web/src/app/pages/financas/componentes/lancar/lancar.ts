import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../../common/erros';
import { valorOu } from '../../../../common/recursos';
import { lerValor, valorValidator } from '../../../../common/validadores';
import { TipoLancamento } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { IconeContaPipe } from '../../services/financas';

/** Lançar uma receita (entra no saldo) ou despesa (sai do saldo) numa das minhas contas. */
@Component({
  selector: 'app-lancar',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    CurrencyPipe,
    IconeContaPipe,
  ],
  templateUrl: './lancar.html',
  styleUrl: './lancar.css',
})
export class Lancar implements OnInit {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  /** ?tipo=RECEITA|DESPESA e ?conta=id já escolhidos (vêm dos atalhos). */
  readonly tipoInicial = input<string>(undefined, { alias: 'tipo' });
  readonly contaInicial = input<string>(undefined, { alias: 'conta' });

  protected readonly contas = rxResource({ stream: () => this.minhaConta.contas() });
  protected readonly categorias = rxResource({ stream: () => this.minhaConta.categorias() });

  /**
   * Só contas ativas recebem lançamento, e o investimento não (ele só movimenta com aplicação e resgate).
   * Categorias de sistema (Pix...) são do banco.
   */
  protected readonly contasAtivas = computed(() =>
    valorOu(this.contas, []).filter((c) => c.status === 'ATIVA' && c.tipo !== 'INVESTIMENTO'),
  );
  protected readonly categoriasLancaveis = computed(() => valorOu(this.categorias, []).filter((c) => !c.sistema));
  protected readonly erroCarga = computed(() => {
    const e = this.contas.error() ?? this.categorias.error();
    return e ? mensagemDeErro(e) : '';
  });

  protected readonly form = inject(NonNullableFormBuilder).group({
    tipo: ['DESPESA' as TipoLancamento, Validators.required],
    contaId: [0, Validators.min(1)],
    categoriaId: [0, Validators.min(1)],
    valor: ['', [Validators.required, valorValidator]],
    descricao: ['', Validators.maxLength(200)],
  });

  private readonly tipo = toSignal(this.form.controls.tipo.valueChanges, { initialValue: this.form.controls.tipo.value });
  private readonly contaEscolhida = toSignal(this.form.controls.contaId.valueChanges, { initialValue: 0 });
  protected readonly ehReceita = computed(() => this.tipo() === 'RECEITA');
  protected readonly saldoDaConta = computed(
    () => this.contasAtivas().find((c) => c.id === this.contaEscolhida())?.saldo ?? null,
  );

  protected readonly salvando = signal(false);
  protected readonly erro = signal('');

  constructor() {
    // Escolhe a conta da URL ou a principal assim que as contas chegam.
    effect(() => {
      const lista = this.contasAtivas();
      const controle = this.form.controls.contaId;
      if (lista.length && !controle.value) {
        const daUrl = lista.find((c) => c.id === Number(this.contaInicial()));
        controle.setValue((daUrl ?? lista[0]).id);
      }
    });
  }

  ngOnInit(): void {
    if (this.tipoInicial() === 'RECEITA' || this.tipoInicial() === 'DESPESA') {
      this.form.controls.tipo.setValue(this.tipoInicial() as TipoLancamento);
    }
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.erro.set('');
    this.minhaConta
      .lancar({ contaId: v.contaId, tipo: v.tipo, valor: lerValor(v.valor), categoriaId: v.categoriaId, descricao: v.descricao.trim() })
      .subscribe({
        next: (t) => {
          const texto = v.tipo === 'RECEITA' ? 'Receita lançada' : 'Despesa lançada';
          this.snack.open(`${texto} em ${t.contaNome}.`, 'OK', { duration: 3000 });
          this.router.navigateByUrl('/app');
        },
        error: (e) => {
          this.salvando.set(false);
          this.erro.set(mensagemDeErro(e));
        },
      });
  }
}
