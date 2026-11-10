# Banco Hidra

Internet banking acadêmico com a identidade visual da Hidra (fundo escuro, verde e prata).
Projeto da disciplina de **Programação Orientada a Objetos** — EC04 / 2026 — SETREM.

> Todo o dinheiro deste banco é fictício. Projeto para fins de estudo.

## Estrutura

```
banco-hidra/
├── api/                 Quarkus + Hibernate Panache + PostgreSQL   (porta 8080)
├── web/                 Angular 22 + Angular Material              (porta 4300)
│   ├── /admin           painel do administrador
│   └── /app             internet banking do cliente
└── docker-compose.yml   PostgreSQL para desenvolvimento
```

Baseado nos projetos de aula [finance-api](https://github.com/tiagoseibel/finance-api) e
[finance-ui](https://github.com/tiagoseibel/finance-ui).

## Funcionalidades

**Painel ADM**
- Dashboard: clientes, contas, total custodiado e últimas movimentações
- Clientes: criar (já abre a conta), editar, listar
- Contas: extrato, bloquear/desbloquear, depósito
- Categorias de transação

**App do cliente**
- Saldo (com opção de ocultar) e últimas transações
- Extrato com filtros
- Pix: enviar por chave, minhas chaves, cobrar com QR code
- Pagamento de boleto (simulado) e comprovantes

## Modelo de dados

| Entidade | Campos principais |
|---|---|
| `Usuario` | nome, cpf, email, senha (hash), perfil (`ADMIN` / `CLIENTE`) |
| `Conta` | agência, número, titular, saldo, status |
| `Transacao` | tipo (E/S), valor, data, descrição, categoria, conta, contraparte |
| `Categoria` | descrição |
| `ChavePix` | tipo, valor, conta |

O saldo é **encapsulado** na entidade `Conta`: só muda pelos métodos `depositar()` e `sacar()`,
que validam valor e saldo disponível.

## Roteiro

- [x] 1. API base: Postgres, entidades, controllers de categoria, conta e transação, transferência com validação
- [x] 2. Login e perfis (JWT)
- [ ] 3. Painel ADM
- [ ] 4. App do cliente: saldo, extrato, Pix
- [ ] 5. Extras: QR code, boleto, comprovante, testes

## API

Erros voltam com corpo `{"erro": "mensagem"}`: **422** regra de negócio, **404** não encontrado,
**401** sem login (ou login recusado), **403** perfil sem permissão.

Toda rota, exceto o login, exige o cabeçalho `Authorization: Bearer <token>`.

### Login (todos)

| Método | Rota | O que faz |
|---|---|---|
| POST | `/auth/login` | `email`, `senha` → `token` (1 h), `trocarSenha`, dados do usuário |
| GET | `/auth/eu` | Quem está logado |
| PUT | `/auth/senha` | `senhaAtual`, `novaSenha` → devolve um token novo |

### Administrador (perfil `ADMIN`)

| Método | Rota | O que faz |
|---|---|---|
| GET/POST | `/categorias` | Lista / cria categoria |
| GET/PUT/DELETE | `/categorias/{id}` | Busca / edita / exclui (as de sistema e as em uso são protegidas) |
| GET | `/clientes` | Lista clientes com a conta de cada um |
| POST | `/clientes` | Cadastra cliente (`nome`, `cpf`, `email`), **abre a conta** e devolve a **senha provisória** |
| GET/PUT | `/clientes/{id}` | Busca / edita nome e e-mail |
| POST | `/clientes/{id}/senha-provisoria` | Gera outra senha provisória (esqueceu a senha / acesso bloqueado) |
| GET | `/contas`, `/contas/{id}` | Lista / busca contas (com saldo) |
| GET | `/contas/{id}/extrato?inicio=AAAA-MM-DD&fim=AAAA-MM-DD` | Extrato (padrão: últimos 30 dias; máx. 1 ano) |
| GET | `/contas/{id}/chaves-pix` | Chaves Pix da conta |
| POST | `/contas/{id}/depositos` | Depósito (`valor`, `descricao`) |
| POST/DELETE | `/contas/{id}/bloqueio` | Bloqueia / desbloqueia |
| DELETE | `/contas/{id}` | Encerra a conta (só com saldo zero; nada é apagado) |
| GET | `/transacoes?limite=50` | Últimas movimentações do banco |
| GET | `/transacoes/comprovantes/{autenticacao}` | Qualquer comprovante |

O administrador não transfere dinheiro de clientes: só deposita. Transferências e Pix são do próprio cliente.

### Cliente (perfil `CLIENTE`) — a conta é sempre a do token

| Método | Rota | O que faz |
|---|---|---|
| GET | `/minha-conta` | Conta e saldo |
| GET | `/minha-conta/extrato?inicio=&fim=` | Extrato |
| GET | `/minha-conta/destinatario?agencia=&numero=` | De quem é a conta, antes de transferir |
| POST | `/minha-conta/transferencias` | `agencia`, `numero`, `valor`, `descricao` |
| GET | `/minha-conta/comprovantes/{autenticacao}` | Comprovante (só se a conta participou) |
| GET/POST | `/minha-conta/pix/chaves` | Lista / cadastra chave (`tipo`: CPF, EMAIL, TELEFONE, ALEATORIA; `valor`) |
| DELETE | `/minha-conta/pix/chaves/{id}` | Exclui chave |
| GET | `/minha-conta/pix/destinatario?chave=` | Quem recebe (nome e CPF mascarado) antes de confirmar |
| POST | `/minha-conta/pix` | Envia Pix (`chave`, `valor`, `descricao`) |

## Segurança

- **Primeiro acesso:** o ADM cadastra o cliente e recebe uma senha provisória (só aparece uma vez) para entregar a ele.
  Com ela o token só permite trocar a senha (`PUT /auth/senha`); a troca devolve o token com acesso completo.
- **Senha:** mínimo 8 caracteres, com letras e números; guardada só como hash **BCrypt**.
- **Tentativas:** 5 senhas erradas seguidas bloqueiam o acesso por 15 minutos (uma nova senha provisória libera).
- E-mail inexistente e senha errada dão a mesma resposta, no mesmo tempo (não revela quem é cliente).
- **Administrador inicial:** criado na primeira execução com `admin@hidra.com` / `hidra123`
  (troque com a variável `BANCO_ADMIN_SENHA`).
- **Chaves do token:** em dev e nos testes o Quarkus gera um par RSA na memória (todo login cai quando a API reinicia).
  Em produção gere chaves próprias, que **não** vão para o Git:

  ```bash
  mkdir -p api/chaves
  openssl genrsa -out api/chaves/privada.pem 2048
  openssl rsa -in api/chaves/privada.pem -pubout -out api/chaves/publica.pem
  ```

## Regras de negócio
- **Saldo encapsulado** em `Conta` (`depositar`/`sacar`): valor > 0, no máximo 2 casas, conta ativa, saldo suficiente.
- **Transferência atômica**: trava as duas contas (sempre na mesma ordem, para evitar deadlock), debita, credita e
  grava as duas pontas com a mesma autenticação. Se algo falhar, nada é gravado.
- CPF validado pelos dígitos verificadores; chave Pix CPF só pode ser o CPF do titular; até 5 chaves por conta.
- Número da conta sorteado (6 dígitos + DV módulo 11), como nos bancos reais.

## Como rodar

Requisitos: Docker e JDK 21.

```bash
docker compose up -d          # PostgreSQL em localhost:5433
cd api && ./mvnw quarkus:dev  # API em http://localhost:8080
cd api && ./mvnw test         # 24 testes (os de API sobem um Postgres descartável)
cd web && npm install && npm start   # site em http://localhost:4300 (etapa 3)
```
