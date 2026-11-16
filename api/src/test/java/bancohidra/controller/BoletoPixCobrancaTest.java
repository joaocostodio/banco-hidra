package bancohidra.controller;

import bancohidra.controller.Api.Cliente;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static bancohidra.controller.Api.como;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/** Etapa 5: cobrança Pix por QR code (copia e cola) e boleto entre clientes. */
@QuarkusTest
class BoletoPixCobrancaTest {

    private static void depositar(Cliente c, String valor) {
        como(Api.admin()).body(Map.of("valor", valor, "descricao", "Saldo inicial"))
                .when().post("/contas/" + c.contaId() + "/depositos").then().statusCode(201);
    }

    private static float saldo(Cliente c) {
        return como(c.token()).when().get("/minha-conta").then().extract().path("saldo");
    }

    // ---------- Pix copia e cola ----------

    @Test
    void cobrancaPixGeraCodigoQueOutroClienteLeEPaga() {
        Cliente rita = Api.novoCliente("Rita Prado");
        Cliente caio = Api.novoCliente("Caio Melo");
        depositar(caio, "100.00");

        long chaveId = ((Number) como(rita.token()).body(Map.of("tipo", "ALEATORIA"))
                .when().post("/minha-conta/pix/chaves").then().statusCode(201)
                .extract().path("id")).longValue();

        String codigo = como(rita.token()).body(Map.of("chaveId", chaveId, "valor", "30.00", "descricao", "Rifa"))
                .when().post("/minha-conta/pix/cobrancas")
                .then().statusCode(200)
                .body("valor", equalTo(30.0f))
                .body("codigo", containsString("br.gov.bcb.pix"))
                .extract().path("codigo");

        String chave = como(caio.token()).body(Map.of("codigo", codigo))
                .when().post("/minha-conta/pix/ler-codigo")
                .then().statusCode(200)
                .body("valor", equalTo(30.0f))
                .body("descricao", equalTo("Rifa"))
                .body("destinatario.nome", equalTo("Rita Prado"))
                .extract().path("chave");

        como(caio.token()).body(Map.of("chave", chave, "valor", "30.00", "descricao", "Rifa"))
                .when().post("/minha-conta/pix").then().statusCode(201);
        assertSaldo(rita, 30f);
        assertSaldo(caio, 70f);
    }

    @Test
    void naoCobraComChaveDeOutroNemLeCodigoAlterado() {
        Cliente lia = Api.novoCliente("Lia Torres");
        Cliente gil = Api.novoCliente("Gil Pinto");
        long chaveDaLia = ((Number) como(lia.token()).body(Map.of("tipo", "EMAIL"))
                .when().post("/minha-conta/pix/chaves").then().statusCode(201)
                .extract().path("id")).longValue();

        como(gil.token()).body(Map.of("chaveId", chaveDaLia))
                .when().post("/minha-conta/pix/cobrancas").then().statusCode(404);

        String codigo = como(lia.token()).body(Map.of("chaveId", chaveDaLia))
                .when().post("/minha-conta/pix/cobrancas")
                .then().statusCode(200).body("valor", nullValue())
                .extract().path("codigo");
        // Sem valor: o pagador escolhe
        como(gil.token()).body(Map.of("codigo", codigo))
                .when().post("/minha-conta/pix/ler-codigo").then().statusCode(200).body("valor", nullValue());
        // Trocar qualquer caractere quebra o CRC
        String alterado = codigo.replace("LIA", "LUA");
        como(gil.token()).body(Map.of("codigo", alterado))
                .when().post("/minha-conta/pix/ler-codigo").then().statusCode(422);
    }

    // ---------- Boleto ----------

