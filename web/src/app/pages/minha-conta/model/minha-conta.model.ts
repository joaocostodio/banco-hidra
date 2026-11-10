import { StatusConta, TipoChavePix } from '../../contas/model/conta.model';

export type { ChavePix, Conta, Periodo, TipoChavePix, Transacao } from '../../contas/model/conta.model';

/** GET /minha-conta/dados */
export interface MeusDados {
  nome: string;
  email: string;
  /** Completo (000.000.000-00): só o próprio dono vê. */
  cpf: string | null;
  agencia: string;
  numero: string;
  status: StatusConta;
  clienteDesde: string;
}

/** Quem vai receber, antes de confirmar (nome e CPF mascarado). */
export interface Destinatario {
  nome: string;
  cpfMascarado: string;
  banco: string;
  agencia: string;
  conta: string;
  /** Preenchido quando a busca foi por chave Pix. */
  tipoChave: TipoChavePix | null;
}

export interface EnvioPix {
  chave: string;
  valor: number;
  descricao: string;
}

export interface Transferencia {
  agencia: string;
  numero: string;
  valor: number;
  descricao: string;
}
