import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { mensagemDeErro } from '../../../../common/erros';
import { IniciaisPipe } from '../../../../common/formatos';
import { lerValor, somenteDigitos, valorValidator } from '../../../../common/validadores';
import { Destinatario, Transacao } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService, pareceCodigoPix, ultimosDias } from '../../../minha-conta/services/minha-conta.service';
import { NOME_TIPO_CHAVE } from '../../services/chaves';

export type ModoEnvio = 'pix' | 'transferencia';
type Passo = 'destino' | 'valor' | 'confirmar' | 'pronto';

interface Contato {
  nome: string;
  conta: string;
}

/** 123456-7 enquanto digita. */
function formatarNumeroConta(texto: string): string {
  const d = somenteDigitos(texto).slice(0, 7);
  return d.length > 6 ? `${d.slice(0, 6)}-${d.slice(6)}` : d;
}

/**
 * Enviar dinheiro em 4 passos: para quem → quanto → confirmar → pronto.
 * O mesmo componente faz Pix (por chave) e transferência (por agência e conta).
 */
@Component({
  selector: 'app-enviar',
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
    IniciaisPipe,
  ],
  templateUrl: './enviar.html',
  styleUrl: './enviar.css',
})
export class Enviar implements OnInit {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Vem do "data" da rota. */
  readonly modo = input<ModoEnvio>('pix');
  /** ?chave= (Pix) ou ?conta= (transferência) já preenchidos. */
  readonly chave = input<string>();
  readonly conta = input<string>();

  protected readonly passo = signal<Passo>('destino');
  protected readonly ocupado = signal(false);
  protected readonly erro = signal('');
  protected readonly destinatario = signal<Destinatario | null>(null);
  protected readonly resultado = signal<Transacao | null>(null);
  /** Veio de um "Pix copia e cola" com valor: não dá para mudar o valor. */
  protected readonly valorFixo = signal(false);

  protected readonly minha = rxResource({ stream: () => this.minhaConta.conta() });
  private readonly historico = rxResource({
    params: () => (this.modo() === 'transferencia' ? ultimosDias(90) : undefined),
    stream: ({ params }) => this.minhaConta.extrato(params),
  });

  /** Para quem já mandei dinheiro (atalho na transferência). */
  protected readonly contatos = computed<Contato[]>(() => {
    const vistos = new Map<string, Contato>();
    for (const t of this.historico.value() ?? []) {
      if (t.tipo === 'SAIDA' && t.contraparteConta && t.contraparteNome && !vistos.has(t.contraparteConta)) {
        vistos.set(t.contraparteConta, { nome: t.contraparteNome, conta: t.contraparteConta });
      }
    }
    return [...vistos.values()].slice(0, 4);
  });

  // ---------- Formulários ----------
  protected readonly destinoPix = this.fb.group({ chave: ['', Validators.required] });
  protected readonly destinoConta = this.fb.group({
    agencia: ['0001', [Validators.required, Validators.pattern(/^\d{4}$/)]],
    numero: ['', [Validators.required, Validators.pattern(/^\d{6}-\d$/)]],
  });
  protected readonly valores = this.fb.group({
    valor: ['', [Validators.required, valorValidator, (c: AbstractControl) => this.cabeNoSaldo(c)]],
    descricao: ['', Validators.maxLength(140)],
  });
  private readonly valorDigitado = toSignal(this.valores.controls.valor.valueChanges, { initialValue: '' });

  protected readonly valor = computed(() => {
    const v = lerValor(this.valorDigitado());
    return Number.isNaN(v) ? 0 : v;
  });
  protected readonly saldoDepois = computed(() => (this.minha.value()?.saldo ?? 0) - this.valor());

  protected readonly ehPix = computed(() => this.modo() === 'pix');
  protected readonly titulo = computed(() => (this.ehPix() ? 'Enviar Pix' : 'Transferir'));
  protected readonly nomeTipoChave = NOME_TIPO_CHAVE;
  protected readonly passos: Passo[] = ['destino', 'valor', 'confirmar'];

  /** Campo principal de cada passo: recebe o foco quando o passo aparece. */
  private readonly foco = viewChild<ElementRef<HTMLInputElement>>('foco');

  constructor() {
    effect(() => this.foco()?.nativeElement.focus());
  }

  ngOnInit(): void {
    if (this.chave()) {
      this.destinoPix.setValue({ chave: this.chave()! });
    }
    if (this.conta()) {
      this.destinoConta.controls.numero.setValue(formatarNumeroConta(this.conta()!));
    }
  }

  // ---------- Passo 1: para quem ----------
  protected formatarNumero(): void {
    const c = this.destinoConta.controls.numero;
    c.setValue(formatarNumeroConta(c.value), { emitEvent: false });
  }

  protected usarContato(c: Contato): void {
    this.destinoConta.setValue({ agencia: '0001', numero: c.conta });
    this.buscarDestino();
  }

