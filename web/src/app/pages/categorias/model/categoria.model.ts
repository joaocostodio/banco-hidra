export interface Categoria {
  id: number;
  descricao: string;
  /** Usada pelo próprio banco (Depósito, Pix...): não pode ser alterada nem excluída. */
  sistema: boolean;
  /** Quantas linhas de extrato usam a categoria. */
  usos: number;
}
