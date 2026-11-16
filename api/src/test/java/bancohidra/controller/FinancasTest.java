package bancohidra.controller;

import bancohidra.controller.Api.Cliente;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static bancohidra.controller.Api.anonimo;
import static bancohidra.controller.Api.como;
import static bancohidra.controller.Api.sessao;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Gestão financeira: cadastro próprio, várias contas, lançamentos de receita e despesa, categorias
 * próprias, transações filtradas, transferência entre as próprias contas e o resumo da tela inicial.
 */
@QuarkusTest
class FinancasTest {

    // ---------- atalhos ----------

    /** Cadastro pela própria pessoa: devolve a sessão (cookie). */
    private static String cadastrar(String nome) {
        String email = "fin." + System.nanoTime() + "@hidra.com";
        return sessao(anonimo()
                .body(Map.of("nome", nome, "cpf", Api.cpfNovo(), "email", email, "senha", Api.SENHA_CLIENTE))
                .when().post("/auth/cadastro").then().statusCode(201).extract().response());
    }

    private static long abrirConta(String sessao, String nome, String tipo) {
        return como(sessao).body(Map.of("nome", nome, "tipo", tipo))
                .when().post("/minha-conta/contas").then().statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    private static long categoria(String sessao, String descricao) {
        List<Map<String, Object>> lista = como(sessao).when().get("/minha-conta/categorias")
                .then().statusCode(200).extract().jsonPath().getList("$");
        return lista.stream().filter(c -> descricao.equals(c.get("descricao")))
                .map(c -> ((Number) c.get("id")).longValue()).findFirst().orElseThrow();
    }

    private static void lancar(String sessao, long contaId, String tipo, String valor, long categoriaId, int status) {
        como(sessao).body(Map.of("contaId", contaId, "tipo", tipo, "valor", valor, "categoriaId", categoriaId))
                .when().post("/minha-conta/lancamentos").then().statusCode(status);
    }

    private static long principal(String sessao) {
        return como(sessao).when().get("/minha-conta").then().extract().jsonPath().getLong("id");
    }

    // ---------- cadastro próprio ----------

    @Test
    void pessoaSeCadastraEJaEntraComContaCorrente() {
        String sessao = cadastrar("Nara Alves");
        como(sessao).when().get("/minha-conta/contas").then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].nome", equalTo("Conta corrente"))
                .body("[0].tipo", equalTo("CORRENTE"))
                .body("[0].principal", equalTo(true));
    }

    @Test
    void cadastroExigeSenhaForteCpfValidoEEmailNovo() {
        String email = "dup." + System.nanoTime() + "@hidra.com";
        anonimo().body(Map.of("nome", "Teste", "cpf", Api.cpfNovo(), "email", email, "senha", "fraca"))
                .when().post("/auth/cadastro").then().statusCode(422);
        anonimo().body(Map.of("nome", "Teste", "cpf", "123.456.789-00", "email", email, "senha", Api.SENHA_CLIENTE))
                .when().post("/auth/cadastro").then().statusCode(422).body("erro", equalTo("CPF inválido"));
        anonimo().body(Map.of("nome", "Teste", "cpf", Api.cpfNovo(), "email", email, "senha", Api.SENHA_CLIENTE))
                .when().post("/auth/cadastro").then().statusCode(201);
        anonimo().body(Map.of("nome", "Outro", "cpf", Api.cpfNovo(), "email", email, "senha", Api.SENHA_CLIENTE))
                .when().post("/auth/cadastro").then().statusCode(422);
    }

    @Test
    void cadastroNaoAceitaVirarAdministrador() {
        String email = "adm." + System.nanoTime() + "@hidra.com";
        String sessao = sessao(anonimo()
                .body(Map.of("nome", "Esperto", "cpf", Api.cpfNovo(), "email", email, "senha", Api.SENHA_CLIENTE,
                        "perfil", "ADMIN"))
                .when().post("/auth/cadastro").then().statusCode(201)
                .body("usuario.perfil", equalTo("CLIENTE"))
                .extract().response());
        como(sessao).when().get("/clientes").then().statusCode(403);
    }

