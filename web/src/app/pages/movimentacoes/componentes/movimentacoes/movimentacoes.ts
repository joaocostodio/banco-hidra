import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../../common/erros';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { Icone } from '../../../../common/icone/icone';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { DetalheTransacaoPipe, SinalPipe, TituloTransacaoPipe } from '../../../contas/componentes/transacao.pipes';
import { Transacao } from '../../../contas/model/conta.model';
import { ContaService } from '../../../contas/services/conta.service';

/** Quantas linhas buscar (a API limita em 200). */
const LIMITE = 200;

/** Livro-caixa do banco: as últimas linhas de extrato de todas as contas. */
@Component({
  selector: 'app-movimentacoes',
  imports: [
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    CurrencyPipe,
    DatePipe,
    IconeCategoriaPipe,
    Icone,
    TituloTransacaoPipe,
    DetalheTransacaoPipe,
    SinalPipe,
  ],
  templateUrl: './movimentacoes.html',
  styleUrl: './movimentacoes.css',
})
export class Movimentacoes {
  private readonly contas = inject(ContaService);
  private readonly dialog = inject(MatDialog);

  protected readonly limite = LIMITE;
  protected readonly linhas = rxResource({ stream: () => this.contas.ultimasTransacoes(LIMITE) });
  protected readonly colunas = ['data', 'conta', 'descricao', 'categoria', 'valor'];

  protected readonly categoria = signal<string>('');
  protected readonly categorias = computed(() =>
    [...new Set((this.linhas.value() ?? []).map((t) => t.categoria))].sort(),
  );
  protected readonly filtradas = computed(() =>
    (this.linhas.value() ?? []).filter((t) => !this.categoria() || t.categoria === this.categoria()),
  );

  protected readonly tamanhoPagina = signal(25);
  protected readonly pagina = linkedSignal({ source: this.filtradas, computation: () => 0 });
  protected readonly visiveis = computed(() => {
    const inicio = this.pagina() * this.tamanhoPagina();
    return this.filtradas().slice(inicio, inicio + this.tamanhoPagina());
  });

  protected readonly erro = computed(() => (this.linhas.error() ? mensagemDeErro(this.linhas.error()) : ''));

  protected mudarPagina(e: PageEvent): void {
    this.tamanhoPagina.set(e.pageSize);
    this.pagina.set(e.pageIndex);
  }

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.contas.comprovante(t.autenticacao) });
  }
}
