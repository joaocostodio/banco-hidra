import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { HttpErrorResponse } from '@angular/common/http';
import { mensagemDeErro } from '../../../../common/erros';
import { Categoria } from '../../model/categoria.model';
import { CategoriaService } from '../../services/categoria.service';

/** Criar ou renomear categoria. Recebe a categoria para editar, ou null para criar. */
@Component({
  selector: 'app-categoria-form',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './categoria-form.html',
})
export class CategoriaFormDialog {
  protected readonly categoria = inject<Categoria | null>(MAT_DIALOG_DATA);
  private readonly service = inject(CategoriaService);
  private readonly ref = inject<MatDialogRef<CategoriaFormDialog, Categoria>>(MatDialogRef);

  protected readonly form = inject(NonNullableFormBuilder).group({
    descricao: [this.categoria?.descricao ?? '', [Validators.required, Validators.maxLength(150)]],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const descricao = this.form.getRawValue().descricao.trim();
    const pedido = this.categoria
      ? this.service.editar(this.categoria.id, descricao)
      : this.service.criar(descricao);
    this.enviando.set(true);
    this.erro.set('');
    pedido.subscribe({
      next: (c) => this.ref.close(c),
      error: (e) => {
        this.enviando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 422) {
          this.form.controls.descricao.setErrors({ api: mensagemDeErro(e) });
        } else {
          this.erro.set(mensagemDeErro(e));
        }
      },
    });
  }
}
