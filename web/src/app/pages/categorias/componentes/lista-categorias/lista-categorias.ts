import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter, switchMap } from 'rxjs';
import { Confirmacao } from '../../../../common/confirmacao/confirmacao';
import { mensagemDeErro } from '../../../../common/erros';
import { IconeCategoriaPipe } from '../../../../common/formatos';
import { Categoria } from '../../model/categoria.model';
import { CategoriaService, podeExcluir } from '../../services/categoria.service';
import { CategoriaFormDialog } from '../categoria-form/categoria-form';

/** Categorias de transação: as do sistema são fixas; as do ADM podem ser renomeadas e excluídas (se não usadas). */
@Component({
  selector: 'app-lista-categorias',
  imports: [MatButtonModule, MatIconModule, MatTooltipModule, MatProgressBarModule, DecimalPipe, IconeCategoriaPipe],
  templateUrl: './lista-categorias.html',
  styleUrl: './lista-categorias.css',
})
export class ListaCategorias {
  private readonly service = inject(CategoriaService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);

  protected readonly categorias = rxResource({ stream: () => this.service.listar() });

  /** As do sistema primeiro, depois as criadas pelo ADM. */
  protected readonly ordenadas = computed(() =>
    [...(this.categorias.value() ?? [])].sort((a, b) => Number(b.sistema) - Number(a.sistema) || a.descricao.localeCompare(b.descricao)),
  );

  protected readonly podeExcluir = podeExcluir;
  protected readonly erro = computed(() => (this.categorias.error() ? mensagemDeErro(this.categorias.error()) : ''));

  protected abrirForm(categoria: Categoria | null): void {
    this.dialog
      .open(CategoriaFormDialog, { data: categoria })
      .afterClosed()
      .pipe(filter(Boolean))
      .subscribe(() => {
        this.snack.open(categoria ? 'Categoria renomeada.' : 'Categoria criada.', 'OK', { duration: 3000 });
        this.categorias.reload();
      });
  }

  protected excluir(c: Categoria): void {
    this.dialog
      .open(Confirmacao, {
        data: {
          titulo: 'Excluir categoria?',
          mensagem: `A categoria "${c.descricao}" será excluída.`,
          confirmar: 'Excluir',
          icone: 'delete',
          perigo: true,
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => this.service.excluir(c.id)),
      )
      .subscribe({
        next: () => {
          this.snack.open('Categoria excluída.', 'OK', { duration: 3000 });
          this.categorias.reload();
        },
        error: (e) => this.snack.open(mensagemDeErro(e), 'OK', { duration: 6000 }),
      });
  }
}
