import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { mensagemDeErro } from '../../../../common/erros';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { Icone } from '../../../../common/icone/icone';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { DetalheTransacaoPipe, SinalPipe, TituloTransacaoPipe } from '../../../contas/componentes/transacao.pipes';
import { Transacao } from '../../../contas/model/conta.model';
import { ContaService } from '../../../contas/services/conta.service';
import { PainelService, alturasDasBarras, saudacao } from '../../services/painel.service';

/** Painel inicial do ADM: números do banco, volume da semana e últimas movimentações. */
@Component({
  selector: 'app-painel',
  imports: [
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressBarModule,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    IconeCategoriaPipe,
    Icone,
    TituloTransacaoPipe,
    DetalheTransacaoPipe,
    SinalPipe,
  ],
  templateUrl: './painel.html',
  styleUrl: './painel.css',
})
export class Painel {
  private readonly painel = inject(PainelService);
  private readonly contas = inject(ContaService);
  private readonly dialog = inject(MatDialog);
  protected readonly auth = inject(AuthService);

  protected readonly resumo = rxResource({ stream: () => this.painel.resumo() });
  protected readonly ultimas = rxResource({ stream: () => this.contas.ultimasTransacoes(6) });

  protected readonly hoje = new Date();
  protected readonly saudacao = saudacao(this.hoje);
  protected readonly colunas = ['descricao', 'conta', 'quando', 'valor'];

  protected readonly barras = computed(() => {
    const dias = this.resumo.value()?.ultimosDias ?? [];
    const alturas = alturasDasBarras(dias);
    return dias.map((d, i) => ({ ...d, altura: alturas[i], hoje: i === dias.length - 1 }));
  });

  protected readonly volumeSemana = computed(() =>
    (this.resumo.value()?.ultimosDias ?? []).reduce((soma, d) => soma + d.valor, 0),
  );

  protected readonly erro = computed(() => {
    const e = this.resumo.error() ?? this.ultimas.error();
    return e ? mensagemDeErro(e) : '';
  });

  protected recarregar(): void {
    this.resumo.reload();
    this.ultimas.reload();
  }

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.contas.comprovante(t.autenticacao) });
  }
}
