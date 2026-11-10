import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../../../common/config';
import { ChavePix, Comprovante, Conta, Deposito, Periodo, Transacao } from '../model/conta.model';

/** Contas vistas pelo administrador (/contas e /transacoes da API). */
@Injectable({ providedIn: 'root' })
export class ContaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/contas`;

  listar(): Observable<Conta[]> {
    return this.http.get<Conta[]>(this.url);
  }

  buscar(id: number): Observable<Conta> {
    return this.http.get<Conta>(`${this.url}/${id}`);
  }

  extrato(id: number, periodo: Periodo): Observable<Transacao[]> {
    return this.http.get<Transacao[]>(`${this.url}/${id}/extrato`, { params: { ...periodo } });
  }

  chavesPix(id: number): Observable<ChavePix[]> {
    return this.http.get<ChavePix[]>(`${this.url}/${id}/chaves-pix`);
  }

  depositar(id: number, deposito: Deposito): Observable<Transacao> {
    return this.http.post<Transacao>(`${this.url}/${id}/depositos`, deposito);
  }

  bloquear(id: number): Observable<Conta> {
    return this.http.post<Conta>(`${this.url}/${id}/bloqueio`, null);
  }

  desbloquear(id: number): Observable<Conta> {
    return this.http.delete<Conta>(`${this.url}/${id}/bloqueio`);
  }

  encerrar(id: number): Observable<Conta> {
    return this.http.delete<Conta>(`${this.url}/${id}`);
  }

  /** Últimas movimentações do banco inteiro. */
  ultimasTransacoes(limite = 50): Observable<Transacao[]> {
    return this.http.get<Transacao[]>(`${API}/transacoes`, { params: { limite } });
  }

  comprovante(autenticacao: string): Observable<Comprovante> {
    return this.http.get<Comprovante>(`${API}/transacoes/comprovantes/${autenticacao}`);
  }
}

/** Data local no formato da API (AAAA-MM-DD). */
export function dataApi(data: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${data.getFullYear()}-${p(data.getMonth() + 1)}-${p(data.getDate())}`;
}
