import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormGroupDirective, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { Confirmacao } from '../../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../../common/erros';
import { valorOu } from '../../../../common/recursos';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { Icone } from '../../../../common/icone/icone';
import { CategoriaCliente } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';

/** Categorias: as padrão do banco (só leitura) e as minhas (criar, renomear, excluir). */
@Component({
  selector: 'app-categorias',
  imports: [RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatProgressBarModule, MatTooltipModule, IconeCategoriaPipe, Icone],
  templateUrl: './categorias.html',
  styleUrl: './categorias.css',
})
export class Categorias {
  private readonly minhaConta = inject(MinhaContaService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly categorias = rxResource({ stream: () => this.minhaConta.categorias() });
  protected readonly minhas = computed(() => valorOu(this.categorias, []).filter((c) => c.propria));
  /** As de sistema (Pix, Transferência...) ficam de fora: o banco usa, mas não servem para lançar. */
  protected readonly padrao = computed(() => valorOu(this.categorias, []).filter((c) => !c.propria && !c.sistema));
  protected readonly erro = computed(() => (this.categorias.error() ? mensagemDeErro(this.categorias.error()) : ''));

  protected readonly form = this.fb.group({ nova: ['', [Validators.required, Validators.maxLength(60)]] });
  protected readonly nova = this.form.controls.nova;
  private readonly formulario = viewChild(FormGroupDirective);
  /** Categoria sendo renomeada (o campo aparece no lugar do nome). */
  protected readonly editando = signal<number | null>(null);
  protected readonly nomeEditado = this.fb.control('', [Validators.required, Validators.maxLength(60)]);
  protected readonly salvando = signal(false);

  protected criar(): void {
    if (this.nova.invalid) {
      this.nova.markAsTouched();
      return;
    }
    this.salvando.set(true);
    this.minhaConta.criarCategoria(this.nova.value.trim()).subscribe({
      next: (c) => {
        this.salvando.set(false);
        this.formulario()?.resetForm(); // limpa também o "já enviado" (senão o campo vazio fica vermelho)
        this.snack.open(`Categoria ${c.descricao} criada.`, 'OK', { duration: 2500 });
        this.categorias.reload();
      },
      error: (e) => {
        this.salvando.set(false);
        this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 });
      },
    });
  }

  protected editar(c: CategoriaCliente): void {
    this.editando.set(c.id);
    this.nomeEditado.setValue(c.descricao);
  }

  protected salvarNome(c: CategoriaCliente): void {
    const nome = this.nomeEditado.value.trim();
    if (this.nomeEditado.invalid || nome === c.descricao) {
      this.editando.set(null);
      return;
    }
    this.minhaConta.renomearCategoria(c.id, nome).subscribe({
      next: () => {
        this.editando.set(null);
        this.categorias.reload();
      },
      error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 }),
    });
  }

  protected excluir(c: CategoriaCliente): void {
    this.dialog
      .open(Confirmacao, {
        data: {
          titulo: `Excluir ${c.descricao}?`,
          mensagem: 'Só dá para excluir uma categoria que nenhuma transação usa.',
          confirmar: 'Excluir',
          icone: 'delete',
          perigo: true,
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.minhaConta.excluirCategoria(c.id)),
      )
      .subscribe({
        next: () => {
          this.snack.open('Categoria excluída.', 'OK', { duration: 2500 });
          this.categorias.reload();
        },
        error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 5000 }),
      });
  }
}
