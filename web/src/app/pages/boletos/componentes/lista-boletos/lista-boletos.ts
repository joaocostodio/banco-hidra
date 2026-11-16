import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../../../common/erros';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { SituacaoBoletoPipe } from '../../services/boletos';

/** Área de boletos: pagar, emitir e os boletos que eu emiti. */
@Component({
  selector: 'app-lista-boletos',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, CurrencyPipe, DatePipe, SituacaoBoletoPipe],
  templateUrl: './lista-boletos.html',
  styleUrl: './lista-boletos.css',
})
export class ListaBoletos {
  private readonly minhaConta = inject(MinhaContaService);

  protected readonly boletos = rxResource({ stream: () => this.minhaConta.boletos() });
  protected readonly erro = computed(() => (this.boletos.error() ? mensagemDeErro(this.boletos.error()) : ''));
  protected readonly aReceber = computed(() =>
    (this.boletos.value() ?? []).filter((b) => b.status === 'ABERTO').reduce((s, b) => s + b.valor, 0),
  );
}
