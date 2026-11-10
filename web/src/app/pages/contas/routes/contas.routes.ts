import { Routes } from '@angular/router';

export const CONTAS_ROUTES: Routes = [
  {
    path: '',
    title: 'Contas',
    loadComponent: () => import('../componentes/lista-contas/lista-contas').then((m) => m.ListaContas),
  },
  {
    path: ':id',
    title: 'Conta',
    loadComponent: () => import('../componentes/detalhe-conta/detalhe-conta').then((m) => m.DetalheConta),
  },
];
