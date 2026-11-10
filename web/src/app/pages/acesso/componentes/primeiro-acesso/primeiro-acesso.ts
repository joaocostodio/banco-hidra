import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { AuthService } from '../../../../common/auth/auth.service';
import { FormSenha } from '../form-senha/form-senha';
import { Vitrine } from '../vitrine/vitrine';

/** Quem entrou com a senha provisória só sai daqui depois de criar a própria senha. */
@Component({
  selector: 'app-primeiro-acesso',
  imports: [Vitrine, FormSenha, MatButtonModule],
  templateUrl: './primeiro-acesso.html',
  styleUrl: './primeiro-acesso.css',
})
export class PrimeiroAcesso {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected concluir(): void {
    this.router.navigateByUrl(this.auth.destinoInicial());
  }
}
