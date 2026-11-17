import { StatusConta, TipoChavePix } from '../../contas/model/conta.model';

export type { ChavePix, Conta, Periodo, TipoChavePix, Transacao } from '../../contas/model/conta.model';

// ---------- Gestão financeira ----------

export type TipoConta = 'CORRENTE' | 'POUPANCA' | 'CARTEIRA' | 'INVESTIMENTO';

/** Uma das contas do cliente (GET /minha-conta/contas). */
export interface MinhaConta {
  id: number;
  nome: string;
  tipo: TipoConta;
  agencia: string;
  numero: string;
  saldo: number;
  status: StatusConta;
  /** A primeira conta: a das chaves Pix e dos boletos (não pode ser encerrada). */
  principal: boolean;
  abertaEm: string;
  /** Poupança e investimento: como rende e quando cai o próximo rendimento. */
  rendimento: { regra: string; proximo: string } | null;
  /** Só na poupança com meta. Progresso de 0 a 1. */
  meta: { descricao: string | null; valor: number; data: string | null; progresso: number } | null;
  /** Só no investimento. */
  investimento: {
    produto: ProdutoInvestimentoId;
    produtoNome: string;
    taxaAnual: number;
    isentoIR: boolean;
    /** Dinheiro que entrou (o resto do saldo é lucro). */
    aplicado: number;
    lucro: number;
    /** Alíquota de hoje pela tabela regressiva (0,225 = 22,5%). */
    aliquotaIR: number;
    irSeResgatarTudo: number;
  } | null;
}

export type ProdutoInvestimentoId = 'CDB' | 'TESOURO_SELIC' | 'LCI';

export interface ProdutoInvestimento {
  id: ProdutoInvestimentoId;
  nome: string;
  taxaAnual: number;
  isentoIR: boolean;
}

/** Abrir conta: sem nome, fica o do tipo; o investimento exige o produto. */
export interface NovaConta {
  nome: string;
  tipo: TipoConta;
  produto: ProdutoInvestimentoId | null;
}

export interface Meta {
  valor: number;
  descricao: string;
  /** AAAA-MM-DD ou null (sem prazo). */
  data: string | null;
}

export type TipoLancamento = 'RECEITA' | 'DESPESA';

export interface Lancamento {
  contaId: number;
  tipo: TipoLancamento;
  valor: number;
  categoriaId: number;
  descricao: string;
}

/** Categoria que o cliente enxerga: padrão do banco, de sistema (só para filtrar) ou própria. */
export interface CategoriaCliente {
  id: number;
  descricao: string;
  propria: boolean;
  sistema: boolean;
}

export interface TotalCategoria {
  categoriaId: number;
  categoria: string;
  total: number;
}

/** Tela inicial: saldo por conta e geral, receitas e despesas do mês. */
export interface ResumoFinanceiro {
  saldoTotal: number;
  contas: MinhaConta[];
  receitasMes: number;
  despesasMes: number;
  despesasPorCategoria: TotalCategoria[];
}

/** Filtros das transações (todos opcionais; datas AAAA-MM-DD). */
export interface FiltroTransacoes {
  contaId?: number | null;
  categoriaId?: number | null;
  tipo?: 'ENTRADA' | 'SAIDA' | null;
  inicio: string;
  fim: string;
}

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
  /** De qual das minhas contas sai (sem ela, a principal). */
  contaOrigemId?: number;
  agencia: string;
  numero: string;
  valor: number;
  descricao: string;
}
