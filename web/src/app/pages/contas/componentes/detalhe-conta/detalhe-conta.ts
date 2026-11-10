import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, input, numberAttribute, signal } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { Observable, filter, switchMap } from 'rxjs';
import { Confirmacao, DadosConfirmacao } from '../../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../../common/erros';
import { CorAvatarPipe, IconeCategoriaPipe, IniciaisPipe } from '../../../../common/formatos';
import { Icone } from '../../../../common/icone/icone';
import { StatusConta } from '../../../../common/status-conta/status-conta';
import { AcoesCliente } from '../../../clientes/services/acoes-cliente.service';
import { ClienteService } from '../../../clientes/services/cliente.service';
import { Conta, Periodo, Transacao } from '../../model/conta.model';
import { ContaService, dataApi } from '../../services/conta.service';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { DepositoDialog } from '../deposito/deposito';
import { DetalheTransacaoPipe, SinalPipe, TituloTransacaoPipe } from '../transacao.pipes';

type Atalho = 7 | 30 | 90 | 'outro';

const NOME_CHAVE = { CPF: 'CPF', EMAIL: 'E-mail', TELEFONE: 'Celular', ALEATORIA: 'Aleatória' } as const;
const ICONE_CHAVE = { CPF: 'badge', EMAIL: 'mail', TELEFONE: 'smartphone', ALEATORIA: 'casino' } as const;

/** Uma conta vista pelo ADM: titular, saldo, ações, extrato e chaves Pix. */
@Component({
  selector: 'app-detalhe-conta',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTableModule,
    MatTabsModule,
    MatTooltipModule,
    CurrencyPipe,
    DatePipe,
    IniciaisPipe,
    CorAvatarPipe,
    IconeCategoriaPipe,
    Icone,
    TituloTransacaoPipe,
    DetalheTransacaoPipe,
    SinalPipe,
    StatusConta,
  ],
  templateUrl: './detalhe-conta.html',
  styleUrl: './detalhe-conta.css',
})
export class DetalheConta {
  private readonly contas = inject(ContaService);
  private readonly clientes = inject(ClienteService);
  private readonly acoes = inject(AcoesCliente);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  /** :id da rota. */
  readonly id = input.required({ transform: numberAttribute });

  protected readonly conta = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.contas.buscar(params),
  });
  protected readonly cliente = rxResource({
    params: () => this.conta.value()?.titularId,
    stream: ({ params }) => this.clientes.buscar(params),
  });
  protected readonly chaves = rxResource({
    params: () => this.id(),
    stream: ({ params }) => this.contas.chavesPix(params),
  });

  // ---------- Período do extrato ----------
  protected readonly atalho = signal<Atalho>(30);
  protected readonly intervalo = new FormGroup({
    inicio: new FormControl<Date | null>(null),
    fim: new FormControl<Date | null>(null),
  });
  private readonly intervaloEscolhido = toSignal(this.intervalo.valueChanges, { initialValue: this.intervalo.value });
  protected readonly hoje = new Date();
  protected readonly umAnoAtras = new Date(this.hoje.getFullYear() - 1, this.hoje.getMonth(), this.hoje.getDate() + 1);

  protected readonly periodo = computed<Periodo | undefined>(() => {
    const atalho = this.atalho();
    if (atalho === 'outro') {
      const { inicio, fim } = this.intervaloEscolhido();
      return inicio && fim ? { inicio: dataApi(inicio), fim: dataApi(fim) } : undefined;
    }
    const inicio = new Date();
    inicio.setDate(inicio.getDate() - (atalho - 1));
    return { inicio: dataApi(inicio), fim: dataApi(this.hoje) };
  });

  protected readonly extrato = rxResource({
    params: () => {
      const p = this.periodo();
      return p ? { id: this.id(), ...p } : undefined;
    },
    stream: ({ params }) => this.contas.extrato(params.id, params),
  });

  protected readonly entradas = computed(() => this.somar('ENTRADA'));
  protected readonly saidas = computed(() => this.somar('SAIDA'));

  protected readonly colunas = ['data', 'descricao', 'categoria', 'valor', 'saldo'];
  protected readonly nomeChave = NOME_CHAVE;
  protected readonly iconeChave = ICONE_CHAVE;
  protected readonly mensagemDeErro = mensagemDeErro;

  protected readonly erro = computed(() => {
    const e = this.conta.error();
    return e ? mensagemDeErro(e) : '';
  });

  // ---------- Ações ----------
  protected depositar(conta: Conta): void {
    this.dialog
      .open<DepositoDialog, Conta, Transacao>(DepositoDialog, { data: conta })
      .afterClosed()
      .pipe(filter((t): t is Transacao => !!t))
      .subscribe(() => {
        this.snack.open('Depósito feito.', 'OK', { duration: 3000 });
        this.recarregar();
      });
  }

  protected alternarBloqueio(conta: Conta): void {
    const bloquear = conta.status === 'ATIVA';
    const dados: DadosConfirmacao = bloquear
      ? {
          titulo: 'Bloquear a conta?',
          mensagem: `Enquanto estiver bloqueada, a conta de ${conta.titularNome} não envia nem recebe dinheiro (Pix, transferências e depósitos). O saldo fica guardado.`,
          confirmar: 'Bloquear',
          icone: 'lock',
        }
      : {
          titulo: 'Desbloquear a conta?',
          mensagem: `A conta de ${conta.titularNome} volta a movimentar normalmente.`,
          confirmar: 'Desbloquear',
          icone: 'lock_open',
        };
    this.confirmarE(dados, () => (bloquear ? this.contas.bloquear(conta.id) : this.contas.desbloquear(conta.id)), bloquear ? 'Conta bloqueada.' : 'Conta desbloqueada.');
  }

  protected encerrar(conta: Conta): void {
    this.confirmarE(
      {
        titulo: 'Encerrar a conta?',
        mensagem: `A conta ${conta.numero} de ${conta.titularNome} será encerrada e não poderá ser reaberta. O histórico continua guardado.`,
        confirmar: 'Encerrar conta',
        icone: 'block',
        perigo: true,
      },
      () => this.contas.encerrar(conta.id),
      'Conta encerrada.',
    );
  }

  protected editarCliente(): void {
    const c = this.cliente.value();
    if (c) {
      this.acoes.editar(c).subscribe(() => this.recarregar());
    }
  }

  protected novaSenha(): void {
    const c = this.cliente.value();
    if (c) {
      this.acoes.novaSenhaProvisoria(c).subscribe(() => this.cliente.reload());
    }
  }

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.contas.comprovante(t.autenticacao) });
  }

  protected escolherAtalho(valor: Atalho): void {
    this.atalho.set(valor);
  }

  private confirmarE(dados: DadosConfirmacao, acao: () => Observable<Conta>, sucesso: string): void {
    this.dialog
      .open(Confirmacao, { data: dados })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => acao()),
      )
      .subscribe({
        next: () => {
          this.snack.open(sucesso, 'OK', { duration: 3000 });
          this.recarregar();
        },
        error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
      });
  }

  private recarregar(): void {
    this.conta.reload();
    this.cliente.reload();
    this.extrato.reload();
  }

  private somar(tipo: 'ENTRADA' | 'SAIDA'): number {
    return (this.extrato.value() ?? []).filter((t) => t.tipo === tipo).reduce((s, t) => s + t.valor, 0);
  }
}
