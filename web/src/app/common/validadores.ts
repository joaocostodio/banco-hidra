import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function somenteDigitos(texto: string | null | undefined): string {
  return (texto ?? '').replace(/\D/g, '');
}

/** Confere os dois dígitos verificadores do CPF (mesma regra da API). */
export function cpfEhValido(texto: string): boolean {
  const cpf = somenteDigitos(texto);
  if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) {
    return false;
  }
  const digito = (base: string) => {
    let soma = 0;
    for (let i = 0; i < base.length; i++) {
      soma += Number(base[i]) * (base.length + 1 - i);
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  const d1 = digito(cpf.slice(0, 9));
  const d2 = digito(cpf.slice(0, 9) + d1);
  return cpf.endsWith(`${d1}${d2}`);
}

/** 52998224725 → 529.982.247-25 (formata enquanto a pessoa digita). */
export function formatarCpf(texto: string): string {
  const d = somenteDigitos(texto).slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2');
}

/** 529.982.247-25 → ***.982.247-** (como os bancos mostram). */
export function mascararCpf(cpf: string | null): string {
  const d = somenteDigitos(cpf);
  return d.length === 11 ? `***.${d.slice(3, 6)}.${d.slice(6, 9)}-**` : '—';
}

export const cpfValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null => {
  const valor = somenteDigitos(c.value);
  if (!valor) {
    return null; // "required" cuida do vazio
  }
  return valor.length === 11 && cpfEhValido(valor) ? null : { cpf: true };
};

/**
 * Valor digitado em reais → número. Aceita "1234,56", "1.234,56" e "1234.56".
 * Devolve NaN se não for um número.
 */
export function lerValor(texto: string | null | undefined): number {
  let t = (texto ?? '').replace(/[R$\s]/g, '');
  if (!t) {
    return NaN;
  }
  if (t.includes(',')) {
    t = t.replace(/\./g, '').replace(',', '.');
  }
  return /^\d+(\.\d+)?$/.test(t) ? Number(t) : NaN;
}

/** Valor em reais: maior que zero e com no máximo 2 casas (mesma regra da API). */
export const valorValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null => {
  if (!c.value) {
    return null;
  }
  const v = lerValor(c.value);
  if (Number.isNaN(v)) {
    return { valor: 'Digite um valor como 150,00' };
  }
  if (v <= 0) {
    return { valor: 'O valor deve ser maior que zero' };
  }
  if (Math.round(v * 100) / 100 !== v) {
    return { valor: 'Use no máximo 2 casas depois da vírgula' };
  }
  if (v > 1_000_000_000) {
    return { valor: 'Valor alto demais' };
  }
  return null;
};

/** As regras de senha da API, uma a uma, para mostrar a lista enquanto a pessoa digita. */
export function regrasDaSenha(senha: string) {
  return {
    tamanho: senha.length >= 8 && senha.length <= 72,
    letras: /\p{L}/u.test(senha),
    numeros: /\d/.test(senha),
  };
}

export const senhaForteValidator: ValidatorFn = (c: AbstractControl): ValidationErrors | null => {
  const senha: string = c.value ?? '';
  if (!senha) {
    return null;
  }
  const r = regrasDaSenha(senha);
  return r.tamanho && r.letras && r.numeros ? null : { senhaFraca: true };
};

/** Validador de grupo: "novaSenha" e "confirmacao" iguais; "novaSenha" diferente de "senhaAtual". */
export const senhasValidator: ValidatorFn = (g: AbstractControl): ValidationErrors | null => {
  const atual = g.get('senhaAtual')?.value;
  const nova = g.get('novaSenha')?.value;
  const confirmacao = g.get('confirmacao')?.value;
  const erros: ValidationErrors = {};
  if (nova && confirmacao && nova !== confirmacao) {
    erros['naoConferem'] = true;
  }
  if (nova && atual && nova === atual) {
    erros['igualAtual'] = true;
  }
  return Object.keys(erros).length ? erros : null;
};