    // ---------- contas ----------

    @Test
    void abreRenomeiaEEncerraContas() {
        String sessao = cadastrar("Otávio Luz");
        long poupanca = abrirConta(sessao, "Reserva", "POUPANCA");
        como(sessao).body(Map.of("nome", "Reserva de emergência", "tipo", "POUPANCA"))
                .when().put("/minha-conta/contas/" + poupanca).then().statusCode(200)
                .body("nome", equalTo("Reserva de emergência"));

        // A principal não pode ser encerrada; a outra, com saldo zero, pode
        como(sessao).when().delete("/minha-conta/contas/" + principal(sessao)).then().statusCode(422);
        como(sessao).when().delete("/minha-conta/contas/" + poupanca).then().statusCode(200)
                .body("status", equalTo("ENCERRADA"));
    }

    @Test
    void naoEncerraContaComSaldo() {
        String sessao = cadastrar("Paula Reis");
        long carteira = abrirConta(sessao, "Carteira", "CARTEIRA");
        lancar(sessao, carteira, "RECEITA", "10.00", categoria(sessao, "Salário"), 201);
        como(sessao).when().delete("/minha-conta/contas/" + carteira).then().statusCode(422)
                .body("erro", containsString("saldo zero"));
    }

    @Test
    void naoMexeNaContaDeOutroCliente() {
        String ana = cadastrar("Ana Prata");
        String beto = cadastrar("Beto Prata");
        long contaDaAna = principal(ana);
        long salario = categoria(beto, "Salário");

        lancar(beto, contaDaAna, "RECEITA", "100.00", salario, 404);
        como(beto).body(Map.of("nome", "Minha agora", "tipo", "CORRENTE"))
                .when().put("/minha-conta/contas/" + contaDaAna).then().statusCode(404);
        como(beto).when().delete("/minha-conta/contas/" + contaDaAna).then().statusCode(404);
        como(beto).queryParam("contaId", contaDaAna).when().get("/minha-conta/transacoes").then().statusCode(404);
        como(beto).body(Map.of("contaOrigemId", contaDaAna, "agencia", "0001", "numero", "000000-0", "valor", 1))
                .when().post("/minha-conta/transferencias").then().statusCode(404);
    }

    // ---------- lançamentos ----------

    @Test
    void receitaEntraEDespesaSaiComSaldo() {
        String sessao = cadastrar("Rita Campos");
        long conta = principal(sessao);
        lancar(sessao, conta, "RECEITA", "1000.00", categoria(sessao, "Salário"), 201);
        lancar(sessao, conta, "DESPESA", "250.50", categoria(sessao, "Alimentação"), 201);
        // Despesa maior que o saldo é recusada
        lancar(sessao, conta, "DESPESA", "5000.00", categoria(sessao, "Moradia"), 422);
        como(sessao).when().get("/minha-conta").then().body("saldo", equalTo(749.5f));
    }

    /**
     * 10 despesas de R$ 20 ao mesmo tempo numa conta com R$ 100: exatamente 5 passam e o saldo termina
     * em zero. Sem o lock que relê o saldo (MovimentacaoService.travar), duas despesas poderiam partir do
     * mesmo saldo antigo e uma "apagar" a outra.
     */
    @Test
    void despesasSimultaneasNaoPassamDoSaldo() throws Exception {
        String sessao = cadastrar("Zeca Concorrente");
        long conta = principal(sessao);
        long moradia = categoria(sessao, "Moradia");
        lancar(sessao, conta, "RECEITA", "100.00", categoria(sessao, "Salário"), 201);

        var tarefas = new java.util.ArrayList<java.util.concurrent.Callable<Integer>>();
        for (int i = 0; i < 10; i++) {
            tarefas.add(() -> como(sessao)
                    .body(Map.of("contaId", conta, "tipo", "DESPESA", "valor", "20.00", "categoriaId", moradia))
                    .when().post("/minha-conta/lancamentos").then().extract().statusCode());
        }
        var executor = java.util.concurrent.Executors.newFixedThreadPool(10);
        try {
            long aceitas = 0;
            for (var resultado : executor.invokeAll(tarefas)) {
                if (resultado.get() == 201) {
                    aceitas++;
                }
            }
            assertEquals(5, aceitas);
        } finally {
            executor.shutdown();
        }
        como(sessao).when().get("/minha-conta").then().body("saldo", equalTo(0.0f));
    }

