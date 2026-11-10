import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../../../common/config';
import { Categoria } from '../model/categoria.model';

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/categorias`;

  listar(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(this.url);
  }

  criar(descricao: string): Observable<Categoria> {
    return this.http.post<Categoria>(this.url, { descricao });
  }

  editar(id: number, descricao: string): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.url}/${id}`, { descricao });
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

/** Só pode excluir categoria criada pelo ADM e que nenhuma transação usa. */
export function podeExcluir(c: Categoria): boolean {
  return !c.sistema && c.usos === 0;
}
