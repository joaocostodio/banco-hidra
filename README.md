# Banco Hidra

Gestão financeira e internet banking acadêmico, com a identidade visual da Hidra (fundo escuro, verde e prata).
Projeto da disciplina de **Programação Orientada a Objetos** — EC04 / 2026 — SETREM.

> Todo o dinheiro deste banco é fictício. Projeto para fins de estudo.

## O que o sistema faz

**Gestão financeira (cliente)** — o que a atividade pede:
- **Cadastro próprio** ("Criar conta") e login com **JWT em cookie httpOnly**
- **Contas**: cada pessoa tem várias (corrente, poupança, carteira, investimento): abrir, renomear, encerrar.
  Cada tipo é uma **subclasse** com regras próprias (herança e polimorfismo, ver "Herança das contas")
- **Lançamentos** de receita e despesa, com **categoria**, em qualquer uma das contas
- **Categorias**: as padrão do banco (Alimentação, Moradia, Salário...) e as **próprias** de cada cliente
- **Transações filtradas por conta e por categoria** (e por tipo e período)
- **Transferência entre contas** (entre as minhas ou para outra pessoa)
- **Tela inicial com saldo por conta e geral**, receitas × despesas do mês e para onde foi o dinheiro

**Extras de banco**: Pix (chaves, envio, cobrança com QR code / copia e cola), boletos (emitir, pagar com multa e
juros, imprimir com código de barras), comprovantes e um **painel do administrador** (clientes, contas, depósitos,
bloqueio, categorias padrão, movimentações).

## Como executar

Requisitos: **Docker** (para o PostgreSQL), **JDK 21** e **Node.js 22+**.

```bash
# 1. Banco de dados (PostgreSQL 17 em localhost:5433)
docker compose up -d

# 2. API (Quarkus) em http://localhost:8080 — cria as tabelas e o administrador sozinha
cd api
./mvnw quarkus:dev          # no Windows: mvnw.cmd quarkus:dev

# 3. Site (Angular) em http://localhost:4300 — em outro terminal
cd web
npm install
npm start
```

Abra **http://localhost:4300** e clique em **Criar minha conta**, ou entre como administrador
(`joao` / `moraes`, configurável — ver Segurança).

Roteiro da apresentação, com onde está cada requisito no código: [docs/apresentacao.md](docs/apresentacao.md).

Testes da API (sobem um PostgreSQL descartável sozinhos, pelo Docker):

```bash
cd api
./mvnw test                 # 87 testes (usa a porta 8081: pare outra API de teste antes)
```

Compilar para produção: `cd api && ./mvnw package` (gera `target/quarkus-app/quarkus-run.jar`) e
`cd web && npm run build` (gera `dist/web`).

## Estrutura

```
banco-hidra/
├── api/                 Quarkus + Hibernate Panache + PostgreSQL   (porta 8080)
│   └── src/main/java/bancohidra/
│       ├── entity/      entidades (Conta e filhas, Transacao, Categoria, Usuario, Boleto, ChavePix)
│       ├── service/     regras de negócio (MovimentacaoService, FinancasService, ...)
│       ├── controller/  rotas REST
│       ├── dto/         o que entra e sai da API (records)
│       └── security/    cookie de sessão, proteção CSRF, usuário logado
├── web/                 Angular 22 + Angular Material              (porta 4300)
│   └── src/app/pages/   uma pasta por funcionalidade: model, services, routes, componentes
└── docker-compose.yml   PostgreSQL para desenvolvimento
```