    @Test
    void lancamentoNaoUsaCategoriaDeSistemaNemValorInvalido() {
        String sessao = cadastrar("Saulo Dias");
        long conta = principal(sessao);
        lancar(sessao, conta, "RECEITA", "10.00", categoria(sessao, "Pix"), 422);
        lancar(sessao, conta, "RECEITA", "-10.00", categoria(sessao, "Salário"), 422);
        lancar(sessao, conta, "RECEITA", "10.001", categoria(sessao, "Salário"), 422);
    }

    // ---------- categorias próprias ----------

    @Test
    void categoriasPropriasSaoPrivadas() {
        String tais = cadastrar("Taís Rocha");
        String ugo = cadastrar("Ugo Rocha");
        long academia = como(tais).body(Map.of("descricao", "Academia"))
                .when().post("/minha-conta/categorias").then().statusCode(201)
                .body("propria", equalTo(true)).extract().jsonPath().getLong("id");

        // Nome repetido (mesmo com outra caixa) e nome de categoria padrão são recusados
        como(tais).body(Map.of("descricao", "academia")).when().post("/minha-conta/categorias").then().statusCode(422);
        como(tais).body(Map.of("descricao", "Lazer")).when().post("/minha-conta/categorias").then().statusCode(422);

        // O outro cliente não vê, não usa, não edita e não exclui
        como(ugo).when().get("/minha-conta/categorias").then().body("descricao", not(hasItem("Academia")));
        lancar(ugo, principal(ugo), "RECEITA", "10.00", academia, 404);
        como(ugo).body(Map.of("descricao", "Hackeada")).when().put("/minha-conta/categorias/" + academia)
                .then().statusCode(404);
        como(ugo).when().delete("/minha-conta/categorias/" + academia).then().statusCode(404);
        // ...e pode ter a própria "Academia"
        como(ugo).body(Map.of("descricao", "Academia")).when().post("/minha-conta/categorias").then().statusCode(201);

        // Em uso não exclui; renomear pode
        lancar(tais, principal(tais), "RECEITA", "50.00", academia, 201);
        como(tais).when().delete("/minha-conta/categorias/" + academia).then().statusCode(422);
        como(tais).body(Map.of("descricao", "Academia e esportes")).when().put("/minha-conta/categorias/" + academia)
                .then().statusCode(200).body("descricao", equalTo("Academia e esportes"));

        // O administrador só vê as globais
        como(Api.admin()).when().get("/categorias").then().body("descricao", not(hasItem("Academia e esportes")));
        como(Api.admin()).when().get("/categorias/" + academia).then().statusCode(404);
    }

    @Test
    void categoriaPadraoNaoPodeSerAlteradaPeloCliente() {
        String sessao = cadastrar("Vera Melo");
        long lazer = categoria(sessao, "Lazer");
        como(sessao).body(Map.of("descricao", "Minha")).when().put("/minha-conta/categorias/" + lazer)
                .then().statusCode(404);
        como(sessao).when().delete("/minha-conta/categorias/" + lazer).then().statusCode(404);
    }

    // ---------- transferência entre as próprias contas, filtros e resumo ----------

