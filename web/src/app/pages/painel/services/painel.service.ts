import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../../../common/config';
import { DiaMovimento, ResumoPainel } from '../model/painel.model';

@Injectable({ providedIn: 'root' })
export class PainelService {
  private readonly http = inject(HttpClient);

  resumo(): Observable<ResumoPainel> {
    return this.http.get<ResumoPainel>(`${API}/painel`);
  }
}

/** Altura de cada barra do gráfico (em %), relativa ao maior dia. */
export function alturasDasBarras(dias: DiaMovimento[]): number[] {
  const maior = Math.max(...dias.map((d) => d.valor), 0);
  // Dia sem movimento fica vazio; dia com pouco movimento ganha um mínimo para aparecer.
  return dias.map((d) => (maior === 0 || d.valor === 0 ? 0 : Math.max(4, Math.round((d.valor / maior) * 100))));
}

/** "Bom dia" / "Boa tarde" / "Boa noite". */
export function saudacao(agora = new Date()): string {
  const h = agora.getHours();
  if (h >= 5 && h < 12) {
    return 'Bom dia';
  }
  return h >= 12 && h < 18 ? 'Boa tarde' : 'Boa noite';
}
