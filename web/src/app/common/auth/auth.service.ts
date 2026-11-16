import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API } from '../config';
import { Cadastro, LoginResposta } from './sessao.model';

const CHAVE = 'banco-hidra.sessao';

/**
 * Quem está logado e para onde cada perfil vai.
 *
 * A sessão de verdade é o cookie httpOnly que a API grava (o JavaScript não lê, então um script injetado
 * não rouba a sessão). Aqui fica só o que a tela precisa mostrar: nome, perfil e quando a sessão vence.
 * Se esses dados forem alterados no navegador, nada muda na API: ela confere o cookie em toda chamada.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessao = signal<LoginResposta | null>(lerSessao());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);
  readonly precisaTrocarSenha = computed(() => this.sessao()?.trocarSenha ?? false);
  readonly primeiroNome = computed(() => this.usuario()?.nome.split(' ')[0] ?? '');

  /** Há sessão e ela ainda não venceu (se venceu, esquece). */
  estaLogado(): boolean {
    const s = this.sessao();
    if (!s) {
      return false;
    }
    if (new Date(s.expiraEm).getTime() <= Date.now()) {
      this.limpar();
      return false;
    }
    return true;
  }

  entrar(email: string, senha: string): Observable<LoginResposta> {
    return this.http
      .post<LoginResposta>(`${API}/auth/login`, { email, senha })
      .pipe(tap((r) => this.guardar(r)));
  }

  /** Cria a conta e já entra. */
  cadastrar(dados: Cadastro): Observable<LoginResposta> {
    return this.http.post<LoginResposta>(`${API}/auth/cadastro`, dados).pipe(tap((r) => this.guardar(r)));
  }

  trocarSenha(senhaAtual: string, novaSenha: string): Observable<LoginResposta> {
    return this.http
      .put<LoginResposta>(`${API}/auth/senha`, { senhaAtual, novaSenha })
      .pipe(tap((r) => this.guardar(r)));
  }

  /**
   * Sai e volta para o login. A API apaga o cookie; se ela não responder, o cookie vence sozinho
   * (dura no máximo 1 hora). {@code motivo} vira uma mensagem na tela de entrar.
   */
  sair(motivo?: 'expirou'): void {
    this.limpar();
    this.http.post(`${API}/auth/sair`, null).subscribe({ error: () => undefined });
    this.router.navigate(['/entrar'], { queryParams: motivo ? { motivo } : {} });
  }

  /** Para onde mandar quem acabou de entrar (ou abriu a raiz do site). */
  destinoInicial(): string {
    if (!this.estaLogado()) {
      return '/entrar';
    }
    if (this.precisaTrocarSenha()) {
      return '/primeiro-acesso';
    }
    return this.usuario()?.perfil === 'ADMIN' ? '/admin' : '/app';
  }

  private guardar(resposta: LoginResposta): void {
    this.sessao.set(resposta);
    try {
      localStorage.setItem(CHAVE, JSON.stringify(resposta));
    } catch {
      // Navegador sem armazenamento: a sessão vale só enquanto a aba estiver aberta.
    }
  }

  private limpar(): void {
    this.sessao.set(null);
    try {
      localStorage.removeItem(CHAVE);
    } catch {
      // nada a fazer
    }
  }
}

function lerSessao(): LoginResposta | null {
  try {
    const texto = localStorage.getItem(CHAVE);
    const sessao = texto ? (JSON.parse(texto) as Partial<LoginResposta> & { token?: unknown }) : null;
    // Sessão salva pela versão antiga (com o token no localStorage): descarta e pede login de novo.
    if (!sessao || 'token' in sessao || !sessao.expiraEm || !sessao.usuario) {
      localStorage.removeItem(CHAVE);
      return null;
    }
    return sessao as LoginResposta;
  } catch {
    return null;
  }
}
