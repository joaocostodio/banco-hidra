import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API } from '../config';
import { AuthService } from './auth.service';

/**
 * Toda chamada à API:
 * - leva o cookie de sessão (o navegador manda sozinho: é o mesmo endereço do site);
 * - leva o cabeçalho X-Requested-With, que a API exige para qualquer mudança (proteção contra CSRF:
 *   um formulário ou imagem de outro site não consegue mandar esse cabeçalho).
 * Se a API responder 401 com alguém logado (a sessão venceu, ou a API reiniciou), encerra a sessão.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(API)) {
    return next(req);
  }
  const auth = inject(AuthService);
  const pedido = req.clone({ setHeaders: { 'X-Requested-With': 'XMLHttpRequest' } });

  return next(pedido).pipe(
    catchError((erro: unknown) => {
      const ehLogin = req.url.startsWith(`${API}/auth/`);
      if (!ehLogin && erro instanceof HttpErrorResponse && erro.status === 401 && auth.estaLogado()) {
        auth.sair('expirou');
      }
      return throwError(() => erro);
    }),
  );
};
