import { StatusConta } from '../../contas/model/conta.model';

export interface ContaResumo {
  id: number;
  agencia: string;
  numero: string;
  status: StatusConta;
  saldo: number;
}

export interface Cliente {
  id: number;
  nome: string;
  /** Formatado (000.000.000-00). */
  cpf: string | null;
  email: string;
  criadoEm: string;
  /** Ainda não trocou a senha provisória. */
  primeiroAcesso: boolean;
  conta: ContaResumo | null;
}

export interface NovoCliente {
  nome: string;
  cpf: string;
  email: string;
}

export interface EdicaoCliente {
  nome: string;
  email: string;
}

export interface NovoClienteResposta {
  cliente: Cliente;
  senhaProvisoria: string;
}

/** Filtros da lista de clientes. */
export type FiltroCliente = 'todos' | 'ATIVA' | 'BLOQUEADA' | 'ENCERRADA' | 'primeiro-acesso';
