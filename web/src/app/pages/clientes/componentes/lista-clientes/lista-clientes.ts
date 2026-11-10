import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, linkedSignal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { mensagemDeErro } from '../../../../common/erros';
import { CorAvatarPipe, IniciaisPipe } from '../../../../common/formatos';
import { StatusConta } from '../../../../common/status-conta/status-conta';
import { mascararCpf } from '../../../../common/validadores';
import { Cliente, FiltroCliente } from '../../model/cliente.model';
import { AcoesCliente } from '../../services/acoes-cliente.service';
import { ClienteService, filtrarClientes } from '../../services/cliente.service';

const FILTROS: { valor: FiltroCliente; nome: string }[] = [
  { valor: 'todos', nome: 'Todos' },
  { valor: 'ATIVA', nome: 'Ativas' },
  { valor: 'BLOQUEADA', nome: 'Bloqueadas' },
  { valor: 'ENCERRADA', nome: 'Encerradas' },
  { valor: 'primeiro-acesso', nome: '1º acesso pendente' },
];

/** Lista de clientes com busca, filtro por situação e ações de cada um. */
@Component({
  selector: 'app-lista-clientes',
  imports: [
    MatTableModule,
    MatPaginatorModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatProgressBarModule,
    CurrencyPipe,
    IniciaisPipe,
    CorAvatarPipe,
    StatusConta,
  ],
  templateUrl: './lista-clientes.html',
  styleUrl: './lista-clientes.css',
})
export class ListaClientes implements OnInit {
  private readonly service = inject(ClienteService);
  private readonly acoes = inject(AcoesCliente);
  private readonly router = inject(Router);

  /** Parâmetros da URL (?busca=, ?filtro=, ?novo=1). */
  readonly busca = input<string>();
  readonly filtro = input<string>();
  readonly novo = input<string>();

  protected readonly filtros = FILTROS;
  protected readonly colunas = ['cliente', 'cpf', 'conta', 'situacao', 'saldo', 'acoes'];
  protected readonly mascararCpf = mascararCpf;

  protected readonly clientes = rxResource({ stream: () => this.service.listar() });

  protected readonly termo = linkedSignal(() => this.busca() ?? '');
  protected readonly filtroAtual = linkedSignal<FiltroCliente>(() =>
    FILTROS.some((f) => f.valor === this.filtro()) ? (this.filtro() as FiltroCliente) : 'todos',
  );

  protected readonly filtrados = computed(() =>
    filtrarClientes(this.clientes.value() ?? [], this.termo(), this.filtroAtual()),
  );

  /** Quantos clientes em cada filtro (respeitando a busca). */
  protected readonly contagem = computed(() => {
    const todos = this.clientes.value() ?? [];
    const termo = this.termo();
    return Object.fromEntries(FILTROS.map((f) => [f.valor, filtrarClientes(todos, termo, f.valor).length]));
  });

  protected readonly tamanhoPagina = linkedSignal(() => 10);
  /** Volta para a primeira página sempre que a busca ou o filtro mudam. */
  protected readonly pagina = linkedSignal({ source: this.filtrados, computation: () => 0 });
  protected readonly visiveis = computed(() => {
    const inicio = this.pagina() * this.tamanhoPagina();
    return this.filtrados().slice(inicio, inicio + this.tamanhoPagina());
  });

  protected readonly erro = computed(() => (this.clientes.error() ? mensagemDeErro(this.clientes.error()) : ''));

  ngOnInit(): void {
    if (this.novo()) {
      // Veio do botão "Novo cliente" do painel: abre o cadastro e limpa a URL.
      this.router.navigate([], { queryParams: {}, replaceUrl: true });
      this.cadastrar();
    }
  }

  protected escolherFiltro(valor: FiltroCliente | undefined): void {
    this.filtroAtual.set(valor ?? 'todos');
  }

  protected mudarPagina(e: PageEvent): void {
    this.tamanhoPagina.set(e.pageSize);
    this.pagina.set(e.pageIndex);
  }

  protected cadastrar(): void {
    this.acoes.cadastrar().subscribe(() => this.clientes.reload());
  }

  protected editar(c: Cliente): void {
    this.acoes.editar(c).subscribe(() => this.clientes.reload());
  }

  protected novaSenha(c: Cliente): void {
    this.acoes.novaSenhaProvisoria(c).subscribe(() => this.clientes.reload());
  }

  protected verConta(c: Cliente): void {
    if (c.conta) {
      this.router.navigate(['/admin/contas', c.conta.id]);
    }
  }
}
