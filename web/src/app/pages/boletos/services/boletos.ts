import { Pipe, PipeTransform } from '@angular/core';
import { Boleto } from '../../minha-conta/model/minha-conta.model';

/** Como o boleto aparece na lista: "Pago", "Vencido"... e a cor do selo. */
export function situacao(b: Boleto): { texto: string; classe: string } {
  if (b.status === 'PAGO') {
    return { texto: 'Pago', classe: 'pago' };
  }
  if (b.status === 'CANCELADO') {
    return { texto: 'Cancelado', classe: 'cancelado' };
  }
  return b.vencido ? { texto: 'Vencido', classe: 'vencido' } : { texto: 'Aberto', classe: 'aberto' };
}

@Pipe({ name: 'situacaoBoleto' })
export class SituacaoBoletoPipe implements PipeTransform {
  transform(b: Boleto): { texto: string; classe: string } {
    return situacao(b);
  }
}

/** Regras de atraso (mesmas da API), para avisar quem emite. */
export const REGRA_ATRASO = 'Depois do vencimento: multa de 2% e juros de 1% ao mês (por dia de atraso).';
