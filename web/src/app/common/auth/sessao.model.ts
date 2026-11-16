export type Perfil = 'ADMIN' | 'CLIENTE';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
}

/**
 * Resposta do login, do cadastro e da troca de senha. O token JWT NÃO vem aqui: ele fica num cookie
 * httpOnly, que o navegador manda sozinho e que o JavaScript não consegue ler.
 */
export interface LoginResposta {
  /** Quando a sessão acaba (para o site saber, sem precisar ler o token). */
  expiraEm: string;
  /** true: a sessão só serve para trocar a senha (primeiro acesso). */
  trocarSenha: boolean;
  usuario: Usuario;
}

/** "Criar conta". */
export interface Cadastro {
  nome: string;
  cpf: string;
  email: string;
  senha: string;
}
