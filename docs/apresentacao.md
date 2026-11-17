# Roteiro da apresentação

Onde está cada requisito da atividade no código, para mostrar funcionando e explicar o trecho.
Os caminhos da API são relativos a `api/src/main/java/bancohidra/`; os do site, a `web/src/app/`.

## Antes de começar

```bash
docker compose up -d
cd api && ./mvnw quarkus:dev        # API
cd web && npm start                 # site: http://localhost:4300
```

Roteiro sugerido na tela: **Criar conta → lançar uma receita (Salário) → abrir uma Poupança → transferir para
ela → lançar duas despesas → Início (saldo por conta e geral) → Transações filtrando por conta e por categoria**.

## Requisitos da atividade

| Requisito | Na tela | No código |
|---|---|---|
| Lançamento de contas | Contas → Nova conta | `service/ClienteService.abrirConta()`, `controller/MinhasContasController` · site: `pages/financas/componentes/minhas-contas` |
| Categorias | Transações → Categorias | `entity/Categoria` (padrão × própria pelo campo `dono`), `service/FinancasService.criarCategoria()` · site: `pages/financas/componentes/categorias` |
| Transações | Início → Receita / Despesa | `service/FinancasService.lancar()` → `service/MovimentacaoService.lancar()` · site: `pages/financas/componentes/lancar` |
| Filtro por categoria e conta | Transações | `service/FinancasService.transacoes()` (JPQL só com parâmetros) · site: `pages/minha-conta/componentes/extrato` |
| Transferência entre contas | Transferir → Entre minhas contas | `service/MovimentacaoService.transferir()` · site: `pages/pix/componentes/enviar` |
| Saldo por conta e geral | Início | `service/FinancasService.resumo()` · site: `pages/minha-conta/componentes/inicio-cliente` |
| JWT em cookie httpOnly | (F12 → `document.cookie` vem vazio) | `security/CookieSessao`, `controller/AuthController`, `application.properties` (`mp.jwt.token.cookie`) · site: `common/auth/auth.interceptor.ts` |

## Trechos bons para explicar

**1. Encapsulamento do saldo** — `entity/Conta.java`, `depositar()` e `sacar()`.
O campo `saldo` é `private` e não tem setter: a única forma de mudar é pelos dois métodos, que validam valor,
situação da conta e saldo suficiente. Nenhum outro código consegue "setar" um saldo errado.

**2. Transferência atômica e sem deadlock** — `service/MovimentacaoService.transferir()`.
`@Transactional`: ou grava as duas pontas, ou nada. Trava as duas contas sempre na mesma ordem (menor id
primeiro), para duas transferências cruzadas (A→B e B→A) não ficarem esperando uma pela outra.

**3. Concorrência** — `service/MovimentacaoService.travar()`.
`SELECT ... FOR UPDATE` + releitura do saldo. O teste `FinancasTest.despesasSimultaneasNaoPassamDoSaldo` manda
10 despesas de R$ 20 ao mesmo tempo numa conta com R$ 100: passam exatamente 5. (Sem a releitura, passavam as 10.)

**4. Sessão segura** — `security/CookieSessao.java` e `security/ProtecaoCsrf.java`.
- `HttpOnly`: o JavaScript não lê o cookie → um XSS não rouba a sessão.
- `SameSite=Strict` + cabeçalho `X-Requested-With` obrigatório → outro site não faz ações em nome do usuário (CSRF).
- O papel (`CLIENTE`/`ADMIN`) vai dentro do JWT assinado; `@RolesAllowed` em cada controller.

**5. Um cliente não enxerga o outro** — `service/ClienteService.contaDo()` e `FinancasService.categoriaVisivel()`.
Tudo parte do usuário do token, nunca de um id vindo da tela. Recurso de outra pessoa responde 404
(nem confirma que existe). Testado em `FinancasTest.naoMexeNaContaDeOutroCliente`.

**6. Resumo do mês sem contar transferência interna** — `service/FinancasService.resumo()`.
Mover dinheiro da corrente para a poupança não é receita nem despesa: a consulta ignora transações cuja
contraparte é do mesmo titular.

**7. Herança e polimorfismo das contas** — `entity/Conta.java` (abstrata) e as filhas `ContaCorrente`,
`ContaCarteira`, `ContaPoupanca`, `ContaInvestimento`.
`Conta` tem métodos "gancho" (`exigirPodeReceberDe`, `exigirPodeEnviarPara`, `aceitaLancamento`, `prepararSaida`,
`aoReceber`) e as filhas sobrescrevem o que muda. O `MovimentacaoService.transferir()` chama os ganchos sem saber
o tipo da conta: é o polimorfismo. Na tela: abrir um Investimento e tentar lançar uma despesa nele (recusa) ou
transferir para a conta de outra pessoa (recusa).

**8. Método-modelo nos rendimentos** — `entity/ContaRemunerada.render()`.
O "como" é igual para poupança e investimento (acha a data-base, credita, guarda até quando pagou); o "quanto" vem
de `calcularRendimento()`, que cada filha implementa: poupança 0,5% ao mês, investimento diário pela taxa do
produto (`entity/ProdutoInvestimento`). `service/RendimentoService` roda todo dia (`@Scheduled`).

**9. Imposto de Renda regressivo** — `entity/ContaInvestimento.prepararSaida()` e `aliquotaPorPrazo()`.
No resgate, só o lucro paga imposto (22,5% até 180 dias, 20% até 360, 17,5% até 720, 15% depois; LCI isenta). A
conta guarda o `valorAplicado` e a data média das aplicações para separar lucro de principal.

**10. Meta da poupança** — `entity/ContaPoupanca.definirMeta()` e `progressoDaMeta()` (barra de progresso na tela).

Para mostrar rendimento sem esperar: no painel do ADM, `POST /painel/rendimentos/simular-mes` (testado em
`PoupancaInvestimentoTest`).

## Testes

`cd api && ./mvnw test` — 87 testes (usa a porta 8081: pare outra API de teste antes). Os principais para mostrar:

| Classe | O que prova |
|---|---|
| `FinancasTest` | cadastro, contas, lançamentos, categorias privadas, filtros, resumo, concorrência |
| `SegurancaTest` | cookie httpOnly/SameSite, CSRF, perfis, bloqueio após 5 senhas erradas |
| `FluxoBancoTest` | depósito, Pix e transferência entre clientes |
| `PoupancaInvestimentoTest` | poupança, meta, investimento, IR no resgate, simulação de 1 mês |
| `ContaTest`, `ContaRemuneradaTest`, `BoletoTest` | regras das entidades, sem banco de dados |

## Esboço

`docs/esboco/index.html` é o esboço das telas do painel do administrador, feito antes da implementação.
