package bancohidra.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static bancohidra.controller.Api.anonimo;
import static bancohidra.controller.Api.como;
import static bancohidra.controller.Api.sessao;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Poupança (rendimento mensal e meta) e investimento (produtos, aplicação, resgate com IR). */
@QuarkusTest
class PoupancaInvestimentoTest {

    private static String cadastrar(String nome) {
        String email = "rend." + System.nanoTime() + "@hidra.com";
        return sessao(anonimo()
                .body(Map.of("nome", nome, "cpf", Api.cpfNovo(), "email", email, "senha", Api.SENHA_CLIENTE))
                .when().post("/auth/cadastro").then().statusCode(201).extract().response());
    }

    private static JsonPath abrir(String sessao, Map<String, Object> corpo) {
        return como(sessao).body(corpo).when().post("/minha-conta/contas").then().statusCode(201)
                .extract().jsonPath();
    }

    private static JsonPath principal(String sessao) {
        return como(sessao).when().get("/minha-conta").then().extract().jsonPath();
    }

    private static void receita(String sessao, long contaId, String valor) {
        List<Map<String, Object>> cats = como(sessao).when().get("/minha-conta/categorias").then().extract()
                .jsonPath().getList("$");
        long salario = cats.stream().filter(c -> "Salário".equals(c.get("descricao")))
                .map(c -> ((Number) c.get("id")).longValue()).findFirst().orElseThrow();
        como(sessao).body(Map.of("contaId", contaId, "tipo", "RECEITA", "valor", valor, "categoriaId", salario))
                .when().post("/minha-conta/lancamentos").then().statusCode(201);
    }

    private static void transferir(String sessao, long origem, JsonPath destino, String valor, int status) {
        como(sessao).body(Map.of("contaOrigemId", origem, "agencia", destino.getString("agencia"),
                        "numero", destino.getString("numero"), "valor", valor))
                .when().post("/minha-conta/transferencias").then().statusCode(status);
    }

    /** Um campo de uma das minhas contas, ex.: campo(s, id, "investimento.lucro"). */
    private static String campo(String sessao, long contaId, String caminho) {
        return como(sessao).when().get("/minha-conta/contas").then().extract().jsonPath()
                .getString("find { it.id == " + contaId + " }." + caminho);
    }

    private static float numero(String sessao, long contaId, String caminho) {
        return Float.parseFloat(campo(sessao, contaId, caminho));
    }

    @Test
    void listaOsProdutosDeInvestimento() {
        como(cadastrar("Ana Produto")).when().get("/minha-conta/contas/produtos").then().statusCode(200)
                .body("$", hasSize(3))
                .body("id", hasItem("CDB"))
                .body("find { it.id == 'LCI' }.isentoIR", equalTo(true));
    }

    @Test
    void investimentoExigeProdutoETipoNaoMuda() {
        String s = cadastrar("Beto Tipo");
        como(s).body(Map.of("tipo", "INVESTIMENTO")).when().post("/minha-conta/contas").then().statusCode(422);
        long poupanca = abrir(s, Map.of("tipo", "POUPANCA")).getLong("id");
        // Renomear funciona; o corpo só tem nome (não há como trocar o tipo)
        como(s).body(Map.of("nome", "Reserva")).when().put("/minha-conta/contas/" + poupanca)
                .then().statusCode(200).body("nome", equalTo("Reserva")).body("tipo", equalTo("POUPANCA"));
    }

