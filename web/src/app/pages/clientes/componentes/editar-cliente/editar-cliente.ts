import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { mensagemDeErro } from '../../../../common/erros';
import { Cliente } from '../../model/cliente.model';
import { ClienteService } from '../../services/cliente.service';

/** Edita nome e e-mail. O CPF não muda (é a identidade do cliente). */
@Component({
  selector: 'app-editar-cliente',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './editar-cliente.html',
})
export class EditarClienteDialog {
  protected readonly cliente = inject<Cliente>(MAT_DIALOG_DATA);
  private readonly clientes = inject(ClienteService);
  private readonly ref = inject<MatDialogRef<EditarClienteDialog, Cliente>>(MatDialogRef);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: [this.cliente.nome, [Validators.required, Validators.maxLength(150)]],
    email: [this.cliente.email, [Validators.required, Validators.email, Validators.maxLength(150)]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, email } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set('');
    this.clientes.editar(this.cliente.id, { nome: nome.trim(), email: email.trim() }).subscribe({
      next: (atualizado) => this.ref.close(atualizado),
      error: (e) => {
        this.enviando.set(false);
        const msg = mensagemDeErro(e);
        if (e instanceof HttpErrorResponse && e.status === 422 && msg.includes('e-mail')) {
          this.form.controls.email.setErrors({ api: msg });
        } else {
          this.erro.set(msg);
        }
      },
    });
  }
}
