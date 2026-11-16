import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { mensagemDeErro } from '../../../../common/erros';
import { valorOu } from '../../../../common/recursos';
import { Transacao } from '../../model/minha-conta.model';
import { MinhaContaService, agruparPorDia, nomeDoDia, ultimosDias } from '../../services/minha-conta.service';
import { ItemTransacao } from '../item-transacao/item-transacao';

type Tipo = 'tudo' | 'ENTRADA' | 'SAIDA';

/**
 * Transações de todas as minhas contas, agrupadas por dia, com filtros por conta, categoria,
 * tipo (entradas/saídas) e período. Os filtros vão para a API (ela devolve só o que pedi).
 */
@Component({
  selector: 'app-extrato',
  imports: [RouterLink, MatButtonModule, MatButtonToggleModule, MatChipsModule, MatFormFieldModule, MatSelectModule,
    MatIconModule, MatProgressBarModule, CurrencyPipe, DatePipe, ItemTransacao],
  templateUrl: './extrato.html',
  styleUrl: './extrato.css',
})
export class Extrato implements OnInit {
  protected readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);

  /** Filtros vindos da URL (?conta=, ?categoria=, ?tipo=), ex.: ao tocar numa conta na tela inicial. */
  readonly contaDaUrl = input<string>(undefined, { alias: 'conta' });
  readonly categoriaDaUrl = input<string>(undefined, { alias: 'categoria' });
  readonly tipoDaUrl = input<string>(undefined, { alias: 'tipo' });

  protected readonly dias = signal(30);
  protected readonly tipo = signal<Tipo>('tudo');
  protected readonly contaId = signal<number | null>(null);
  protected readonly categoriaId = signal<number | null>(null);

  protected readonly contas = rxResource({ stream: () => this.minhaConta.contas() });
  protected readonly categorias = rxResource({ stream: () => this.minhaConta.categorias() });

  protected readonly extrato = rxResource({
    params: () => ({
      ...ultimosDias(this.dias()),
      contaId: this.contaId(),
      categoriaId: this.categoriaId(),
      tipo: this.tipo() === 'tudo' ? null : (this.tipo() as 'ENTRADA' | 'SAIDA'),
    }),
    stream: ({ params }) => this.minhaConta.transacoes(params),
  });

  /** Listas dos filtros (vazias enquanto carregam ou se der erro). */
  protected readonly listaContas = computed(() => valorOu(this.contas, []));
  protected readonly listaCategorias = computed(() => valorOu(this.categorias, []));
  private readonly transacoes = computed(() => valorOu(this.extrato, []));

  protected readonly grupos = computed(() => agruparPorDia(this.transacoes()));
  protected readonly entradas = computed(() => this.somar('ENTRADA'));
  protected readonly saidas = computed(() => this.somar('SAIDA'));

  /** O "saldo do dia" só faz sentido olhando uma conta só, sem filtrar categoria nem tipo. */
  protected readonly mostraSaldoDoDia = computed(
    () => this.contaId() !== null && this.categoriaId() === null && this.tipo() === 'tudo',
  );
  protected readonly temFiltro = computed(
    () => this.contaId() !== null || this.categoriaId() !== null || this.tipo() !== 'tudo',
  );

  protected readonly nomeDoDia = nomeDoDia;
  protected readonly mensagemDeErro = mensagemDeErro;

  ngOnInit(): void {
    const conta = Number(this.contaDaUrl());
    const categoria = Number(this.categoriaDaUrl());
    if (conta > 0) {
      this.contaId.set(conta);
    }
    if (categoria > 0) {
      this.categoriaId.set(categoria);
    }
    if (this.tipoDaUrl() === 'ENTRADA' || this.tipoDaUrl() === 'SAIDA') {
      this.tipo.set(this.tipoDaUrl() as Tipo);
    }
  }

  protected limparFiltros(): void {
    this.contaId.set(null);
    this.categoriaId.set(null);
    this.tipo.set('tudo');
  }

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(t.autenticacao) });
  }

  private somar(tipo: 'ENTRADA' | 'SAIDA'): number {
    return this.transacoes().filter((t) => t.tipo === tipo).reduce((s, t) => s + t.valor, 0);
  }
}