Baseado nos projetos de aula [finance-api](https://github.com/tiagoseibel/finance-api) e
[finance-ui](https://github.com/tiagoseibel/finance-ui).

## Modelo de dados

| Entidade | Campos principais |
|---|---|
| `Usuario` | nome, cpf, email, senha (hash BCrypt), perfil (`ADMIN` / `CLIENTE`) |
| `Conta` (abstrata) | nome, tipo (`CORRENTE` / `POUPANCA` / `CARTEIRA` / `INVESTIMENTO`), agência, número, titular, saldo, status. Uma tabela só (`SINGLE_TABLE`), a coluna `tipo` diz a subclasse |
| `ContaPoupanca` | meta (valor, descrição, data) e último rendimento |
| `ContaInvestimento` | produto (`CDB` / `TESOURO_SELIC` / `LCI`), valor aplicado, data média das aplicações |
| `Transacao` | tipo (entrada/saída), valor, data, descrição, categoria, conta, contraparte, autenticação |
| `Categoria` | descrição, sistema, dono (nulo = padrão do banco; preenchido = categoria própria do cliente) |
| `ChavePix` | tipo, valor, conta |
| `Boleto` | linha digitável, código de barras, beneficiário, valor, vencimento, status, pagador |

**Encapsulamento (POO):** o saldo da `Conta` é privado e só muda por `depositar()` e `sacar()`, que validam
valor, situação da conta e saldo. O status do `Boleto` só muda por `registrarPagamento()` e `cancelar()`.
A senha do `Usuario` só é gravada como hash, e a própria entidade controla as tentativas erradas.

A **primeira conta** de cada cliente é a principal: é a das chaves Pix e dos boletos, e não pode ser encerrada.

### Herança das contas

```
Conta (abstrata)
├── ContaCorrente
├── ContaCarteira
└── ContaRemunerada (abstrata: método-modelo render())
    ├── ContaPoupanca       0,5% ao mês no "aniversário" + meta com barra de progresso
    └── ContaInvestimento   CDB, Tesouro Selic ou LCI, rendimento diário, IR no resgate
```

- **Polimorfismo**: `Conta` tem "ganchos" que as filhas sobrescrevem (`exigirPodeReceberDe`, `exigirPodeEnviarPara`,
  `aceitaLancamento`, `prepararSaida`, `aoReceber`). O `MovimentacaoService` só conversa com `Conta` e nunca
  pergunta o tipo: quem decide é a própria conta.
- **Método-modelo**: `ContaRemunerada.render()` é igual para todas (acha a data-base, credita, guarda a data); a
  filha só diz *quanto* rende em `calcularRendimento()`.
- **Fábrica**: `Conta.nova(tipo, produto)` cria a subclasse certa.

## Segurança

- **Sessão em cookie httpOnly**: o JWT (assinado com RSA, validade de 1 hora) vai no cookie `hidra_sessao`
  com `HttpOnly` (o JavaScript não lê: um script injetado não rouba a sessão), `SameSite=Strict` (outro site não
  consegue usá-lo) e `Secure` quando o acesso é por HTTPS. O token nunca aparece no corpo da resposta nem no
  `localStorage`. `POST /auth/sair` apaga o cookie.
- **CSRF**: além do `SameSite=Strict`, toda requisição que muda dados exige o cabeçalho `X-Requested-With`
  (formulários e imagens de outros sites não conseguem mandá-lo) e o CORS só aceita as origens do próprio site.
- **Isolamento entre clientes**: tudo em `/minha-conta` parte do usuário do token. Conta, categoria, transação
  ou comprovante de outra pessoa respondem **404** (nem confirma que existe).
- **Perfis**: o cadastro próprio sempre cria `CLIENTE`; as rotas do administrador exigem `ADMIN`.
- **Senha**: mínimo 8 caracteres, com letras e números (máximo 72, limite do BCrypt); guardada só como hash.
- **Tentativas**: 5 senhas erradas seguidas bloqueiam o acesso por 15 minutos. E-mail inexistente e senha
  errada dão a mesma resposta, no mesmo tempo (não revela quem é cliente).
- **Concorrência**: toda operação que mexe em saldo trava a linha da conta no banco (`SELECT ... FOR UPDATE`) e
  relê o saldo antes de alterar. Duas despesas ao mesmo tempo nunca passam do saldo (há teste para isso).
- **SQL**: as consultas usam só parâmetros (JPQL/Panache), nunca texto do usuário concatenado.
- **Administrador inicial**: criado ao subir a API com o usuário `joao` / senha `moraes`. Troque com as
  variáveis `BANCO_ADMIN_LOGIN` e `BANCO_ADMIN_SENHA`. Essa senha vem da configuração do servidor e não passa
  pela regra de senha forte; se trocar pelo painel, a nova precisa ser forte.
- **Chaves do token**: em dev e nos testes o Quarkus gera um par RSA na memória (todo login cai quando a API
  reinicia). Em produção gere chaves próprias, que **não** vão para o Git:

  ```bash
  mkdir -p api/chaves
  openssl genrsa -out api/chaves/privada.pem 2048
  openssl rsa -in api/chaves/privada.pem -pubout -out api/chaves/publica.pem
  ```

## Regras de negócio

- **Saldo**: valor > 0, no máximo 2 casas, conta ativa e saldo suficiente para despesas e transferências.
- **Lançamento**: só em conta minha e ativa; categoria padrão ou própria (as de sistema — Pix, Transferência... —
  são do banco).
- **Transferência atômica**: trava as duas contas (sempre na mesma ordem, para evitar deadlock), debita, credita
  e grava as duas pontas com a mesma autenticação. Se algo falhar, nada é gravado.
- **Resumo do mês**: transferência entre as próprias contas não conta como receita nem despesa.
- **Contas**: até 10 abertas por cliente; encerrar só com saldo zero (o histórico continua no extrato).
- **Poupança**: rende 0,5% ao mês (juros compostos) no aniversário da conta. Pode ter uma **meta** ("guardar
  R$ 8.000 para a viagem até dezembro"); o progresso é o saldo dividido pela meta.
- **Investimento**: rende todo dia, `saldo × ((1 + taxa anual)^(dias/365) − 1)`. Produtos (taxas fictícias):
  CDB 110% do CDI, Tesouro Selic e LCI 95% do CDI. Só recebe aplicação e só faz resgate com contas do **mesmo
  dono**, e **não aceita lançamento manual** de receita ou despesa.
- **Imposto de Renda no resgate**: incide só sobre a parte do lucro resgatada, pela tabela regressiva (até 180 dias
  22,5%, até 360 dias 20%, até 720 dias 17,5%, depois 15%). A LCI é isenta.
- **Rendimentos**: uma tarefa agendada (`RendimentoService`) paga todo dia às 00:10. O administrador pode simular a
  passagem de 1 mês em `POST /painel/rendimentos/simular-mes`.
- **Categorias próprias**: até 30 por cliente; nome único entre as que ele enxerga; em uso não pode ser excluída.
- CPF validado pelos dígitos verificadores; número da conta sorteado (6 dígitos + DV módulo 11).
- **Pix copia e cola** no padrão BR Code do Banco Central (campos EMV + CRC16).
- **Boleto** no padrão Febraban (44/47 dígitos, DVs módulo 10 e 11); vencido: multa de 2% + juros de 1% ao mês.

## API

Erros voltam com corpo `{"erro": "mensagem"}`: **422** regra de negócio, **404** não encontrado,
**401** sem login (ou login recusado), **403** perfil sem permissão ou requisição sem `X-Requested-With`.

### Sessão (todos)

| Método | Rota | O que faz |
|---|---|---|
| POST | `/auth/cadastro` | `nome`, `cpf`, `email`, `senha` → cria o cliente com a conta corrente e já abre a sessão |
| POST | `/auth/login` | `email` (ou usuário do ADM), `senha` → grava o cookie; responde `expiraEm`, `trocarSenha`, usuário |
| POST | `/auth/sair` | Apaga o cookie |
| GET | `/auth/eu` | Quem está logado |
| PUT | `/auth/senha` | `senhaAtual`, `novaSenha` → sessão nova |

### Cliente (perfil `CLIENTE`)

| Método | Rota | O que faz |
|---|---|---|
| GET | `/minha-conta/resumo` | Saldo de cada conta e geral, receitas/despesas do mês, despesas por categoria |
| GET/POST | `/minha-conta/contas` | Minhas contas / abre outra (`nome`, `tipo`) |
| PUT/DELETE | `/minha-conta/contas/{id}` | Renomeia / encerra |
| PUT/DELETE | `/minha-conta/contas/{id}/meta` | Define / remove a meta da poupança (`valor`, `descricao`, `data`) |
| POST | `/minha-conta/lancamentos` | `contaId`, `tipo` (`RECEITA`/`DESPESA`), `valor`, `categoriaId`, `descricao` |
| GET | `/minha-conta/transacoes?contaId=&categoriaId=&tipo=&inicio=&fim=` | Transações de todas as minhas contas, com filtros |
| GET/POST | `/minha-conta/categorias` | Padrão + minhas / cria categoria própria |
| PUT/DELETE | `/minha-conta/categorias/{id}` | Renomeia / exclui (só as próprias) |
| POST | `/minha-conta/transferencias` | `contaOrigemId` (opcional), `agencia`, `numero`, `valor`, `descricao` |
| GET | `/minha-conta/destinatario?agencia=&numero=` | De quem é a conta, antes de transferir |
| GET | `/minha-conta`, `/minha-conta/dados` | Conta principal / dados do perfil |
| GET | `/minha-conta/comprovantes/{autenticacao}` | Comprovante (só se uma conta minha participou) |
| | `/minha-conta/pix/...`, `/minha-conta/boletos/...` | Pix (chaves, envio, cobrança) e boletos |

### Administrador (perfil `ADMIN`)

| Método | Rota | O que faz |
|---|---|---|
| GET | `/painel`, `/painel/rede` | Números do banco; endereço para a turma |
| GET/POST/PUT/DELETE | `/categorias` | Categorias padrão (as próprias dos clientes são privadas) |
| GET/POST/PUT | `/clientes` | Lista, cadastra (com senha provisória) e edita clientes |
| GET/POST/DELETE | `/contas/...` | Extrato, depósito, bloqueio e encerramento de contas |
| GET | `/transacoes` | Últimas movimentações do banco |
| POST | `/painel/rendimentos/simular-mes` | Paga os rendimentos como se tivesse passado 1 mês |

## Site (web/)

Angular 22 + Angular Material, seguindo as instruções da disciplina: só CSS, signals, reactive forms,
HTML/CSS separados do TypeScript e a estrutura `pages/<funcionalidade>/{model,services,routes,componentes}`.

- O site chama a API sempre por `/api` no próprio endereço; o `ng serve` repassa para a porta 8080
  (`web/proxy.conf.json`). Por isso o cookie de sessão funciona igual pelo `localhost`, pelo IP ou pelo túnel.
- **Cliente** (`/app`): Início (saldo geral e por conta, o mês, gastos por categoria), Transações (filtros),
  Contas, Lançar receita/despesa, Categorias, Transferir, Pix, Boletos, Perfil.
- **Administrador** (`/admin`): painel, clientes, contas, movimentações e categorias padrão.

## Acesso pela turma

- **Mesma rede**: os colegas abrem `http://IP-DO-PC:4300` (o painel do ADM mostra o endereço). O firewall do
  Windows precisa liberar a porta **4300** (a API fica só atrás do site):
  `New-NetFirewallRule -DisplayName "Banco Hidra" -Direction Inbound -Protocol TCP -LocalPort 4300 -Action Allow`
- **Outra rede (túnel)**: com o [Cloudflare Quick Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/do-more-with-tunnels/trycloudflare/)
  (grátis, sem conta): `cloudflared tunnel --url http://localhost:4300`. O link `https://xxxx.trycloudflare.com`
  muda a cada vez; o painel do ADM detecta o túnel e mostra o link. Com o banco na internet, troque a senha do ADM.
