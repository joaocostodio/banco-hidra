import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../../../common/config';
import { Conta } from '../../contas/model/conta.model';

/** Área do cliente: a conta é sempre a de quem está logado. */
@Injectable({ providedIn: 'root' })
export class MinhaContaService {
  private readonly http = inject(HttpClient);

  conta(): Observable<Conta> {
    return this.http.get<Conta>(`${API}/minha-conta`);
  }
}
