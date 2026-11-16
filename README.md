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
- Pix: enviar por chave ou por código copia e cola, minhas chaves, cobrar com QR code
- Boletos: emitir (com linha digitável e código de barras para imprimir), pagar o de um colega
  (com multa e juros se vencido), cancelar; comprovantes

## Modelo de dados

| Entidade | Campos principais |
|---|---|
| `Usuario` | nome, cpf, email, senha (hash), perfil (`ADMIN` / `CLIENTE`) |
| `Conta` | agência, número, titular, saldo, status |
| `Transacao` | tipo (E/S), valor, data, descrição, categoria, conta, contraparte |
| `Categoria` | descrição |
| `ChavePix` | tipo, valor, conta |
| `Boleto` | linha digitável, código de barras, beneficiário, valor, vencimento, status (`ABERTO` / `PAGO` / `CANCELADO`), pagador |

O saldo é **encapsulado** na entidade `Conta`: só muda pelos métodos `depositar()` e `sacar()`,
que validam valor e saldo disponível.

## Roteiro

- [x] 1. API base: Postgres, entidades, controllers de categoria, conta e transação, transferência com validação
- [x] 2. Login e perfis (JWT)
- [x] 3. Painel ADM (Angular + Material com o tema da Hidra; esboço das telas em `docs/esboco/index.html`)
- [x] 4. App do cliente: saldo, extrato, Pix (enviar e chaves), transferência, comprovante, perfil
- [x] 5. Extras: cobrança Pix com QR code (copia e cola), boleto entre clientes, testes (51)

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
| GET | `/painel` | Total custodiado, clientes, contas por situação, operações de hoje e dos últimos 7 dias |
| GET/POST | `/categorias` | Lista (com quantas transações usam cada uma) / cria categoria |
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
| GET | `/minha-conta/dados` | Nome, e-mail, CPF, agência e conta (tela de perfil) |
| GET | `/minha-conta/extrato?inicio=&fim=` | Extrato |
| GET | `/minha-conta/destinatario?agencia=&numero=` | De quem é a conta, antes de transferir |
| POST | `/minha-conta/transferencias` | `agencia`, `numero`, `valor`, `descricao` |
| GET | `/minha-conta/comprovantes/{autenticacao}` | Comprovante (só se a conta participou) |
| GET/POST | `/minha-conta/pix/chaves` | Lista / cadastra chave (`tipo`: CPF, EMAIL, TELEFONE, ALEATORIA; `valor` — em branco, CPF e e-mail usam os do titular) |
| DELETE | `/minha-conta/pix/chaves/{id}` | Exclui chave |
| GET | `/minha-conta/pix/destinatario?chave=` | Quem recebe (nome e CPF mascarado) antes de confirmar |
| POST | `/minha-conta/pix` | Envia Pix (`chave`, `valor`, `descricao`) |
| POST | `/minha-conta/pix/cobrancas` | Cobrança: `chaveId` (uma das minhas), `valor` (opcional), `descricao` → `codigo` (Pix copia e cola) |
| POST | `/minha-conta/pix/ler-codigo` | `codigo` colado pelo pagador → chave, valor, mensagem e quem recebe |
| GET/POST | `/minha-conta/boletos` | Boletos que emiti / emite (`valor`, `vencimento`, `descricao`) |
| GET/DELETE | `/minha-conta/boletos/{id}` | Busca / cancela (só aberto) |
| GET | `/minha-conta/boletos/consulta?linha=` | Confere a linha digitável (ou o código de barras): beneficiário e total do dia |
| POST | `/minha-conta/boletos/pagamento` | Paga (`linha`) e devolve a transação (comprovante) |

## Segurança

- **Primeiro acesso:** o ADM cadastra o cliente e recebe uma senha provisória (só aparece uma vez) para entregar a ele.
  Com ela o token só permite trocar a senha (`PUT /auth/senha`); a troca devolve o token com acesso completo.
- **Senha:** mínimo 8 caracteres, com letras e números; guardada só como hash **BCrypt**.
- **Tentativas:** 5 senhas erradas seguidas bloqueiam o acesso por 15 minutos (uma nova senha provisória libera).
- E-mail inexistente e senha errada dão a mesma resposta, no mesmo tempo (não revela quem é cliente).
- **Administrador inicial:** criado ao subir a API com o usuário `joao` / senha `moraes` (o login aceita e-mail
  ou nome de usuário). Troque com as variáveis `BANCO_ADMIN_LOGIN` e `BANCO_ADMIN_SENHA`. Essa senha vem da
  configuração do servidor e não passa pela regra de senha forte; se trocar pelo painel, a nova precisa ser forte.
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
- **Pix copia e cola** no padrão BR Code do Banco Central (campos EMV + CRC16): um caractere alterado e o código
  é recusado. Com valor, quem paga não pode mudá-lo. A câmera do celular não lê o QR em `http://IP` (o navegador
  só libera câmera em HTTPS), então na turma o pagamento é pelo código colado.
