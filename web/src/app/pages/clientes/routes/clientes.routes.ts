import { Routes } from '@angular/router';

export const CLIENTES_ROUTES: Routes = [
  {
    path: '',
    title: 'Clientes',
    loadComponent: () => import('../componentes/lista-clientes/lista-clientes').then((m) => m.ListaClientes),
  },
];
