import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { mensagemDeErro } from '../../../../common/erros';
import { Vitrine } from '../vitrine/vitrine';

/** Login único: ADM e cliente entram aqui e cada um vai para a sua área. */
@Component({
  selector: 'app-entrar',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    Vitrine,
  ],
  templateUrl: './entrar.html',
  styleUrl: './entrar.css',
})
export class Entrar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** ?motivo=expirou quando a sessão caiu. */
  readonly motivo = input<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    // E-mail (cliente) ou nome de usuário (administrador, ex.: "joao"): não exige formato de e-mail.
    email: ['', Validators.required],
    senha: ['', Validators.required],
  });

  protected readonly enviando = signal(false);
  protected readonly erro = signal('');
  protected readonly mostrarSenha = signal(false);

  protected entrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, senha } = this.form.getRawValue();
    this.enviando.set(true);
    this.erro.set('');
    this.auth.entrar(email.trim(), senha).subscribe({
      next: () => this.router.navigateByUrl(this.auth.destinoInicial()),
      error: (e) => {
        this.erro.set(mensagemDeErro(e, 'Não foi possível entrar.'));
        this.enviando.set(false);
        this.form.controls.senha.reset();
      },
    });
  }
}
