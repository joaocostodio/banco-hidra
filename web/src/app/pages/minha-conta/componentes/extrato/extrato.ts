import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { mensagemDeErro } from '../../../../common/erros';
import { Transacao } from '../../model/minha-conta.model';
import { MinhaContaService, agruparPorDia, nomeDoDia, ultimosDias } from '../../services/minha-conta.service';
import { ItemTransacao } from '../item-transacao/item-transacao';

type Tipo = 'tudo' | 'ENTRADA' | 'SAIDA';

/** Extrato do cliente, agrupado por dia, com filtro de período e de entradas/saídas. */
@Component({
  selector: 'app-extrato',
  imports: [MatButtonToggleModule, MatChipsModule, MatIconModule, MatProgressBarModule, CurrencyPipe, DatePipe, ItemTransacao],
  templateUrl: './extrato.html',
  styleUrl: './extrato.css',
})
export class Extrato {
  protected readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);

  protected readonly dias = signal(30);
  protected readonly tipo = signal<Tipo>('tudo');

  protected readonly extrato = rxResource({
    params: () => ultimosDias(this.dias()),
    stream: ({ params }) => this.minhaConta.extrato(params),
  });

  protected readonly filtradas = computed(() =>
    (this.extrato.value() ?? []).filter((t) => this.tipo() === 'tudo' || t.tipo === this.tipo()),
  );
  protected readonly grupos = computed(() => agruparPorDia(this.filtradas()));

  protected readonly entradas = computed(() => this.somar('ENTRADA'));
  protected readonly saidas = computed(() => this.somar('SAIDA'));

  protected readonly nomeDoDia = nomeDoDia;
  protected readonly mensagemDeErro = mensagemDeErro;

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(t.autenticacao) });
  }

  private somar(tipo: 'ENTRADA' | 'SAIDA'): number {
    return (this.extrato.value() ?? []).filter((t) => t.tipo === tipo).reduce((s, t) => s + t.valor, 0);
  }
}
