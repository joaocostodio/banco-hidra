export interface DiaMovimento {
  data: string;
  operacoes: number;
  valor: number;
}

/** GET /painel */
export interface ResumoPainel {
  totalCustodiado: number;
  clientes: number;
  clientesNoMes: number;
  primeiroAcessoPendente: number;
  contasAtivas: number;
  contasBloqueadas: number;
  contasEncerradas: number;
  operacoesHoje: number;
  valorHoje: number;
  ultimosDias: DiaMovimento[];
}
