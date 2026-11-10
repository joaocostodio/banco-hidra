import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface DadosConfirmacao {
  titulo: string;
  mensagem: string;
  confirmar: string;
  icone?: string;
  /** Ação que não tem volta (botão em vermelho). */
  perigo?: boolean;
}

/** Diálogo "tem certeza?". Fecha com true quando a pessoa confirma. */
@Component({
  selector: 'app-confirmacao',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './confirmacao.html',
  styleUrl: './confirmacao.css',
})
export class Confirmacao {
  protected readonly dados = inject<DadosConfirmacao>(MAT_DIALOG_DATA);
}
