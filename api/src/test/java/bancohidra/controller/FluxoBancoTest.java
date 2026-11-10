package bancohidra.controller;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Fluxo completo pela API, com um PostgreSQL de verdade (o Quarkus sobe um descartável
 * pelo Docker): cadastrar clientes, depositar, Pix por chave, extrato e comprovante.
 */
@QuarkusTest
class FluxoBancoTest {

    private long cadastrar(String nome, String cpf, String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("nome", nome, "cpf", cpf, "email", email))
                .when().post("/clientes")
                .then().statusCode(201)
                .body("conta.agencia", equalTo("0001"))
                .extract().jsonPath().getLong("conta.id");
    }

    @Test
    void depositoPixExtratoEComprovante() {
        long ana = cadastrar("Ana Souza", "529.982.247-25", "ana@hidra.com");
        long beto = cadastrar("Beto Lima", "111.444.777-35", "beto@hidra.com");

        // CPF repetido é recusado
        given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Outra", "cpf", "52998224725", "email", "x@hidra.com"))
                .when().post("/clientes").then().statusCode(422);

        // Administrador deposita 100 na conta da Ana
        given().contentType(ContentType.JSON).body(Map.of("valor", 100))
                .when().post("/contas/" + ana + "/depositos")
                .then().statusCode(201).body("saldoApos", equalTo(100.0f));

        // Beto cadastra a chave e-mail
        given().contentType(ContentType.JSON).body(Map.of("tipo", "EMAIL", "valor", "Beto@Hidra.com"))
                .when().post("/pix/contas/" + beto + "/chaves")
                .then().statusCode(201).body("valor", equalTo("beto@hidra.com"));

        // Ana consulta a chave antes de pagar
        given().queryParam("chave", "beto@hidra.com")
                .when().get("/pix/destinatario")
                .then().statusCode(200)
                .body("nome", equalTo("Beto Lima"))
                .body("cpfMascarado", equalTo("***.444.777-**"));

        // Pix de 40 da Ana para o Beto
        String autenticacao = given().contentType(ContentType.JSON)
                .body(Map.of("contaOrigemId", ana, "chave", "beto@hidra.com", "valor", 40, "descricao", "Almoço"))
                .when().post("/pix/transferencias")
                .then().statusCode(201)
                .body("tipo", equalTo("SAIDA"))
                .body("categoria", equalTo("Pix"))
                .body("contraparteNome", equalTo("Beto Lima"))
                .extract().path("autenticacao");

        // Pix maior que o saldo é recusado e não altera nada
        given().contentType(ContentType.JSON)
                .body(Map.of("contaOrigemId", ana, "chave", "beto@hidra.com", "valor", 1000))
                .when().post("/pix/transferencias")
                .then().statusCode(422).body("erro", equalTo("Saldo insuficiente"));

        given().when().get("/contas/" + ana).then().body("saldo", equalTo(60.0f));
        given().when().get("/contas/" + beto).then().body("saldo", equalTo(40.0f));

        // Extrato da Ana: Pix (mais recente) e depósito
        given().when().get("/contas/" + ana + "/extrato")
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].descricao", equalTo("Almoço"))
                .body("[1].categoria", equalTo("Depósito"));

        // Comprovante liga as duas pontas
        given().when().get("/transacoes/comprovantes/" + autenticacao)
                .then().statusCode(200)
                .body("origem.nome", equalTo("Ana Souza"))
                .body("destino.nome", equalTo("Beto Lima"))
                .body("valor", equalTo(40.0f));

        // Conta bloqueada não recebe
        given().when().post("/contas/" + beto + "/bloqueio").then().statusCode(200).body("status", equalTo("BLOQUEADA"));
        given().contentType(ContentType.JSON)
                .body(Map.of("contaOrigemId", ana, "contaDestinoId", beto, "valor", 1))
                .when().post("/transacoes/transferencias")
                .then().statusCode(422);
        given().when().delete("/contas/" + beto + "/bloqueio").then().statusCode(200).body("status", equalTo("ATIVA"));
    }

    @Test
    void categoriasDoSistemaSaoProtegidas() {
        int pix = given().when().get("/categorias").then().statusCode(200)
                .extract().jsonPath().getInt("find { it.descricao == 'Pix' }.id");

        given().when().delete("/categorias/" + pix).then().statusCode(422);

        int nova = given().contentType(ContentType.JSON).body(Map.of("descricao", "Mercado"))
                .when().post("/categorias").then().statusCode(201)
                .body("sistema", is(false)).extract().path("id");

        given().contentType(ContentType.JSON).body(Map.of("descricao", "Supermercado"))
                .when().put("/categorias/" + nova).then().statusCode(200).body("descricao", equalTo("Supermercado"));
        given().when().delete("/categorias/" + nova).then().statusCode(204);
    }
}
