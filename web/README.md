# Banco Hidra — site (Angular)

Site do Banco Hidra: Angular 22 + Angular Material. Visão geral, API e segurança estão no
[README principal](../README.md).

## Rodar

Com a API rodando (porta 8080):

```bash
npm install
npm start          # http://localhost:4300 (também na rede, para a turma)
```

O site chama a API em `/api`; o `ng serve` repassa para `http://127.0.0.1:8080` (`proxy.conf.json`).
Por isso o cookie de sessão é do próprio site.

## Compilar

```bash
npm run build      # gera dist/web (produção)
```

Em produção, sirva `dist/web/browser` num servidor que também repasse `/api` para a API (como o proxy faz em dev).

## Organização

Segue as instruções da disciplina: só CSS, signals, reactive forms, HTML/CSS separados do TypeScript.

```
src/app/
├── common/            reaproveitado: sessão (auth), layout, comprovante, diálogos, validações, formatos
└── pages/
    ├── acesso/        entrar, criar conta, primeiro acesso, trocar senha
    ├── minha-conta/   início (resumo), transações (filtros), perfil
    ├── financas/      minhas contas, lançar receita/despesa, categorias
    ├── pix/           enviar Pix / transferir, chaves, cobrar com QR code
    ├── boletos/       emitir, pagar, ver/imprimir
    ├── painel/        (ADM) números do banco
    ├── clientes/      (ADM) cadastro e edição de clientes
    ├── contas/        (ADM) contas, extrato, depósito, bloqueio
    ├── movimentacoes/ (ADM) últimas movimentações
    └── categorias/    (ADM) categorias padrão
```

Cada funcionalidade tem `model/` (interfaces), `services/` (chamadas à API e regras de tela),
`routes/` (rotas, carregadas sob demanda) e `componentes/`.
