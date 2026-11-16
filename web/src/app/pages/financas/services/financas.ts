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

export const TIPOS_CONTA = Object.keys(NOME_TIPO_CONTA) as TipoConta[];

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
