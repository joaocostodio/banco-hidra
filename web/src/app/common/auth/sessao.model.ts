export type Perfil = 'ADMIN' | 'CLIENTE';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
}

/** Resposta do POST /auth/login e do PUT /auth/senha. */
export interface LoginResposta {
  token: string;
  expiraEm: string;
  /** true: o token só serve para trocar a senha (primeiro acesso). */
  trocarSenha: boolean;
  usuario: Usuario;
}
