import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API } from '../../../common/config';
import { somenteDigitos } from '../../../common/validadores';
import { Cliente, EdicaoCliente, FiltroCliente, NovoCliente, NovoClienteResposta } from '../model/cliente.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API}/clientes`;

  listar(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.url);
  }

  buscar(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.url}/${id}`);
  }

  /** Cadastra e abre a conta. A resposta traz a senha provisória (única vez que ela aparece). */
  cadastrar(novo: NovoCliente): Observable<NovoClienteResposta> {
    return this.http.post<NovoClienteResposta>(this.url, { ...novo, cpf: somenteDigitos(novo.cpf) });
  }

  editar(id: number, dados: EdicaoCliente): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.url}/${id}`, dados);
  }

  novaSenhaProvisoria(id: number): Observable<string> {
    return this.http
      .post<{ senhaProvisoria: string }>(`${this.url}/${id}/senha-provisoria`, null)
      .pipe(map((r) => r.senhaProvisoria));
  }
}

/**
 * Regras de busca e filtro da lista (ficam aqui para a tela só exibir).
 * A busca aceita nome, e-mail, CPF ou número da conta, com ou sem pontuação.
 */
export function filtrarClientes(clientes: Cliente[], termo: string, filtro: FiltroCliente): Cliente[] {
  const texto = termo.trim().toLowerCase();
  const digitos = somenteDigitos(termo);
  return clientes.filter((c) => {
    if (filtro === 'primeiro-acesso' && !c.primeiroAcesso) {
      return false;
    }
    if (filtro !== 'todos' && filtro !== 'primeiro-acesso' && c.conta?.status !== filtro) {
      return false;
    }
    if (!texto) {
      return true;
    }
    return (
      c.nome.toLowerCase().includes(texto) ||
      c.email.toLowerCase().includes(texto) ||
      (digitos.length >= 3 &&
        (somenteDigitos(c.cpf).includes(digitos) || somenteDigitos(c.conta?.numero).includes(digitos)))
    );
  });
}
