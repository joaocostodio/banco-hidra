import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { API, PORTA_SITE } from './config';

const LOCAL = ['localhost', '127.0.0.1', '[::1]'];

interface Rede {
  enderecos: string[];
  /** Link do túnel da Cloudflare, se houver um aberto no PC (vale para qualquer rede). */
  tunel: string | null;
  /** O ADM ainda usa a senha padrão (que está no README). */
  senhaPadrao: boolean;
}

/**
 * "Modo turma": o endereço que os colegas usam para abrir o site no PC deles, nesta ordem:
 * 1. o túnel da Cloudflare, se estiver aberto (funciona de qualquer rede: outro Wi-Fi, 4G);
 * 2. o endereço por onde o ADM abriu o site, se não foi pelo localhost;
 * 3. o IP deste PC na rede local (informado pela API), que só funciona no mesmo Wi-Fi.
 */
@Injectable({ providedIn: 'root' })
export class EnderecoTurma {
  private readonly http = inject(HttpClient);

  readonly rede = rxResource({ stream: (): Observable<Rede> => this.http.get<Rede>(`${API}/painel/rede`) });

  /** Ex.: http://192.168.0.15:4300 (null se o PC não estiver numa rede local). */
  readonly url = computed(() => {
    const tunel = this.rede.value()?.tunel;
    if (tunel) {
      return tunel;
    }
    if (!LOCAL.includes(location.hostname)) {
      return location.origin;
    }
    const ip = this.rede.value()?.enderecos[0];
    return ip ? `http://${ip}:${PORTA_SITE}` : null;
  });

  readonly senhaPadrao = computed(() => this.rede.value()?.senhaPadrao ?? false);

  /** Melhor endereço para mandar a alguém: o da turma, ou este mesmo se não houver rede. */
  melhorEndereco(): string {
    return this.url() ?? location.origin;
  }
}
