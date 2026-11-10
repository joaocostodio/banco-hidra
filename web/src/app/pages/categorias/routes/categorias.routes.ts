import { Routes } from '@angular/router';

export const CATEGORIAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Categorias',
    loadComponent: () => import('../componentes/lista-categorias/lista-categorias').then((m) => m.ListaCategorias),
  },
];
