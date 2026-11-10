import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { HttpErrorResponse } from '@angular/common/http';
import { mensagemDeErro } from '../../../../common/erros';
import { cpfValidator, formatarCpf } from '../../../../common/validadores';
import { NovoClienteResposta } from '../../model/cliente.model';
import { ClienteService } from '../../services/cliente.service';

/** Cadastro de cliente: nome, CPF e e-mail. A conta é aberta junto. */
@Component({
  selector: 'app-novo-cliente',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './novo-cliente.html',
  styleUrl: './novo-cliente.css',
})
export class NovoClienteDialog {
  private readonly clientes = inject(ClienteService);
  private readonly ref = inject<MatDialogRef<NovoClienteDialog, NovoClienteResposta>>(MatDialogRef);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    cpf: ['', [Validators.required, cpfValidator]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');

  /** Formata o CPF enquanto digita (000.000.000-00). */
  protected formatarCpf(): void {
    const campo = this.form.controls.cpf;
    const formatado = formatarCpf(campo.value);
    if (formatado !== campo.value) {
      campo.setValue(formatado, { emitEvent: false });
    }
  }

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, cpf, email } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set('');
    this.clientes.cadastrar({ nome: nome.trim(), cpf, email: email.trim() }).subscribe({
      next: (resposta) => this.ref.close(resposta),
      error: (e) => {
        this.enviando.set(false);
        this.mostrarErro(e);
      },
    });
  }

  /** Erros de CPF/e-mail repetido aparecem embaixo do campo; o resto, no topo. */
  private mostrarErro(e: unknown): void {
    const msg = mensagemDeErro(e);
    if (e instanceof HttpErrorResponse && e.status === 422) {
      if (msg.includes('e-mail')) {
        this.form.controls.email.setErrors({ api: msg });
        return;
      }
      if (msg.includes('CPF')) {
        this.form.controls.cpf.setErrors({ api: msg });
        return;
      }
    }
    this.erro.set(msg);
  }
}
