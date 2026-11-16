import { Routes } from '@angular/router';

/** Boletos do cliente: emitir para receber, pagar o de outra pessoa e ver/imprimir. */
export const BOLETOS_ROUTES: Routes = [
  {
    path: 'boletos',
    title: 'Boletos',
    loadComponent: () => import('../componentes/lista-boletos/lista-boletos').then((m) => m.ListaBoletos),
  },
  {
    path: 'boletos/emitir',
    title: 'Emitir boleto',
    loadComponent: () => import('../componentes/emitir-boleto/emitir-boleto').then((m) => m.EmitirBoleto),
  },
  {
    path: 'boletos/pagar',
    title: 'Pagar boleto',
    loadComponent: () => import('../componentes/pagar-boleto/pagar-boleto').then((m) => m.PagarBoleto),
  },
  {
    path: 'boletos/:id',
    title: 'Boleto',
    loadComponent: () => import('../componentes/ver-boleto/ver-boleto').then((m) => m.VerBoleto),
  },
];
