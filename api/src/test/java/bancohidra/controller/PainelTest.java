package bancohidra.controller;

import bancohidra.controller.Api.Cliente;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static bancohidra.controller.Api.como;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Dados extras que o painel ADM usa: resumo do banco, saldo na lista de clientes, uso das categorias. */
@QuarkusTest
class PainelTest {

    private static JsonPath resumo(String admin) {
        return como(admin).when().get("/painel").then().statusCode(200)
                .body("ultimosDias", hasSize(7))
                .extract().jsonPath();
    }

    private static BigDecimal dinheiro(JsonPath json, String campo) {
        return new BigDecimal(json.getString(campo)).setScale(2);
    }

    @Test
    void resumoContaCadaOperacaoUmaVez() {
        String admin = Api.admin();
        JsonPath antes = resumo(admin);

        Cliente ivo = Api.novoCliente("Ivo Rocha");
        Cliente jana = Api.novoCliente("Jana Couto");
        como(admin).body(Map.of("valor", 30)).when().post("/contas/" + ivo.contaId() + "/depositos")
                .then().statusCode(201);
        como(ivo.token()).body(Map.of("agencia", jana.agencia(), "numero", jana.numero(), "valor", 10))
                .when().post("/minha-conta/transferencias").then().statusCode(201);

        JsonPath depois = resumo(admin);
        assertEquals(antes.getLong("clientes") + 2, depois.getLong("clientes"));
        assertEquals(antes.getLong("contasAtivas") + 2, depois.getLong("contasAtivas"));
        // Depósito + transferência = 2 operações (a transferência tem 2 linhas, mas conta uma vez)
        assertEquals(antes.getLong("operacoesHoje") + 2, depois.getLong("operacoesHoje"));
        assertEquals(dinheiro(antes, "valorHoje").add(new BigDecimal("40.00")), dinheiro(depois, "valorHoje"));
        // Transferência interna não muda o total guardado no banco; o depósito sim
        assertEquals(dinheiro(antes, "totalCustodiado").add(new BigDecimal("30.00")),
                dinheiro(depois, "totalCustodiado"));
        assertEquals(depois.getLong("operacoesHoje"), depois.getLong("ultimosDias[6].operacoes"));
    }

    @Test
    void listaDeClientesMostraSaldoEPrimeiroAcesso() {
        String admin = Api.admin();
        Cliente kaio = Api.novoCliente("Kaio Prates");
        como(admin).body(Map.of("valor", 12.5)).when().post("/contas/" + kaio.contaId() + "/depositos")
                .then().statusCode(201);
        como(admin).when().get("/clientes/" + kaio.id()).then().statusCode(200)
                .body("conta.saldo", equalTo(12.5f))
                .body("primeiroAcesso", is(false));

        int novo = como(admin)
                .body(Map.of("nome", "Lia Moura", "cpf", Api.cpfValido(950), "email", "lia." + System.nanoTime() + "@hidra.com"))
                .when().post("/clientes").then().statusCode(201)
                .body("cliente.primeiroAcesso", is(true))
                .extract().path("cliente.id");
        como(admin).when().get("/clientes/" + novo).then().body("primeiroAcesso", is(true));
    }

    @Test
    void categoriaInformaQuantasVezesFoiUsada() {
        String admin = Api.admin();
        Cliente leo = Api.novoCliente("Leo Brandt");
        long antes = como(admin).when().get("/categorias").then().statusCode(200)
                .extract().jsonPath().getLong("find { it.descricao == 'Depósito' }.usos");
        como(admin).body(Map.of("valor", 5)).when().post("/contas/" + leo.contaId() + "/depositos")
                .then().statusCode(201);
        como(admin).when().get("/categorias").then()
                .body("find { it.descricao == 'Depósito' }.usos", equalTo((int) antes + 1));
    }

    @Test
    void painelEhSoDoAdministrador() {
        Cliente mia = Api.novoCliente("Mia Torres");
        como(mia.token()).when().get("/painel").then().statusCode(403);
        como(mia.token()).when().get("/painel/rede").then().statusCode(403);
    }

    @Test
    void modoTurmaAvisaQueOAdminUsaASenhaPadrao() {
        como(Api.admin()).when().get("/painel/rede").then().statusCode(200)
                .body("senhaPadrao", is(true));
    }
}
