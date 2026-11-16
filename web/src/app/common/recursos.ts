import { Resource } from '@angular/core';

/**
 * Valor do recurso, ou {@code padrao} enquanto carrega ou se deu erro.
 * No Angular, ler {@code value()} de um recurso com erro lança exceção (e quebra a tela inteira);
 * com isto a tela continua de pé e mostra a mensagem de erro.
 */
export function valorOu<T, P>(recurso: Resource<T | undefined>, padrao: P): T | P {
  return recurso.hasValue() ? (recurso.value() as T) : padrao;
}
