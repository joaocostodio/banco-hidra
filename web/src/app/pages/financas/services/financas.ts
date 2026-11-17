import { Pipe, PipeTransform } from '@angular/core';
import { TipoConta } from '../../minha-conta/model/minha-conta.model';

export const NOME_TIPO_CONTA: Record<TipoConta, string> = {
  CORRENTE: 'Conta corrente',
  POUPANCA: 'Poupança',
  CARTEIRA: 'Carteira',
  INVESTIMENTO: 'Investimento',
};

export const ICONE_TIPO_CONTA: Record<TipoConta, string> = {
  CORRENTE: 'account_balance',
  POUPANCA: 'savings',
  CARTEIRA: 'account_balance_wallet',
  INVESTIMENTO: 'trending_up',
};

export const DESCRICAO_TIPO_CONTA: Record<TipoConta, string> = {
  CORRENTE: 'Para o dia a dia. Não rende.',
  POUPANCA: 'Rende 0,5% ao mês e pode ter uma meta de economia.',
  CARTEIRA: 'O dinheiro em espécie. Não rende.',
  INVESTIMENTO: 'CDB, Tesouro ou LCI: rende todo dia; IR no resgate.',
};

/** O que dá para abrir pela tela "Nova conta" (todos os tipos). */
export const TIPOS_NOVA_CONTA: TipoConta[] = ['POUPANCA', 'INVESTIMENTO', 'CARTEIRA', 'CORRENTE'];

/** Mesmo limite da API (Conta.LIMITE_POR_CLIENTE). */
export const LIMITE_CONTAS = 10;

@Pipe({ name: 'iconeConta' })
export class IconeContaPipe implements PipeTransform {
  transform(tipo: TipoConta): string {
    return ICONE_TIPO_CONTA[tipo];
  }
}

@Pipe({ name: 'nomeTipoConta' })
export class NomeTipoContaPipe implements PipeTransform {
  transform(tipo: TipoConta): string {
    return NOME_TIPO_CONTA[tipo];
  }
}
