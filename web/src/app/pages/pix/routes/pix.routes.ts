import { Routes } from '@angular/router';

const enviar = () => import('../componentes/enviar/enviar').then((m) => m.Enviar);

/** Pix e transferência (a mesma tela de envio, em modos diferentes). */
export const PIX_ROUTES: Routes = [
  {
    path: 'pix',
    title: 'Pix',
    loadComponent: () => import('../componentes/area-pix/area-pix').then((m) => m.AreaPix),
  },
  { path: 'pix/enviar', title: 'Enviar Pix', data: { modo: 'pix' }, loadComponent: enviar },
  {
    path: 'pix/cobrar',
    title: 'Cobrar com QR code',
    loadComponent: () => import('../componentes/cobrar/cobrar').then((m) => m.Cobrar),
  },
  { path: 'transferir', title: 'Transferir', data: { modo: 'transferencia' }, loadComponent: enviar },
];
