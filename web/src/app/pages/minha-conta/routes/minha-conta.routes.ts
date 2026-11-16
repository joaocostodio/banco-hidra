import { Routes } from '@angular/router';

export const MINHA_CONTA_ROUTES: Routes = [
  {
    path: '',
    title: 'Início',
    loadComponent: () => import('../componentes/inicio-cliente/inicio-cliente').then((m) => m.InicioCliente),
  },
  {
    path: 'extrato',
    title: 'Transações',
    loadComponent: () => import('../componentes/extrato/extrato').then((m) => m.Extrato),
  },
  {
    path: 'perfil',
    title: 'Meu perfil',
    loadComponent: () => import('../componentes/perfil/perfil').then((m) => m.Perfil),
  },
];
