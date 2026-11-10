import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { API } from '../config';
import { AuthService } from './auth.service';

/**
 * Coloca o token em toda chamada à API. Se a API responder 401 com um token
 * (venceu, ou a API reiniciou e trocou as chaves), encerra a sessão.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = req.url.startsWith(API) ? auth.token() : null;
  const pedido = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(pedido).pipe(
    catchError((erro: unknown) => {
      if (token && erro instanceof HttpErrorResponse && erro.status === 401) {
        auth.sair('expirou');
      }
      return throwError(() => erro);
    }),
  );
};
