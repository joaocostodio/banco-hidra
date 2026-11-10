import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { TrocarSenha } from '../../pages/acesso/componentes/trocar-senha/trocar-senha';
import { AuthService } from '../auth/auth.service';
import { IniciaisPipe } from '../formatos';
import { Marca } from '../marca/marca';
import { Rodape } from '../rodape/rodape';
import { Titulo } from '../titulo';

interface ItemMenu {
  rota: string;
  icone: string;
  nome: string;
  exato?: boolean;
}

/** Moldura do painel ADM: menu lateral, topo com busca e usuário, rodapé. */
@Component({
  selector: 'app-layout-admin',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ReactiveFormsModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatFormFieldModule,
    MatInputModule,
    Marca,
    Rodape,
    IniciaisPipe,
  ],
  templateUrl: './layout-admin.html',
  styleUrl: './layout-admin.css',
  host: { '(document:keydown)': 'atalho($event)' },
})
export class LayoutAdmin {
  protected readonly auth = inject(AuthService);
  protected readonly titulo = inject(Titulo);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  protected readonly itens: ItemMenu[] = [
    { rota: '/admin', icone: 'space_dashboard', nome: 'Painel', exato: true },
    { rota: '/admin/clientes', icone: 'group', nome: 'Clientes' },
    { rota: '/admin/contas', icone: 'credit_card', nome: 'Contas' },
    { rota: '/admin/movimentacoes', icone: 'sync_alt', nome: 'Movimentações' },
    { rota: '/admin/categorias', icone: 'sell', nome: 'Categorias' },
  ];

  protected readonly celular = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 900px)')
      .pipe(map((r) => r.matches)),
    { initialValue: false },
  );
  protected readonly menuAberto = signal(false);
  protected readonly menuVisivel = computed(() => !this.celular() || this.menuAberto());

  protected readonly busca = new FormControl('', { nonNullable: true });
  private readonly campoBusca = viewChild<ElementRef<HTMLInputElement>>('campoBusca');

  protected buscar(): void {
    const termo = this.busca.value.trim();
    this.router.navigate(['/admin/clientes'], { queryParams: termo ? { busca: termo } : {} });
    this.busca.reset();
    this.campoBusca()?.nativeElement.blur();
  }

  /** Ctrl+K (ou Cmd+K) leva para a busca. */
  protected atalho(evento: KeyboardEvent): void {
    if ((evento.ctrlKey || evento.metaKey) && evento.key.toLowerCase() === 'k') {
      evento.preventDefault();
      this.campoBusca()?.nativeElement.focus();
    }
  }

  protected fecharMenuNoCelular(): void {
    if (this.celular()) {
      this.menuAberto.set(false);
    }
  }

  protected trocarSenha(): void {
    this.dialog.open(TrocarSenha, { autoFocus: 'first-tabbable' });
  }
}
