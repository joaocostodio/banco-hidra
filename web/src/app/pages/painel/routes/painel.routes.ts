import { Routes } from '@angular/router';

export const PAINEL_ROUTES: Routes = [
  {
    path: '',
    title: 'Painel',
    loadComponent: () => import('../componentes/painel/painel').then((m) => m.Painel),
  },
];
