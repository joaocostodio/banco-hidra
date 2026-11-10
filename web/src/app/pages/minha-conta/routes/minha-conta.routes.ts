import { Routes } from '@angular/router';

export const MINHA_CONTA_ROUTES: Routes = [
  {
    path: '',
    title: 'Minha conta',
    loadComponent: () => import('../componentes/inicio-cliente/inicio-cliente').then((m) => m.InicioCliente),
  },
];
