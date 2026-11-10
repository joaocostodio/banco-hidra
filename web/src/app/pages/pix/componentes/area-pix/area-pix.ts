import { Clipboard } from '@angular/cdk/clipboard';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { Confirmacao } from '../../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../../common/erros';
import { ChavePix } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { ChaveLegivelPipe, ICONE_TIPO_CHAVE, LIMITE_CHAVES, NOME_TIPO_CHAVE, chaveLegivel } from '../../services/chaves';
import { DadosNovaChave, NovaChaveDialog } from '../nova-chave/nova-chave';

/** Área Pix: enviar e gerenciar as chaves para receber. */
@Component({
  selector: 'app-area-pix',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule, DatePipe, ChaveLegivelPipe],
  templateUrl: './area-pix.html',
  styleUrl: './area-pix.css',
})
export class AreaPix implements OnInit {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly clipboard = inject(Clipboard);
  private readonly router = inject(Router);

  /** ?nova=1 abre o cadastro de chave (vem do convite da tela inicial). */
  readonly nova = input<string>();

  protected readonly chaves = rxResource({ stream: () => this.minhaConta.chaves() });
  protected readonly dados = rxResource({ stream: () => this.minhaConta.dados() });

  protected readonly limite = LIMITE_CHAVES;
  protected readonly nomes = NOME_TIPO_CHAVE;
  protected readonly icones = ICONE_TIPO_CHAVE;
  protected readonly cheio = computed(() => (this.chaves.value()?.length ?? 0) >= LIMITE_CHAVES);
  protected readonly erro = computed(() => (this.chaves.error() ? mensagemDeErro(this.chaves.error()) : ''));

  ngOnInit(): void {
    if (this.nova()) {
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
      // Espera os dados (CPF/e-mail) para abrir o cadastro já preenchido.
      this.minhaConta.dados().subscribe((dados) => this.abrirCadastro(dados, []));
    }
  }

  protected cadastrar(): void {
    const dados = this.dados.value();
    if (dados) {
      this.abrirCadastro(dados, this.chaves.value() ?? []);
    }
  }

  protected copiar(c: ChavePix): void {
    if (this.clipboard.copy(chaveLegivel(c.tipo, c.valor))) {
      this.snack.open('Chave copiada. Mande para quem vai te pagar.', 'OK', { duration: 2500 });
    }
  }

  protected excluir(c: ChavePix): void {
    this.dialog
      .open(Confirmacao, {
        data: {
          titulo: 'Excluir chave?',
          mensagem: `Ninguém mais vai conseguir te mandar Pix pela chave ${chaveLegivel(c.tipo, c.valor)}. Você pode cadastrá-la de novo depois.`,
          confirmar: 'Excluir chave',
          icone: 'delete',
          perigo: true,
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.minhaConta.excluirChave(c.id)),
      )
      .subscribe({
        next: () => {
          this.snack.open('Chave excluída.', 'OK', { duration: 2500 });
          this.chaves.reload();
        },
        error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 }),
      });
  }

  private abrirCadastro(dados: DadosNovaChave['dados'], existentes: ChavePix[]): void {
    this.dialog
      .open<NovaChaveDialog, DadosNovaChave, ChavePix>(NovaChaveDialog, { data: { dados, existentes } })
      .afterClosed()
      .pipe(filter((c): c is ChavePix => !!c))
      .subscribe(() => {
        this.snack.open('Chave cadastrada! Já dá para receber Pix por ela.', 'OK', { duration: 3500 });
        this.chaves.reload();
      });
  }
}
