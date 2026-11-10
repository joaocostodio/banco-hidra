import { Pipe, PipeTransform } from '@angular/core';

/** "Ana Maria Souza" → "AS" (para o avatar). */
@Pipe({ name: 'iniciais' })
export class IniciaisPipe implements PipeTransform {
  transform(nome: string | null | undefined): string {
    const partes = (nome ?? '').trim().split(/\s+/).filter(Boolean);
    if (partes.length === 0) {
      return '?';
    }
    const primeira = partes[0][0];
    const ultima = partes.length > 1 ? partes[partes.length - 1][0] : '';
    return (primeira + ultima).toUpperCase();
  }
}

/** Ícone (Material Symbols) de cada categoria; as criadas pelo ADM usam uma etiqueta. */
export function iconeDaCategoria(categoria: string | null | undefined): string {
  switch (categoria) {
    case 'Depósito':
      return 'savings';
    case 'Transferência':
      return 'swap_horiz';
    case 'Pix':
      return 'pix';
    case 'Pagamento':
      return 'receipt_long';
    case 'Saque':
      return 'payments';
    default:
      return 'sell';
  }
}

@Pipe({ name: 'iconeCategoria' })
export class IconeCategoriaPipe implements PipeTransform {
  transform(categoria: string | null | undefined): string {
    return iconeDaCategoria(categoria);
  }
}

/** Avatar alterna verde e prata pelo id, para a lista não ficar monótona. */
@Pipe({ name: 'corAvatar' })
export class CorAvatarPipe implements PipeTransform {
  transform(id: number | null | undefined): string {
    return (id ?? 0) % 2 === 0 ? 'avatar prata' : 'avatar';
  }
}
