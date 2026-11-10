import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { regrasDaSenha } from '../validadores';

/** Barra de força + lista das regras da senha, marcadas conforme a pessoa digita. */
@Component({
  selector: 'app-regras-senha',
  imports: [MatIconModule],
  templateUrl: './regras-senha.html',
  styleUrl: './regras-senha.css',
})
export class RegrasSenha {
  readonly senha = input('');
  /** Senha que a nova não pode repetir (a atual/provisória). */
  readonly anterior = input('');

  protected readonly regras = computed(() => {
    const r = regrasDaSenha(this.senha());
    return [
      { ok: r.tamanho, texto: 'Pelo menos 8 caracteres' },
      { ok: r.letras, texto: 'Tem letras' },
      { ok: r.numeros, texto: 'Tem números' },
      { ok: !!this.senha() && this.senha() !== this.anterior(), texto: 'Diferente da senha atual' },
    ];
  });

  /** 0 a 4 tracinhos: as 3 regras obrigatórias + bônus por tamanho e variedade. */
  protected readonly forca = computed(() => {
    const s = this.senha();
    if (!s) {
      return 0;
    }
    const r = regrasDaSenha(s);
    let pontos = [r.tamanho, r.letras, r.numeros].filter(Boolean).length;
    if (pontos === 3 && s.length >= 12 && /[^\p{L}\d]/u.test(s)) {
      pontos = 4;
    } else if (pontos === 3 && (s.length >= 12 || /[^\p{L}\d]/u.test(s) || /[A-Z]/.test(s))) {
      pontos = 3.5;
    }
    return pontos;
  });

  protected readonly rotulo = computed(() => {
    const f = this.forca();
    if (f === 0) {
      return '';
    }
    if (f < 3) {
      return 'Senha fraca';
    }
    return f >= 4 ? 'Senha forte' : 'Senha boa';
  });

  protected readonly tracos = [1, 2, 3, 4];
}