    @Test
    void transfereEntreAsPropriasContasEOResumoNaoContaComoDespesa() {
        String sessao = cadastrar("Wagner Sá");
        long corrente = principal(sessao);
        JsonPath poupanca = como(sessao).body(Map.of("nome", "Poupança", "tipo", "POUPANCA"))
                .when().post("/minha-conta/contas").then().statusCode(201).extract().jsonPath();

        lancar(sessao, corrente, "RECEITA", "3000.00", categoria(sessao, "Salário"), 201);
        lancar(sessao, corrente, "DESPESA", "800.00", categoria(sessao, "Moradia"), 201);
        lancar(sessao, corrente, "DESPESA", "200.00", categoria(sessao, "Alimentação"), 201);
        como(sessao).body(Map.of("contaOrigemId", corrente, "agencia", poupanca.getString("agencia"),
                        "numero", poupanca.getString("numero"), "valor", "500.00", "descricao", "Guardar"))
                .when().post("/minha-conta/transferencias").then().statusCode(201);

        JsonPath resumo = como(sessao).when().get("/minha-conta/resumo").then().statusCode(200)
                .body("contas", hasSize(2))
                .extract().jsonPath();
        assertEquals(2000.0f, resumo.getFloat("saldoTotal"));
        assertEquals(1500.0f, resumo.getFloat("contas[0].saldo"));
        assertEquals(500.0f, resumo.getFloat("contas[1].saldo"));
        assertEquals(3000.0f, resumo.getFloat("receitasMes"), "transferência interna não é receita");
        assertEquals(1000.0f, resumo.getFloat("despesasMes"), "transferência interna não é despesa");
        assertEquals("Moradia", resumo.getString("despesasPorCategoria[0].categoria"));
        assertEquals(800.0f, resumo.getFloat("despesasPorCategoria[0].total"));
    }

    @Test
    void filtraTransacoesPorContaCategoriaETipo() {
        String sessao = cadastrar("Xênia Lima");
        long corrente = principal(sessao);
        long carteira = abrirConta(sessao, "Carteira", "CARTEIRA");
        long alimentacao = categoria(sessao, "Alimentação");
        long salario = categoria(sessao, "Salário");

        lancar(sessao, corrente, "RECEITA", "2000.00", salario, 201);
        lancar(sessao, corrente, "DESPESA", "100.00", alimentacao, 201);
        lancar(sessao, carteira, "RECEITA", "50.00", salario, 201);
        lancar(sessao, carteira, "DESPESA", "20.00", alimentacao, 201);

        como(sessao).when().get("/minha-conta/transacoes").then().statusCode(200).body("$", hasSize(4));
        como(sessao).queryParam("contaId", carteira).when().get("/minha-conta/transacoes")
                .then().body("$", hasSize(2)).body("contaId", everyItem(equalTo((int) carteira)));
        como(sessao).queryParam("categoriaId", alimentacao).when().get("/minha-conta/transacoes")
                .then().body("$", hasSize(2)).body("categoria", everyItem(equalTo("Alimentação")));
        como(sessao).queryParam("contaId", corrente).queryParam("categoriaId", alimentacao)
                .queryParam("tipo", "SAIDA").when().get("/minha-conta/transacoes")
                .then().body("$", hasSize(1)).body("[0].valor", equalTo(100.0f));
        // Período invertido é recusado
        como(sessao).queryParam("inicio", "2026-12-31").queryParam("fim", "2026-01-01")
                .when().get("/minha-conta/transacoes").then().statusCode(422);
    }

    @Test
    void clienteVeComprovanteDeQualquerContaDele() {
        Cliente cli = Api.novoCliente("Yuri Neves");
        long poupanca = abrirConta(cli.token(), "Poupança", "POUPANCA");
        como(Api.admin()).body(Map.of("valor", 100)).when().post("/contas/" + poupanca + "/depositos")
                .then().statusCode(201);
        String autenticacao = como(cli.token())
                .body(Map.of("contaOrigemId", poupanca, "agencia", cli.agencia(), "numero", cli.numero(),
                        "valor", "30.00"))
                .when().post("/minha-conta/transferencias").then().statusCode(201)
                .extract().path("autenticacao");
        como(cli.token()).when().get("/minha-conta/comprovantes/" + autenticacao).then().statusCode(200);
    }
}
