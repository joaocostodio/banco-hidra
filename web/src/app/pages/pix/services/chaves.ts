import { Pipe, PipeTransform } from '@angular/core';
import { formatarCpf, somenteDigitos } from '../../../common/validadores';
import { TipoChavePix } from '../../contas/model/conta.model';

export const NOME_TIPO_CHAVE: Record<TipoChavePix, string> = {
  CPF: 'CPF',
  EMAIL: 'E-mail',
  TELEFONE: 'Celular',
  ALEATORIA: 'Chave aleatória',
};

export const ICONE_TIPO_CHAVE: Record<TipoChavePix, string> = {
  CPF: 'badge',
  EMAIL: 'mail',
  TELEFONE: 'smartphone',
  ALEATORIA: 'key',
};

/** Limite de chaves por conta (mesma regra da API). */
export const LIMITE_CHAVES = 5;

/** (55) 99999-0000 enquanto digita. */
export function formatarCelular(texto: string): string {
  const d = somenteDigitos(texto).slice(0, 11);
  if (d.length <= 2) {
    return d.length ? `(${d}` : '';
  }
  if (d.length <= 7) {
    return `(${d.slice(0, 2)}) ${d.slice(2)}`;
  }
  return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
}

/** Como a chave aparece na tela: CPF com pontos, celular com DDD, o resto como está. */
export function chaveLegivel(tipo: TipoChavePix, valor: string): string {
  if (tipo === 'CPF') {
    return formatarCpf(valor);
  }
  if (tipo === 'TELEFONE') {
    const d = somenteDigitos(valor);
    return formatarCelular(d.length === 13 ? d.slice(2) : d);
  }
  return valor;
}

@Pipe({ name: 'chaveLegivel' })
export class ChaveLegivelPipe implements PipeTransform {
  transform(valor: string, tipo: TipoChavePix): string {
    return chaveLegivel(tipo, valor);
  }
}
