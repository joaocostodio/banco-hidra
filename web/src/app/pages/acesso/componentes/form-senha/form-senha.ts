import { Component, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { ErrorStateMatcher } from '@angular/material/core';
import { AbstractControl, FormGroupDirective, NgForm } from '@angular/forms';
import { AuthService } from '../../../../common/auth/auth.service';
import { mensagemDeErro } from '../../../../common/erros';
import { RegrasSenha } from '../../../../common/regras-senha/regras-senha';
import { senhaForteValidator, senhasValidator } from '../../../../common/validadores';

/** Mostra o erro da confirmação também quando o problema está no grupo (senhas diferentes). */
class ConfirmacaoMatcher implements ErrorStateMatcher {
  isErrorState(c: AbstractControl | null, f: FormGroupDirective | NgForm | null): boolean {
    const tocado = !!c && (c.touched || !!f?.submitted);
    return tocado && (!!c?.invalid || !!c?.parent?.hasError('naoConferem'));
  }
}

/** Formulário de troca de senha (primeiro acesso e "trocar minha senha"). */
@Component({
  selector: 'app-form-senha',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, RegrasSenha],
  templateUrl: './form-senha.html',
  styleUrl: './form-senha.css',
})
export class FormSenha {
  private readonly auth = inject(AuthService);

  readonly rotuloAtual = input('Senha atual');
  readonly textoBotao = input('Salvar');
  readonly salvo = output<void>();

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, senhaForteValidator]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasValidator },
  );

  protected readonly novaSenha = toSignal(this.form.controls.novaSenha.valueChanges, { initialValue: '' });
  protected readonly senhaAtual = toSignal(this.form.controls.senhaAtual.valueChanges, { initialValue: '' });
  protected readonly confirmacaoMatcher = new ConfirmacaoMatcher();

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');
  protected readonly mostrar = signal(false);

  protected salvar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { senhaAtual, novaSenha } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set('');
    this.auth.trocarSenha(senhaAtual, novaSenha).subscribe({
      next: () => {
        this.enviando.set(false);
        this.salvo.emit();
      },
      error: (e) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
