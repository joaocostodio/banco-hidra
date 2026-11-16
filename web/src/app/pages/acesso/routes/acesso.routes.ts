import { Routes } from '@angular/router';
import { primeiroAcessoGuard, visitanteGuard } from '../../../common/auth/auth.guards';

export const ACESSO_ROUTES: Routes = [
  {
    path: 'entrar',
    title: 'Entrar',
    canActivate: [visitanteGuard],
    loadComponent: () => import('../componentes/entrar/entrar').then((m) => m.Entrar),
  },
  {
    path: 'criar-conta',
    title: 'Criar conta',
    canActivate: [visitanteGuard],
    loadComponent: () => import('../componentes/criar-conta/criar-conta').then((m) => m.CriarConta),
  },
  {
    path: 'primeiro-acesso',
    title: 'Primeiro acesso',
    canActivate: [primeiroAcessoGuard],
    loadComponent: () => import('../componentes/primeiro-acesso/primeiro-acesso').then((m) => m.PrimeiroAcesso),
  },
];