- **Boleto** no padrão Febraban: código de barras de 44 dígitos (banco fictício `999`, fator de vencimento,
  valor, DV módulo 11) e linha digitável de 47 (DVs módulo 10 que pegam erro de digitação). Vencimento de hoje a
  1 ano. Depois do vencimento: **multa de 2% + juros de 1% ao mês** por dia de atraso. O status é encapsulado na
  entidade (só muda por `registrarPagamento()` e `cancelar()`), o boleto é travado no pagamento (dois pagando ao
  mesmo tempo: o segundo o encontra pago) e ninguém paga o próprio boleto.

## Como rodar

Requisitos: Docker e JDK 21.

```bash
docker compose up -d          # PostgreSQL em localhost:5433
cd api && ./mvnw quarkus:dev  # API em http://localhost:8080
cd api && ./mvnw test         # 51 testes (os de API sobem um Postgres descartável)
cd web && npm install && npm start   # site em http://localhost:4300
```

## Modo turma (colegas acessando do PC deles)

Em desenvolvimento, a API e o site já escutam na rede (não só no `localhost`). Para a turma usar:

1. Todos na **mesma rede Wi-Fi**. Algumas redes públicas (faculdade, lanchonete) isolam os aparelhos entre si;
   nesse caso ninguém alcança ninguém, e a saída é um roteador próprio ou o celular como roteador.
2. O firewall do Windows precisa deixar entrar nas portas **4300** e **8080**. Normalmente basta clicar em
   "Permitir acesso" no aviso que aparece na primeira vez que o Java/Node rodam. Se não aparecer, num PowerShell
   **como administrador**:

   ```powershell
   New-NetFirewallRule -DisplayName "Banco Hidra (site e API)" -Direction Inbound -Protocol TCP -LocalPort 4300,8080 -Action Allow
   ```

   Para desfazer: `Remove-NetFirewallRule -DisplayName "Banco Hidra (site e API)"`.
3. Suba a API e o site normalmente. O **painel do ADM** mostra o endereço para os colegas
   (ex.: `http://192.168.0.15:4300`) com botão de copiar. A mensagem da senha provisória já vai com esse endereço.
4. **Troque a senha do administrador** antes (o painel avisa enquanto ela for a padrão do README).

O site descobre a API pelo próprio endereço (quem abre `http://IP:4300` usa `http://IP:8080`), e a API aceita (CORS)
sites abertos pelo `localhost` ou por IPs de rede local. Se a API reiniciar, todos precisam entrar de novo.

## Túnel (colegas em outra rede: outro Wi-Fi, 4G)

Com o [Cloudflare Quick Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/do-more-with-tunnels/trycloudflare/)
(grátis, sem conta) o site ganha um endereço público `https://xxxx.trycloudflare.com`:

```powershell
winget install --id Cloudflare.cloudflared -e        # uma vez
cloudflared tunnel --url http://localhost:4300       # com a API e o site rodando
```

- O túnel leva só a porta do site; o site fala com a API por `/api`, que o `ng serve` repassa para a porta 8080
  (`web/proxy.conf.json`). O `ng serve` e a API aceitam apenas endereços `*.trycloudflare.com` além da rede local.
- Abra o painel do ADM **pelo link do túnel**: o endereço para os colegas e a mensagem da senha provisória saem com ele.
- O link muda toda vez que o túnel reinicia, e o banco fica acessível pela internet: troque a senha do ADM antes.

## Site (web/)

Angular 22 + Angular Material, seguindo as instruções da disciplina: só CSS, signals, reactive forms,
HTML/CSS separados do TypeScript e a estrutura `pages/<funcionalidade>/{model,services,routes,componentes}`.

- **Tema:** o tema pronto do Material (`azure-blue.css`) com as cores trocadas pelas da Hidra em `src/styles.css`
  (variáveis `--mat-sys-*`), sem Sass.
- **Login único** em `/entrar`: o perfil do token decide a área (`/admin` ou `/app`). Quem entra com senha
  provisória vai para `/primeiro-acesso` e não sai de lá sem criar a própria senha.
- **Painel ADM:** painel inicial, clientes (busca, filtros, cadastro, edição, nova senha provisória), contas
  (extrato por período, chaves Pix, depósito, bloqueio, encerramento), movimentações com comprovante e categorias.
- **App do cliente** (`/app`): início com saldo (dá para esconder), atalhos e últimas transações; extrato agrupado
  por dia; Pix e transferência na mesma tela de envio em 4 passos (para quem → quanto → confirmar → pronto),
  mostrando nome e CPF mascarado de quem recebe antes de confirmar; minhas chaves Pix; cobrar com QR code
  (o campo da chave também aceita o código copia e cola); boletos (emitir, ver/imprimir com código de barras
  desenhado em SVG, pagar e cancelar); comprovante; perfil (no menu do avatar). No celular, as abas ficam embaixo.
- `common/` tem o que é reaproveitado: sessão e interceptor do token, guardas de rota, validações (CPF, senha, valor),
  marca, rodapé e diálogos.
