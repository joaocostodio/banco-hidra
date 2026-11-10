import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Perfil } from './sessao.model';

/** Só entra quem está logado, já trocou a senha provisória e tem o perfil certo. */
export function exigePerfil(perfil: Perfil): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (auth.estaLogado() && !auth.precisaTrocarSenha() && auth.usuario()?.perfil === perfil) {
      return true;
    }
    return router.parseUrl(auth.destinoInicial());
  };
}

/** Tela de troca obrigatória: só para quem entrou com a senha provisória. */
export const primeiroAcessoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.estaLogado() && auth.precisaTrocarSenha()
    ? true
    : inject(Router).parseUrl(auth.destinoInicial());
};

/** Tela de login: quem já está logado vai direto para a sua área. */
export const visitanteGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.estaLogado() ? inject(Router).parseUrl(auth.destinoInicial()) : true;
};

/** Raiz do site: manda cada um para o lugar certo. */
export const inicioGuard: CanActivateFn = () =>
  inject(Router).parseUrl(inject(AuthService).destinoInicial());
