import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { API, PORTA_SITE } from './config';

const LOCAL = ['localhost', '127.0.0.1', '[::1]'];

interface Rede {
  enderecos: string[];
  /** O ADM ainda usa a senha padrão (que está no README). */
  senhaPadrao: boolean;
}

/**
 * "Modo turma": o endereço que os colegas usam para abrir o site no PC deles.
 * Se o ADM abriu pelo IP, é esse; se abriu pelo localhost, usa o IP deste PC na rede (informado pela API).
 */
@Injectable({ providedIn: 'root' })
export class EnderecoTurma {
  private readonly http = inject(HttpClient);

  readonly rede = rxResource({ stream: (): Observable<Rede> => this.http.get<Rede>(`${API}/painel/rede`) });

  /** Ex.: http://192.168.0.15:4300 (null se o PC não estiver numa rede local). */
  readonly url = computed(() => {
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
