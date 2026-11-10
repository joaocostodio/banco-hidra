import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { Comprovante } from '../../../common/comprovante/comprovante.model';
import { API } from '../../../common/config';
import {
  ChavePix,
  Conta,
  Destinatario,
  EnvioPix,
  MeusDados,
  Periodo,
  TipoChavePix,
  Transacao,
  Transferencia,
} from '../model/minha-conta.model';

const CHAVE_OCULTAR = 'banco-hidra.ocultar-saldo';

/** Internet banking do cliente: a conta é sempre a de quem está logado (/minha-conta da API). */
@Injectable({ providedIn: 'root' })
export class MinhaContaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/minha-conta`;

  /** "Esconder saldo" vale para todas as telas e fica lembrado neste navegador. */
  readonly saldoOculto = signal(lerPreferencia());

  alternarSaldo(): void {
    this.saldoOculto.update((v) => !v);
    try {
      localStorage.setItem(CHAVE_OCULTAR, String(this.saldoOculto()));
    } catch {
      // sem armazenamento: vale só nesta visita
    }
  }

  conta(): Observable<Conta> {
    return this.http.get<Conta>(this.url);
  }

  dados(): Observable<MeusDados> {
    return this.http.get<MeusDados>(`${this.url}/dados`);
  }

  extrato(periodo: Periodo): Observable<Transacao[]> {
    return this.http.get<Transacao[]>(`${this.url}/extrato`, { params: { ...periodo } });
  }

  comprovante(autenticacao: string): Observable<Comprovante> {
    return this.http.get<Comprovante>(`${this.url}/comprovantes/${autenticacao}`);
  }

  // ---------- Transferência por agência e conta ----------
  destinatarioPorConta(agencia: string, numero: string): Observable<Destinatario> {
    return this.http.get<Destinatario>(`${this.url}/destinatario`, { params: { agencia, numero } });
  }

  transferir(t: Transferencia): Observable<Transacao> {
    return this.http.post<Transacao>(`${this.url}/transferencias`, t);
  }

  // ---------- Pix ----------
  chaves(): Observable<ChavePix[]> {
    return this.http.get<ChavePix[]>(`${this.url}/pix/chaves`);
  }

  /** Valor em branco para CPF/e-mail: o banco usa o do titular. Aleatória: o banco gera. */
  cadastrarChave(tipo: TipoChavePix, valor: string): Observable<ChavePix> {
    return this.http.post<ChavePix>(`${this.url}/pix/chaves`, { tipo, valor });
  }

  excluirChave(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/pix/chaves/${id}`);
  }

  destinatarioPorChave(chave: string): Observable<Destinatario> {
    return this.http.get<Destinatario>(`${this.url}/pix/destinatario`, { params: { chave } });
  }

  enviarPix(pix: EnvioPix): Observable<Transacao> {
    return this.http.post<Transacao>(`${this.url}/pix`, pix);
  }
}

function lerPreferencia(): boolean {
  try {
    return localStorage.getItem(CHAVE_OCULTAR) === 'true';
  } catch {
    return false;
  }
}

/** Período dos últimos N dias (hoje incluído), no formato da API. */
export function ultimosDias(dias: number): Periodo {
  const hoje = new Date();
  const inicio = new Date();
  inicio.setDate(inicio.getDate() - (dias - 1));
  return { inicio: dataApi(inicio), fim: dataApi(hoje) };
}

export function dataApi(data: Date): string {
  const p = (n: number) => String(n).padStart(2, '0');
  return `${data.getFullYear()}-${p(data.getMonth() + 1)}-${p(data.getDate())}`;
}

export interface GrupoDoDia {
  dia: string;
  transacoes: Transacao[];
  /** Saldo ao fim do dia (da transação mais recente daquele dia). */
  saldoFinal: number;
}

/** Extrato agrupado por dia (a API já manda do mais recente para o mais antigo). */
export function agruparPorDia(transacoes: Transacao[]): GrupoDoDia[] {
  const grupos: GrupoDoDia[] = [];
  for (const t of transacoes) {
    const dia = t.dataHora.slice(0, 10);
    const ultimo = grupos[grupos.length - 1];
    if (ultimo?.dia === dia) {
      ultimo.transacoes.push(t);
    } else {
      grupos.push({ dia, transacoes: [t], saldoFinal: t.saldoApos });
    }
  }
  return grupos;
}

/** "Hoje", "Ontem" ou null (aí a tela mostra a data). */
export function nomeDoDia(dia: string, hoje = new Date()): string | null {
  const ontem = new Date(hoje);
  ontem.setDate(hoje.getDate() - 1);
  if (dia === dataApi(hoje)) {
    return 'Hoje';
  }
  return dia === dataApi(ontem) ? 'Ontem' : null;
}