    @Test
    void aplicaRendeEResgataComImpostoDeRenda() {
        String s = cadastrar("Carla Investe");
        long corrente = principal(s).getLong("id");
        JsonPath cdb = abrir(s, Map.of("tipo", "INVESTIMENTO", "produto", "CDB"));
        long cdbId = cdb.getLong("id");
        assertEquals("CDB 110% do CDI", cdb.getString("investimento.produtoNome"));

        receita(s, corrente, "10000.00");
        transferir(s, corrente, cdb, "10000.00", 201);   // aplicação
        assertEquals(10000f, numero(s, cdbId, "investimento.aplicado"), 0.001f);

        // O ADM simula 1 mês: o CDB rende (~ R$ 92 em 30 dias, ~ R$ 95 em 31)
        como(Api.admin()).when().post("/painel/rendimentos/simular-mes").then().statusCode(200)
                .body("contas", notNullValue());
        String saldoCdb = campo(s, cdbId, "saldo");
        float saldo = Float.parseFloat(saldoCdb);
        assertTrue(saldo > 10080 && saldo < 10100, "saldo do CDB " + saldoCdb);
        float lucro = numero(s, cdbId, "investimento.lucro");
        float irEstimado = numero(s, cdbId, "investimento.irSeResgatarTudo");
        assertEquals(Math.round(lucro * 22.5f) / 100f, irEstimado, 0.011f, "22,5% sobre o lucro");

        // Resgate total para a corrente: entra o líquido, e o IR aparece no extrato como Impostos
        transferir(s, cdbId, principal(s), saldoCdb, 201);
        assertEquals(0f, numero(s, cdbId, "saldo"), 0.001f);
        assertEquals(saldo - irEstimado, principal(s).getFloat("saldo"), 0.011f);
        como(s).queryParam("categoriaId", categoria(s, "Impostos")).when().get("/minha-conta/transacoes")
                .then().body("$", hasSize(1)).body("[0].descricao", containsString("Imposto de Renda"));
        como(s).queryParam("categoriaId", categoria(s, "Rendimento")).when().get("/minha-conta/transacoes")
                .then().body("$", hasSize(1)).body("[0].tipo", equalTo("ENTRADA"));
    }

    @Test
    void investimentoNaoRecebeDeOutraPessoaNemAceitaLancamento() {
        String dono = cadastrar("Davi Dono");
        String outro = cadastrar("Elis Outra");
        JsonPath inv = abrir(dono, Map.of("tipo", "INVESTIMENTO", "produto", "TESOURO_SELIC"));
        long invId = inv.getLong("id");

        long correnteDoOutro = principal(outro).getLong("id");
        receita(outro, correnteDoOutro, "100.00");
        transferir(outro, correnteDoOutro, inv, "50.00", 422);

        receita(dono, principal(dono).getLong("id"), "100.00");
        List<Map<String, Object>> cats = como(dono).when().get("/minha-conta/categorias").then().extract()
                .jsonPath().getList("$");
        long lazer = cats.stream().filter(c -> "Lazer".equals(c.get("descricao")))
                .map(c -> ((Number) c.get("id")).longValue()).findFirst().orElseThrow();
        como(dono).body(Map.of("contaId", invId, "tipo", "RECEITA", "valor", "10.00", "categoriaId", lazer))
                .when().post("/minha-conta/lancamentos").then().statusCode(422)
                .body("erro", containsString("não aceita lançamentos"));
    }

    @Test
    void poupancaRendeComOMesSimuladoETemMeta() {
        String s = cadastrar("Fábio Poupa");
        long corrente = principal(s).getLong("id");
        JsonPath poupanca = abrir(s, Map.of("tipo", "POUPANCA", "nome", "Viagem"));
        long poupancaId = poupanca.getLong("id");
        assertEquals("POUPANCA", poupanca.getString("tipo"));

        receita(s, corrente, "2000.00");
        transferir(s, corrente, poupanca, "2000.00", 201);

        como(s).body(Map.of("valor", "8000.00", "descricao", "Viagem para o Chile",
                        "data", LocalDate.now().plusMonths(10).toString()))
                .when().put("/minha-conta/contas/" + poupancaId + "/meta").then().statusCode(200)
                .body("meta.progresso", equalTo(0.25f));

        como(Api.admin()).when().post("/painel/rendimentos/simular-mes").then().statusCode(200);
        assertEquals(2010.0f, numero(s, poupancaId, "saldo"), 0.001f, "0,5% de 2.000");

        // Meta só existe na poupança
        como(s).body(Map.of("valor", "100.00")).when().put("/minha-conta/contas/" + corrente + "/meta")
                .then().statusCode(422);
        como(s).when().delete("/minha-conta/contas/" + poupancaId + "/meta").then().statusCode(200)
                .body("meta", nullValue());
    }

    @Test
    void clienteNaoSimulaRendimento() {
        como(cadastrar("Gil Esperto")).when().post("/painel/rendimentos/simular-mes").then().statusCode(403);
    }

    private static long categoria(String sessao, String nome) {
        List<Map<String, Object>> cats = como(sessao).when().get("/minha-conta/categorias").then().extract()
                .jsonPath().getList("$");
        return cats.stream().filter(c -> nome.equals(c.get("descricao")))
                .map(c -> ((Number) c.get("id")).longValue()).findFirst().orElseThrow();
    }
}
