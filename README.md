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

- [ ] 1. API base: Postgres, entidades, controllers de categoria, conta e transação, transferência com validação
- [ ] 2. Login e perfis (JWT)
- [ ] 3. Painel ADM
- [ ] 4. App do cliente: saldo, extrato, Pix
- [ ] 5. Extras: QR code, boleto, comprovante, testes

## Como rodar

```bash
docker compose up -d          # PostgreSQL em localhost:5432
cd api && ./mvnw quarkus:dev  # API em http://localhost:8080 (requer JDK 21)
cd web && npm install && npm start   # site em http://localhost:4300
```
