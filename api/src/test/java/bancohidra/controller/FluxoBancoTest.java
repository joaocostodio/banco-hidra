package bancohidra.controller;

import bancohidra.controller.Api.Cliente;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static bancohidra.controller.Api.como;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Fluxo completo pela API, com um PostgreSQL de verdade (o Quarkus sobe um descartável
 * pelo Docker): admin cadastra clientes e deposita; clientes fazem Pix e transferência.
 */
@QuarkusTest
class FluxoBancoTest {

    @Test
    void depositoPixTransferenciaExtratoEComprovante() {
        String admin = Api.admin();
        Cliente ana = Api.novoCliente("Ana Souza");
        Cliente beto = Api.novoCliente("Beto Lima");

        // CPF repetido é recusado
        como(admin).body(Map.of("nome", "Outra", "cpf", ana.cpf(), "email", "outra@hidra.com"))
                .when().post("/clientes").then().statusCode(422);

        // Administrador deposita 100 na conta da Ana
        como(admin).body(Map.of("valor", 100))
                .when().post("/contas/" + ana.contaId() + "/depositos")
                .then().statusCode(201).body("saldoApos", equalTo(100.0f));

        // Beto cadastra a chave e-mail
        como(beto.token()).body(Map.of("tipo", "EMAIL", "valor", "Beto@Hidra.com"))
                .when().post("/minha-conta/pix/chaves")
                .then().statusCode(201).body("valor", equalTo("beto@hidra.com"));

        // Ana consulta a chave antes de pagar
        como(ana.token()).queryParam("chave", "beto@hidra.com")
                .when().get("/minha-conta/pix/destinatario")
                .then().statusCode(200)
                .body("nome", equalTo("Beto Lima"))
                .body("tipoChave", equalTo("EMAIL"));

        // Pix de 40 da Ana para o Beto
        String autenticacao = como(ana.token())
                .body(Map.of("chave", "beto@hidra.com", "valor", 40, "descricao", "Almoço"))
                .when().post("/minha-conta/pix")
                .then().statusCode(201)
                .body("tipo", equalTo("SAIDA"))
                .body("categoria", equalTo("Pix"))
                .body("contraparteNome", equalTo("Beto Lima"))
                .extract().path("autenticacao");

        // Pix maior que o saldo é recusado e não altera nada
        como(ana.token()).body(Map.of("chave", "beto@hidra.com", "valor", 1000))
                .when().post("/minha-conta/pix")
                .then().statusCode(422).body("erro", equalTo("Saldo insuficiente"));

        // Beto devolve 15 por agência + número
        como(beto.token()).queryParam("agencia", ana.agencia()).queryParam("numero", ana.numero())
                .when().get("/minha-conta/destinatario")
                .then().statusCode(200).body("nome", equalTo("Ana Souza"));
        como(beto.token())
                .body(Map.of("agencia", ana.agencia(), "numero", ana.numero(), "valor", 15))
                .when().post("/minha-conta/transferencias")
                .then().statusCode(201).body("categoria", equalTo("Transferência"));

        // Para a própria conta não pode
        como(ana.token())
                .body(Map.of("agencia", ana.agencia(), "numero", ana.numero(), "valor", 1))
                .when().post("/minha-conta/transferencias")
                .then().statusCode(422);

        como(ana.token()).when().get("/minha-conta").then().body("saldo", equalTo(75.0f));
        como(beto.token()).when().get("/minha-conta").then().body("saldo", equalTo(25.0f));

        // Extrato da Ana: transferência recebida, Pix enviado, depósito (do mais recente ao mais antigo)
        como(ana.token()).when().get("/minha-conta/transacoes")
                .then().statusCode(200)
                .body("$", hasSize(3))
                .body("[0].tipo", equalTo("ENTRADA"))
                .body("[1].descricao", equalTo("Almoço"))
                .body("[2].categoria", equalTo("Depósito"));

        // Comprovante liga as duas pontas
        como(ana.token()).when().get("/minha-conta/comprovantes/" + autenticacao)
                .then().statusCode(200)
                .body("origem.nome", equalTo("Ana Souza"))
                .body("destino.nome", equalTo("Beto Lima"))
                .body("valor", equalTo(40.0f));

        // Conta bloqueada não recebe; desbloqueada volta a receber
        como(admin).when().post("/contas/" + beto.contaId() + "/bloqueio")
                .then().statusCode(200).body("status", equalTo("BLOQUEADA"));
        como(ana.token()).body(Map.of("chave", "beto@hidra.com", "valor", 1))
                .when().post("/minha-conta/pix").then().statusCode(422);
        como(admin).when().delete("/contas/" + beto.contaId() + "/bloqueio")
                .then().statusCode(200).body("status", equalTo("ATIVA"));
    }

    @Test
    void categoriasDoSistemaSaoProtegidas() {
        String admin = Api.admin();
        int pix = como(admin).when().get("/categorias").then().statusCode(200)
                .extract().jsonPath().getInt("find { it.descricao == 'Pix' }.id");

        como(admin).when().delete("/categorias/" + pix).then().statusCode(422);

        int nova = como(admin).body(Map.of("descricao", "Mercado"))
                .when().post("/categorias").then().statusCode(201)
                .body("sistema", is(false)).extract().path("id");

        como(admin).body(Map.of("descricao", "Supermercado"))
                .when().put("/categorias/" + nova).then().statusCode(200).body("descricao", equalTo("Supermercado"));
        como(admin).when().delete("/categorias/" + nova).then().statusCode(204);
    }
}
