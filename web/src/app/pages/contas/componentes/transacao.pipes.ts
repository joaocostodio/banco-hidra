import { Pipe, PipeTransform } from '@angular/core';
import { Transacao } from '../model/conta.model';

/** "Pix recebido", "Transferência enviada", "Depósito"... */
export function tituloDaTransacao(t: Transacao): string {
  const entrada = t.tipo === 'ENTRADA';
  switch (t.categoria) {
    case 'Pix':
      return entrada ? 'Pix recebido' : 'Pix enviado';
    case 'Transferência':
      return entrada ? 'Transferência recebida' : 'Transferência enviada';
    default:
      return t.categoria;
  }
}

/** Segunda linha: com quem foi e a mensagem, quando houver. */
export function detalheDaTransacao(t: Transacao): string {
  const partes: string[] = [];
  if (t.contraparteNome) {
    partes.push((t.tipo === 'ENTRADA' ? 'de ' : 'para ') + t.contraparteNome);
  }
  const descricao = t.descricao?.trim();
  if (descricao && descricao !== t.categoria && !descricao.startsWith(t.categoria)) {
    partes.push(descricao);
  }
  return partes.join(' · ') || (t.categoria === 'Depósito' ? 'pelo banco' : '');
}

@Pipe({ name: 'tituloTransacao' })
export class TituloTransacaoPipe implements PipeTransform {
  transform(t: Transacao): string {
    return tituloDaTransacao(t);
  }
}

@Pipe({ name: 'detalheTransacao' })
export class DetalheTransacaoPipe implements PipeTransform {
  transform(t: Transacao): string {
    return detalheDaTransacao(t);
  }
}

/** "+ R$ 10,00" / "− R$ 10,00" é feito no template; aqui só o sinal. */
@Pipe({ name: 'sinal' })
export class SinalPipe implements PipeTransform {
  transform(t: Transacao): string {
    return t.tipo === 'ENTRADA' ? '+' : '−';
  }
}
