import { Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/**
 * Usa o "title" de cada rota no título da aba ("Clientes · Banco Hidra")
 * e guarda o nome da página atual para o topo do painel.
 */
@Injectable({ providedIn: 'root' })
export class Titulo extends TitleStrategy {
  private readonly title = inject(Title);

  readonly atual = signal('');

  override updateTitle(estado: RouterStateSnapshot): void {
    const titulo = this.buildTitle(estado) ?? '';
    this.atual.set(titulo);
    this.title.setTitle(titulo ? `${titulo} · Banco Hidra` : 'Banco Hidra');
  }
}
