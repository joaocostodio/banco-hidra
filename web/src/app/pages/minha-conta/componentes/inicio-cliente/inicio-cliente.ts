import { CurrencyPipe, PercentPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { ComprovanteDialog } from '../../../../common/comprovante/comprovante';
import { mensagemDeErro } from '../../../../common/erros';
import { valorOu } from '../../../../common/recursos';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { Icone } from '../../../../common/icone/icone';
import { IconeContaPipe } from '../../../financas/services/financas';
import { saudacao } from '../../../painel/services/painel.service';
import { Transacao } from '../../model/minha-conta.model';
import { MinhaContaService, ultimosDias } from '../../services/minha-conta.service';
import { ItemTransacao } from '../item-transacao/item-transacao';

/** Início do app: saldo geral e de cada conta, o mês (receitas × despesas), gastos por categoria e últimas transações. */
@Component({
  selector: 'app-inicio-cliente',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule, CurrencyPipe,
    PercentPipe, ItemTransacao, IconeContaPipe, IconeCategoriaPipe, Icone],
  templateUrl: './inicio-cliente.html',
  styleUrl: './inicio-cliente.css',
  // Ao voltar para a aba do navegador, atualiza (o colega pode ter mandado um Pix).
  host: { '(window:focus)': 'atualizar()' },
})
export class InicioCliente {
  protected readonly auth = inject(AuthService);
  protected readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);

  protected readonly resumo = rxResource({ stream: () => this.minhaConta.resumo() });
  protected readonly recentes = rxResource({ stream: () => this.minhaConta.transacoes(ultimosDias(30)) });
  protected readonly chaves = rxResource({ stream: () => this.minhaConta.chaves() });

  /** O resumo, ou null enquanto carrega / se deu erro (aí aparece a mensagem). */
  protected readonly dados = computed(() => valorOu(this.resumo, null));
  protected readonly ultimas = computed(() => valorOu(this.recentes, []).slice(0, 5));
  protected readonly semChave = computed(() => valorOu(this.chaves, null)?.length === 0);
  protected readonly bloqueadas = computed(() => (this.dados()?.contas ?? []).filter((c) => c.status === 'BLOQUEADA'));
  protected readonly resultadoMes = computed(() => {
    const r = this.dados();
    return r ? r.receitasMes - r.despesasMes : 0;
  });

  /** As 5 categorias com mais gasto no mês, com a fração do total (para a barra). */
  protected readonly gastos = computed(() => {
    const r = this.dados();
    if (!r || r.despesasMes <= 0) {
      return [];
    }
    return r.despesasPorCategoria.slice(0, 5).map((g) => ({ ...g, fracao: g.total / r.despesasMes }));
  });

  protected readonly saudacao = saudacao();

  protected readonly erro = computed(() => {
    const e = this.resumo.error() ?? this.recentes.error();
    return e ? mensagemDeErro(e) : '';
  });

  protected atualizar(): void {
    this.resumo.reload();
    this.recentes.reload();
    this.chaves.reload();
  }

  protected abrirComprovante(t: Transacao): void {
    this.dialog.open(ComprovanteDialog, { data: this.minhaConta.comprovante(t.autenticacao) });
  }
}
