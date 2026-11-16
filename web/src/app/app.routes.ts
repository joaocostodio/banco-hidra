import { Routes } from '@angular/router';
import { exigePerfil, inicioGuard } from './common/auth/auth.guards';
import { LayoutAdmin } from './common/layout-admin/layout-admin';
import { LayoutCliente } from './common/layout-cliente/layout-cliente';
import { ACESSO_ROUTES } from './pages/acesso/routes/acesso.routes';

export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [inicioGuard], children: [] },
  ...ACESSO_ROUTES,

  // Painel do administrador
  {
    path: 'admin',
    component: LayoutAdmin,
    canActivate: [exigePerfil('ADMIN')],
    children: [
      { path: '', loadChildren: () => import('./pages/painel/routes/painel.routes').then((m) => m.PAINEL_ROUTES) },
      {
        path: 'clientes',
        loadChildren: () => import('./pages/clientes/routes/clientes.routes').then((m) => m.CLIENTES_ROUTES),
      },
      {
        path: 'contas',
        loadChildren: () => import('./pages/contas/routes/contas.routes').then((m) => m.CONTAS_ROUTES),
      },
      {
        path: 'movimentacoes',
        loadChildren: () =>
          import('./pages/movimentacoes/routes/movimentacoes.routes').then((m) => m.MOVIMENTACOES_ROUTES),
      },
      {
        path: 'categorias',
        loadChildren: () => import('./pages/categorias/routes/categorias.routes').then((m) => m.CATEGORIAS_ROUTES),
      },
    ],
  },

  // App do cliente
  {
    path: 'app',
    component: LayoutCliente,
    canActivate: [exigePerfil('CLIENTE')],
    // As rotas da conta (início, extrato, perfil), da gestão financeira (contas, lançar, categorias),
    // do Pix e dos boletos ficam no mesmo nível: /app, /app/contas, /app/extrato, /app/pix...
    loadChildren: () =>
      Promise.all([
        import('./pages/minha-conta/routes/minha-conta.routes'),
        import('./pages/financas/routes/financas.routes'),
        import('./pages/pix/routes/pix.routes'),
        import('./pages/boletos/routes/boletos.routes'),
      ]).then(([conta, financas, pix, boletos]) => [
        ...conta.MINHA_CONTA_ROUTES,
        ...financas.FINANCAS_ROUTES,
        ...pix.PIX_ROUTES,
        ...boletos.BOLETOS_ROUTES,
      ]),
  },

  { path: '**', redirectTo: '' },
];
