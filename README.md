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
- [ ] 2. Login e perfis (JWT)
- [ ] 3. Painel ADM
- [ ] 4. App do cliente: saldo, extrato, Pix
- [ ] 5. Extras: QR code, boleto, comprovante, testes

## API

Erros de regra de negócio voltam com status **422** e corpo `{"erro": "mensagem"}`; registro inexistente, **404**.

| Método | Rota | O que faz |
|---|---|---|
| GET/POST | `/categorias` | Lista / cria categoria |
| GET/PUT/DELETE | `/categorias/{id}` | Busca / edita / exclui (as de sistema e as em uso são protegidas) |
| GET | `/clientes` | Lista clientes com a conta de cada um |
| POST | `/clientes` | Cadastra cliente (`nome`, `cpf`, `email`) e **abre a conta** |
| GET/PUT | `/clientes/{id}` | Busca / edita nome e e-mail |
| GET | `/contas`, `/contas/{id}` | Lista / busca contas (com saldo) |
| GET | `/contas/{id}/extrato?inicio=AAAA-MM-DD&fim=AAAA-MM-DD` | Extrato (padrão: últimos 30 dias) |
| POST | `/contas/{id}/depositos` | Depósito (`valor`, `descricao`) |
| POST/DELETE | `/contas/{id}/bloqueio` | Bloqueia / desbloqueia |
| DELETE | `/contas/{id}` | Encerra a conta (só com saldo zero; nada é apagado) |
| GET | `/transacoes?limite=50` | Últimas movimentações do banco |
| POST | `/transacoes/transferencias` | Transferência (`contaOrigemId`, `contaDestinoId`, `valor`, `descricao`) |
| GET | `/transacoes/comprovantes/{autenticacao}` | Comprovante (origem e destino) |
| GET/POST | `/pix/contas/{contaId}/chaves` | Lista / cadastra chave (`tipo`: CPF, EMAIL, TELEFONE, ALEATORIA) |
| DELETE | `/pix/contas/{contaId}/chaves/{chaveId}` | Exclui chave |
| GET | `/pix/destinatario?chave=...` | Quem recebe (nome e CPF mascarado) antes de confirmar |
| POST | `/pix/transferencias` | Envia Pix (`contaOrigemId`, `chave`, `valor`, `descricao`) |

Regras importantes:
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
cd api && ./mvnw test         # 15 testes (o teste de fluxo sobe um Postgres descartável)
cd web && npm install && npm start   # site em http://localhost:4300 (etapa 3)
```
