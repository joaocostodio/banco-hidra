package bancohidra.controller;

import bancohidra.controller.Api.Cliente;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static bancohidra.controller.Api.como;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

/** O que o app do cliente usa além do fluxo principal. */
@QuarkusTest
class AppClienteTest {

    @Test
    void chaveCpfOuEmailEmBrancoUsaOsDadosDoTitular() {
        Cliente nina = Api.novoCliente("Nina Lopes");
        como(nina.token()).body(Map.of("tipo", "CPF", "valor", ""))
                .when().post("/minha-conta/pix/chaves")
                .then().statusCode(201).body("valor", equalTo(nina.cpf()));
        como(nina.token()).body(Map.of("tipo", "EMAIL"))
                .when().post("/minha-conta/pix/chaves")
                .then().statusCode(201).body("valor", equalTo(nina.email()));
        como(nina.token()).when().get("/minha-conta/pix/chaves").then().body("$", hasSize(2));

        // Com valor, continua exigindo que o CPF seja o do titular
        como(nina.token()).body(Map.of("tipo", "CPF", "valor", Api.cpfValido(990)))
                .when().post("/minha-conta/pix/chaves").then().statusCode(422);
    }

    @Test
    void clienteVeOsProprioDados() {
        Cliente otto = Api.novoCliente("Otto Reis");
        como(otto.token()).when().get("/minha-conta/dados").then().statusCode(200)
                .body("nome", equalTo("Otto Reis"))
                .body("email", equalTo(otto.email()))
                .body("cpf", startsWith(otto.cpf().substring(0, 3) + "."))
                .body("numero", equalTo(otto.numero()))
                .body("status", equalTo("ATIVA"));
    }
}
