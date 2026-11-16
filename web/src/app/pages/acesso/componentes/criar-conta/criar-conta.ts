import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { mensagemDeErro } from '../../../../common/erros';
import { RegrasSenha } from '../../../../common/regras-senha/regras-senha';
import {
  cpfValidator,
  formatarCpf,
  senhaForteValidator,
  senhasValidator,
  somenteDigitos,
} from '../../../../common/validadores';
import { Vitrine } from '../vitrine/vitrine';

/** "Criar conta": a pessoa se cadastra sozinha, ganha a conta corrente e já entra. */
@Component({
  selector: 'app-criar-conta',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    RegrasSenha,
    Vitrine,
  ],
  templateUrl: './criar-conta.html',
  styleUrl: '../entrar/entrar.css',
})
export class CriarConta {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      nome: ['', [Validators.required, Validators.maxLength(150)]],
      cpf: ['', [Validators.required, cpfValidator]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      novaSenha: ['', [Validators.required, senhaForteValidator]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasValidator },
  );

  protected readonly senha = toSignal(this.form.controls.novaSenha.valueChanges, { initialValue: '' });
  protected readonly enviando = signal(false);
  protected readonly erro = signal('');
  protected readonly mostrarSenha = signal(false);

  /** 000.000.000-00 enquanto digita. */
  protected formatarCpf(): void {
    const c = this.form.controls.cpf;
    const digitos = somenteDigitos(c.value).slice(0, 11);
    c.setValue(digitos.length === 11 ? formatarCpf(digitos) : digitos, { emitEvent: false });
  }

  protected criar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nome, cpf, email, novaSenha } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set('');
    this.auth.cadastrar({ nome: nome.trim(), cpf, email: email.trim(), senha: novaSenha }).subscribe({
      next: () => this.router.navigateByUrl('/app'),
      error: (e) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível criar a conta.'));
        this.enviando.set(false);
      },
    });
  }
}