  protected buscarDestino(): void {
    const form = this.ehPix() ? this.destinoPix : this.destinoConta;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    if (this.ehPix() && pareceCodigoPix(this.destinoPix.controls.chave.value)) {
      this.lerCodigo(this.destinoPix.controls.chave.value.trim());
      return;
    }
    const busca: Observable<Destinatario> = this.ehPix()
      ? this.minhaConta.destinatarioPorChave(this.destinoPix.controls.chave.value.trim())
      : this.minhaConta.destinatarioPorConta(this.destinoConta.controls.agencia.value, this.destinoConta.controls.numero.value);

    this.ocupado.set(true);
    this.erro.set('');
    busca.subscribe({
      next: (d) => {
        this.ocupado.set(false);
        if (d.conta === this.minha.value()?.numero) {
          this.erro.set(this.ehPix() ? 'Essa chave é sua. Escolha a chave de outra pessoa.' : 'Essa é a sua própria conta.');
          return;
        }
        this.destinatario.set(d);
        this.passo.set('valor');
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(
          e instanceof HttpErrorResponse && e.status === 404
            ? this.ehPix()
              ? 'Não encontramos essa chave Pix. Confira e tente de novo.'
              : 'Não encontramos essa conta. Confira agência e número.'
            : mensagemDeErro(e),
        );
      },
    });
  }

  /**
   * "Pix copia e cola": a API confere o código e diz de quem é a chave. Se a cobrança já tem
   * valor, ele fica travado e a tela pula direto para a confirmação.
   */
  private lerCodigo(codigo: string): void {
    this.ocupado.set(true);
    this.erro.set('');
    this.minhaConta.lerCodigoPix(codigo).subscribe({
      next: (lido) => {
        this.ocupado.set(false);
        if (lido.destinatario.conta === this.minha.value()?.numero) {
          this.erro.set('Esse código é seu: mande para quem vai te pagar.');
          return;
        }
        this.destinoPix.setValue({ chave: lido.chave });
        this.destinatario.set(lido.destinatario);
        this.valores.controls.descricao.setValue(lido.descricao ?? '');
        if (lido.valor) {
          this.valorFixo.set(true);
          this.valores.controls.valor.setValue(lido.valor.toFixed(2).replace('.', ','));
          this.valores.controls.valor.disable();
          this.passo.set('confirmar');
        } else {
          this.passo.set('valor');
        }
      },
      error: (e) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  // ---------- Passo 2: quanto ----------
  protected continuarValor(): void {
    this.valores.controls.valor.updateValueAndValidity();
    if (this.valores.invalid) {
      this.valores.markAllAsTouched();
      return;
    }
    this.erro.set('');
    this.passo.set('confirmar');
  }

  // ---------- Passo 3: confirmar ----------
  protected enviar(): void {
    const d = this.destinatario();
    if (!d) {
      return;
    }
    const valor = this.valor();
    const descricao = this.valores.controls.descricao.value.trim();
    const envio: Observable<Transacao> = this.ehPix()
      ? this.minhaConta.enviarPix({ chave: this.destinoPix.controls.chave.value.trim(), valor, descricao })
      : this.minhaConta.transferir({ agencia: d.agencia, numero: d.conta, valor, descricao });

    this.ocupado.set(true);
    this.erro.set('');
    envio.subscribe({
      next: (t) => {
        this.ocupado.set(false);
        this.resultado.set(t);
        this.passo.set('pronto');
        this.minha.reload();
      },
      error: (e) => {
        this.ocupado.set(false);
        const msg = mensagemDeErro(e);
        this.erro.set(msg);
        if (msg.toLowerCase().includes('saldo')) {
          this.minha.reload();
          this.passo.set('valor');
        }
      },
    });
  }

  // ---------- Navegação ----------
  protected voltar(): void {
    this.erro.set('');
    if (this.valorFixo()) {
      this.recomecar(); // cobrança com valor travado: voltar é escolher outro código
      return;
    }
    this.passo.update((p) => (p === 'confirmar' ? 'valor' : 'destino'));
  }

  protected recomecar(): void {
    this.destinoPix.reset();
    this.destinoConta.reset();
    this.valores.reset();
    this.valores.controls.valor.enable();
    this.valorFixo.set(false);
    this.destinatario.set(null);
    this.resultado.set(null);
    this.erro.set('');
    this.passo.set('destino');
  }

  protected abrirComprovante(): void {
    const t = this.resultado();
    if (t) {
      this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(t.autenticacao) });
    }
  }

  protected indice(p: Passo): number {
    return this.passos.indexOf(p);
  }

  private cabeNoSaldo(c: AbstractControl): ValidationErrors | null {
    const saldo = this.minha?.value()?.saldo;
    const v = lerValor(c.value);
    if (saldo === undefined || Number.isNaN(v)) {
      return null;
    }
    return v > saldo ? { saldo: true } : null;
  }
}
