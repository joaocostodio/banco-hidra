import { Routes } from '@angular/router';

export const MOVIMENTACOES_ROUTES: Routes = [
  {
    path: '',
    title: 'Movimentações',
    loadComponent: () => import('../componentes/movimentacoes/movimentacoes').then((m) => m.Movimentacoes),
  },
];
