import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TrocarSenha } from '../../pages/acesso/componentes/trocar-senha/trocar-senha';
import { AuthService } from '../auth/auth.service';
import { IniciaisPipe } from '../formatos';
import { Icone } from '../icone/icone';
import { Marca } from '../marca/marca';
import { Rodape } from '../rodape/rodape';

interface Aba {
  rota: string;
  icone: string;
  nome: string;
  exato?: boolean;
}

/** Moldura do app do cliente: topo com a marca, abas (embaixo no celular) e rodapé. */
@Component({
  selector: 'app-layout-cliente',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatMenuModule, Marca, Rodape, IniciaisPipe, Icone],
  templateUrl: './layout-cliente.html',
  styleUrl: './layout-cliente.css',
})
export class LayoutCliente {
  protected readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  protected readonly abas: Aba[] = [
    { rota: '/app', icone: 'home', nome: 'Início', exato: true },
    { rota: '/app/extrato', icone: 'receipt_long', nome: 'Extrato' },
    { rota: '/app/pix', icone: 'pix', nome: 'Pix' },
    { rota: '/app/transferir', icone: 'swap_horiz', nome: 'Transferir' },
    { rota: '/app/perfil', icone: 'person', nome: 'Perfil' },
  ];

  protected trocarSenha(): void {
    this.dialog.open(TrocarSenha);
  }
}
