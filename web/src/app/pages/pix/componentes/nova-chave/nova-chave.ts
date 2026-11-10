import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { mensagemDeErro } from '../../../../common/erros';
import { somenteDigitos } from '../../../../common/validadores';
import { ChavePix, MeusDados, TipoChavePix } from '../../../minha-conta/model/minha-conta.model';
import { MinhaContaService } from '../../../minha-conta/services/minha-conta.service';
import { ICONE_TIPO_CHAVE, NOME_TIPO_CHAVE, formatarCelular } from '../../services/chaves';

export interface DadosNovaChave {
  dados: MeusDados;
  existentes: ChavePix[];
}

/** Cadastro de chave Pix: CPF e e-mail já vêm prontos; celular é digitado; aleatória o banco gera. */
@Component({
  selector: 'app-nova-chave',
  imports: [ReactiveFormsModule, MatDialogModule, MatRadioModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule],
  templateUrl: './nova-chave.html',
  styleUrl: './nova-chave.css',
})
export class NovaChaveDialog {
  protected readonly entrada = inject<DadosNovaChave>(MAT_DIALOG_DATA);
  private readonly minhaConta = inject(MinhaContaService);
  private readonly ref = inject<MatDialogRef<NovaChaveDialog, ChavePix>>(MatDialogRef);

  protected readonly tipos: TipoChavePix[] = ['CPF', 'EMAIL', 'TELEFONE', 'ALEATORIA'];
  protected readonly nomes = NOME_TIPO_CHAVE;
  protected readonly icones = ICONE_TIPO_CHAVE;

  /** Só pode ter uma chave CPF (a do titular). */
  protected readonly jaTemCpf = this.entrada.existentes.some((c) => c.tipo === 'CPF');

  protected readonly form = inject(NonNullableFormBuilder).group({
    tipo: [(this.jaTemCpf ? 'EMAIL' : 'CPF') as TipoChavePix],
    email: [this.entrada.dados.email, [Validators.required, Validators.email]],
    celular: ['', [Validators.required, Validators.pattern(/^\(\d{2}\) 9\d{4}-\d{4}$/)]],
  });
  protected readonly tipo = toSignal(this.form.controls.tipo.valueChanges, { initialValue: this.form.controls.tipo.value });

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');

  protected readonly descricao = computed(() => {
    switch (this.tipo()) {
      case 'CPF':
        return `Seu CPF ${this.entrada.dados.cpf ?? ''} vira a sua chave.`;
      case 'EMAIL':
        return 'Quem tiver este e-mail consegue te mandar um Pix.';
      case 'TELEFONE':
        return 'Celular com DDD. Fácil de passar para os amigos.';
      default:
        return 'O banco gera um código aleatório. Bom para não expor seus dados.';
    }
  });

  protected formatarCelular(): void {
    const c = this.form.controls.celular;
    c.setValue(formatarCelular(c.value), { emitEvent: false });
  }

  protected salvar(): void {
    const tipo = this.form.controls.tipo.value;
    const campo = tipo === 'EMAIL' ? this.form.controls.email : tipo === 'TELEFONE' ? this.form.controls.celular : null;
    if (campo?.invalid) {
      campo.markAsTouched();
      return;
    }
    const valor = tipo === 'EMAIL' ? campo!.value.trim() : tipo === 'TELEFONE' ? somenteDigitos(campo!.value) : '';
    this.enviando.set(true);
    this.erro.set('');
    this.minhaConta.cadastrarChave(tipo, valor).subscribe({
      next: (chave) => this.ref.close(chave),
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
