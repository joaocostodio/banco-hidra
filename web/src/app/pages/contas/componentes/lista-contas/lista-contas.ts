import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { mensagemDeErro } from '../../../../common/erros';
import { CorAvatarPipe, IniciaisPipe } from '../../../../common/formatos';
import { StatusConta as ChipStatus } from '../../../../common/status-conta/status-conta';
import { somenteDigitos } from '../../../../common/validadores';
import { Conta, StatusConta } from '../../model/conta.model';
import { ContaService } from '../../services/conta.service';

type FiltroConta = 'todas' | StatusConta;

/** Todas as contas do banco, com saldo. */
@Component({
  selector: 'app-lista-contas',
  imports: [
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    CurrencyPipe,
    DatePipe,
    IniciaisPipe,
    CorAvatarPipe,
    ChipStatus,
  ],
  templateUrl: './lista-contas.html',
  styleUrl: './lista-contas.css',
})
export class ListaContas {
  private readonly service = inject(ContaService);
  private readonly router = inject(Router);

  protected readonly contas = rxResource({ stream: () => this.service.listar() });
  protected readonly colunas = ['titular', 'conta', 'abertura', 'situacao', 'saldo'];
  protected readonly filtros: { valor: FiltroConta; nome: string }[] = [
    { valor: 'todas', nome: 'Todas' },
    { valor: 'ATIVA', nome: 'Ativas' },
    { valor: 'BLOQUEADA', nome: 'Bloqueadas' },
    { valor: 'ENCERRADA', nome: 'Encerradas' },
  ];

  protected readonly termo = signal('');
  protected readonly filtro = signal<FiltroConta>('todas');

  protected readonly filtradas = computed(() => {
    const texto = this.termo().trim().toLowerCase();
    const digitos = somenteDigitos(texto);
    return (this.contas.value() ?? []).filter(
      (c) =>
        (this.filtro() === 'todas' || c.status === this.filtro()) &&
        (!texto ||
          c.titularNome.toLowerCase().includes(texto) ||
          (digitos.length >= 3 && somenteDigitos(c.numero).includes(digitos))),
    );
  });

  protected readonly total = computed(() => this.filtradas().reduce((soma, c) => soma + c.saldo, 0));

  protected readonly tamanhoPagina = signal(10);
  protected readonly pagina = linkedSignal({ source: this.filtradas, computation: () => 0 });
  protected readonly visiveis = computed(() => {
    const inicio = this.pagina() * this.tamanhoPagina();
    return this.filtradas().slice(inicio, inicio + this.tamanhoPagina());
  });

  protected readonly erro = computed(() => (this.contas.error() ? mensagemDeErro(this.contas.error()) : ''));

  protected mudarPagina(e: PageEvent): void {
    this.tamanhoPagina.set(e.pageSize);
    this.pagina.set(e.pageIndex);
  }

  protected abrir(c: Conta): void {
    this.router.navigate(['/admin/contas', c.id]);
  }
}
