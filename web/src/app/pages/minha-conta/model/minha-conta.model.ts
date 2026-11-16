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

/** Cobrança por QR code: `codigo` é o "Pix copia e cola". */
export interface CobrancaPix {
  codigo: string;
  chave: string;
  tipoChave: TipoChavePix;
  valor: number | null;
  descricao: string | null;
}

/** Código colado pelo pagador, já conferido pela API. Sem valor: o pagador digita. */
export interface CodigoPixLido {
  chave: string;
  valor: number | null;
  descricao: string | null;
  destinatario: Destinatario;
}

export type StatusBoleto = 'ABERTO' | 'PAGO' | 'CANCELADO';

/** Boleto emitido por mim. */
export interface Boleto {
  id: number;
  /** Já formatada: 99990.00012 34567.890123 ... */
  linhaDigitavel: string;
  codigoBarras: string;
  valor: number;
  vencimento: string;
  descricao: string | null;
  status: StatusBoleto;
  vencido: boolean;
  emitidoEm: string;
  beneficiarioNome: string;
  agencia: string;
  conta: string;
  pagadorNome: string | null;
  pagoEm: string | null;
  valorPago: number | null;
  autenticacao: string | null;
}

export interface EmissaoBoleto {
  valor: number;
  vencimento: string;
  descricao: string;
}

/** "Confira antes de pagar": o total já vem com multa e juros do dia. */
export interface ConsultaBoleto {
  linhaDigitavel: string;
  beneficiario: Destinatario;
  descricao: string | null;
  vencimento: string;
  valor: number;
  diasAtraso: number;
  multa: number;
  juros: number;
  total: number;
  status: StatusBoleto;
}

export interface Transferencia {
  agencia: string;
  numero: string;
  valor: number;
  descricao: string;
}
