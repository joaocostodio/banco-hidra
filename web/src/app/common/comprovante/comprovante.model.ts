export interface ParteComprovante {
  nome: string;
  agencia: string;
  conta: string;
}

/** Comprovante de uma operação (as duas pontas: quem pagou e quem recebeu). */
export interface Comprovante {
  autenticacao: string;
  categoria: string;
  valor: number;
  descricao: string;
  dataHora: string;
  origem: ParteComprovante | null;
  destino: ParteComprovante | null;
}
