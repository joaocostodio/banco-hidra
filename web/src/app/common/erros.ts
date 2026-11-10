import { HttpErrorResponse } from '@angular/common/http';

/** Texto amigável para um erro da API (ela responde {"erro": "mensagem"}). */
export function mensagemDeErro(erro: unknown, padrao = 'Não foi possível concluir. Tente de novo.'): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Sem conexão com o servidor do banco. A API está rodando?';
    }
    if (typeof erro.error?.erro === 'string') {
      return erro.error.erro;
    }
    if (erro.status === 403) {
      return 'Seu perfil não tem permissão para isso.';
    }
    if (erro.status === 404) {
      return 'Não encontrado.';
    }
  }
  return padrao;
}