    @Test
    void clienteEmiteBoletoEOutroPaga() {
        Cliente eva = Api.novoCliente("Eva Nunes");
        Cliente ivo = Api.novoCliente("Ivo Brito");
        depositar(ivo, "200.00");

        var boleto = como(eva.token())
                .body(Map.of("valor", "120.50", "vencimento", LocalDate.now().plusDays(10).toString(),
                        "descricao", "Aluguel"))
                .when().post("/minha-conta/boletos")
                .then().statusCode(201)
                .body("status", equalTo("ABERTO"))
                .body("vencido", equalTo(false))
                .extract().jsonPath();
        String linha = boleto.getString("linhaDigitavel");

        como(ivo.token()).queryParam("linha", linha)
                .when().get("/minha-conta/boletos/consulta")
                .then().statusCode(200)
                .body("beneficiario.nome", equalTo("Eva Nunes"))
                .body("total", equalTo(120.5f))
                .body("diasAtraso", equalTo(0));

        // Quem emitiu não paga o próprio boleto
        como(eva.token()).body(Map.of("linha", linha))
                .when().post("/minha-conta/boletos/pagamento").then().statusCode(422);

        String autenticacao = como(ivo.token()).body(Map.of("linha", linha))
                .when().post("/minha-conta/boletos/pagamento")
                .then().statusCode(201)
                .body("categoria", equalTo("Pagamento"))
                .body("descricao", equalTo("Boleto: Aluguel"))
                .extract().path("autenticacao");
        assertSaldo(eva, 120.5f);
        assertSaldo(ivo, 79.5f);

        // Não paga duas vezes
        como(ivo.token()).body(Map.of("linha", linha))
                .when().post("/minha-conta/boletos/pagamento")
                .then().statusCode(422).body("erro", equalTo("Este boleto já foi pago"));

        // Quem emitiu vê quem pagou, e os dois abrem o comprovante
        como(eva.token()).when().get("/minha-conta/boletos")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].status", equalTo("PAGO"))
                .body("[0].pagadorNome", equalTo("Ivo Brito"));
        como(eva.token()).when().get("/minha-conta/comprovantes/" + autenticacao).then().statusCode(200);
        como(ivo.token()).when().get("/minha-conta/comprovantes/" + autenticacao).then().statusCode(200);
    }

    @Test
    void semSaldoOBoletoContinuaAberto() {
        Cliente ana = Api.novoCliente("Ana Vaz");
        Cliente bia = Api.novoCliente("Bia Couto");
        depositar(bia, "10.00");
        String linha = como(ana.token())
                .body(Map.of("valor", "50.00", "vencimento", LocalDate.now().toString()))
                .when().post("/minha-conta/boletos").then().statusCode(201)
                .extract().path("linhaDigitavel");

        como(bia.token()).body(Map.of("linha", linha))
                .when().post("/minha-conta/boletos/pagamento").then().statusCode(422);
        assertSaldo(bia, 10f);
        como(ana.token()).when().get("/minha-conta/boletos")
                .then().body("[0].status", equalTo("ABERTO"));
    }

    @Test
    void boletoCanceladoNaoPodeSerPagoELinhaErradaEhRecusada() {
        Cliente teo = Api.novoCliente("Teo Dias");
        Cliente uma = Api.novoCliente("Uma Reis");
        depositar(uma, "100.00");
        var boleto = como(teo.token())
                .body(Map.of("valor", "20.00", "vencimento", LocalDate.now().plusDays(1).toString()))
                .when().post("/minha-conta/boletos").then().statusCode(201).extract().jsonPath();
        String linha = boleto.getString("linhaDigitavel");

        // Outro cliente não cancela nem vê o boleto de alguém
        como(uma.token()).when().delete("/minha-conta/boletos/" + boleto.getLong("id")).then().statusCode(404);
        como(uma.token()).when().get("/minha-conta/boletos/" + boleto.getLong("id")).then().statusCode(404);

        como(teo.token()).when().delete("/minha-conta/boletos/" + boleto.getLong("id"))
                .then().statusCode(200).body("status", equalTo("CANCELADO"));
        como(uma.token()).body(Map.of("linha", linha))
                .when().post("/minha-conta/boletos/pagamento")
                .then().statusCode(422).body("erro", containsString("cancelado"));

        String digitos = linha.replaceAll("\\D", "");
        char trocado = (char) ('0' + (digitos.charAt(3) - '0' + 1) % 10);
        String errada = digitos.substring(0, 3) + trocado + digitos.substring(4);
        como(uma.token()).queryParam("linha", errada)
                .when().get("/minha-conta/boletos/consulta").then().statusCode(422);

        // Vencimento no passado não é aceito na emissão
        como(teo.token()).body(Map.of("valor", "20.00", "vencimento", LocalDate.now().minusDays(1).toString()))
                .when().post("/minha-conta/boletos").then().statusCode(422);
    }

    @Test
    void adminNaoUsaBoletoDoCliente() {
        como(Api.admin()).when().get("/minha-conta/boletos").then().statusCode(403);
    }

    private static void assertSaldo(Cliente c, float esperado) {
        org.junit.jupiter.api.Assertions.assertEquals(esperado, saldo(c), 0.001f);
    }
}
