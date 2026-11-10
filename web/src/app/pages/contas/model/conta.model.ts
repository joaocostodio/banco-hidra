export type StatusConta = 'ATIVA' | 'BLOQUEADA' | 'ENCERRADA';

export interface Conta {
  id: number;
  agencia: string;
  numero: string;
  titularId: number;
  titularNome: string;
  saldo: number;
  status: StatusConta;
  abertaEm: string;
}

/** Uma linha do extrato. */
export interface Transacao {
  id: number;
  contaId: number;
  contaNumero: string;
  tipo: 'ENTRADA' | 'SAIDA';
  valor: number;
  saldoApos: number;
  descricao: string;
  categoria: string;
  contraparteNome: string | null;
  contraparteConta: string | null;
  autenticacao: string;
  dataHora: string;
}

export type TipoChavePix = 'CPF' | 'EMAIL' | 'TELEFONE' | 'ALEATORIA';

export interface ChavePix {
  id: number;
  tipo: TipoChavePix;
  valor: string;
  criadaEm: string;
}

export type { Comprovante, ParteComprovante } from '../../../common/comprovante/comprovante.model';

export interface Deposito {
  valor: number;
  descricao: string;
}

/** Período do extrato (datas AAAA-MM-DD). */
export interface Periodo {
  inicio: string;
  fim: string;
}
