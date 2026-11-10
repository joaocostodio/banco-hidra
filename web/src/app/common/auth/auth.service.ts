import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API } from '../config';
import { LoginResposta } from './sessao.model';

const CHAVE = 'banco-hidra.sessao';

/** Guarda quem está logado (token JWT + dados do usuário) e decide para onde cada perfil vai. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly sessao = signal<LoginResposta | null>(lerSessao());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);
  readonly precisaTrocarSenha = computed(() => this.sessao()?.trocarSenha ?? false);
  readonly primeiroNome = computed(() => this.usuario()?.nome.split(' ')[0] ?? '');

  /** Token válido (ou null se não há sessão ou se ela venceu). */
  token(): string | null {
    const s = this.sessao();
    if (!s) {
      return null;
    }
    if (new Date(s.expiraEm).getTime() <= Date.now()) {
      this.limpar();
      return null;
    }
    return s.token;
  }

  estaLogado(): boolean {
    return this.token() !== null;
  }

  entrar(email: string, senha: string): Observable<LoginResposta> {
    return this.http
      .post<LoginResposta>(`${API}/auth/login`, { email, senha })
      .pipe(tap((r) => this.guardar(r)));
  }

  trocarSenha(senhaAtual: string, novaSenha: string): Observable<LoginResposta> {
    return this.http
      .put<LoginResposta>(`${API}/auth/senha`, { senhaAtual, novaSenha })
      .pipe(tap((r) => this.guardar(r)));
  }

  /** Sai e volta para o login. {@code motivo} vira uma mensagem na tela de entrar. */
  sair(motivo?: 'expirou'): void {
    this.limpar();
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
    return texto ? (JSON.parse(texto) as LoginResposta) : null;
  } catch {
    return null;
  }
}
