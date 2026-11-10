import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormSenha } from '../form-senha/form-senha';

/** "Trocar minha senha", aberto pelo menu do usuário. */
@Component({
  selector: 'app-trocar-senha',
  imports: [MatDialogModule, MatButtonModule, FormSenha],
  templateUrl: './trocar-senha.html',
})
export class TrocarSenha {
  private readonly ref = inject<MatDialogRef<TrocarSenha>>(MatDialogRef);
  private readonly snack = inject(MatSnackBar);

  protected concluir(): void {
    this.snack.open('Senha alterada.', 'OK', { duration: 4000 });
    this.ref.close(true);
  }
}
