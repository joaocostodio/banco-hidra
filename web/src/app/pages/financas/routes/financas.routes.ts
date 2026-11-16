import { Routes } from '@angular/router';

/** Gestão financeira do cliente: contas, lançamentos e categorias. */
export const FINANCAS_ROUTES: Routes = [
  {
    path: 'contas',
    title: 'Minhas contas',
    loadComponent: () => import('../componentes/minhas-contas/minhas-contas').then((m) => m.MinhasContas),
  },
  {
    path: 'lancar',
    title: 'Novo lançamento',
    loadComponent: () => import('../componentes/lancar/lancar').then((m) => m.Lancar),
  },
  {
    path: 'categorias',
    title: 'Categorias',
    loadComponent: () => import('../componentes/categorias/categorias').then((m) => m.Categorias),
  },
];
