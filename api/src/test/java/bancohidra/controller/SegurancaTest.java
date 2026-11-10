package bancohidra.controller;

import bancohidra.controller.Api.Cliente;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static bancohidra.controller.Api.como;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SegurancaTest {

    private static void tentarLogin(String email, String senha, int status) {
        given().contentType(ContentType.JSON).body(Map.of("email", email, "senha", senha))
                .when().post("/auth/login").then().statusCode(status);
    }

    @Test
    void semLoginNaoEntra() {
        given().when().get("/clientes").then().statusCode(401);
        given().when().get("/minha-conta").then().statusCode(401);
        tentarLogin(Api.ADMIN_EMAIL, "senhaErrada1", 401);
        tentarLogin("ninguem@hidra.com", "qualquer1", 401);
    }

    @Test
    void cadaPerfilSoAcessaSuaArea() {
        Cliente cliente = Api.novoCliente("Carla Dias");
        como(cliente.token()).when().get("/clientes").then().statusCode(403);
        como(cliente.token()).when().get("/contas/" + cliente.contaId()).then().statusCode(403);
        como(Api.admin()).when().get("/minha-conta").then().statusCode(403);
    }

    @Test
    void senhaProvisoriaSoServeParaTrocarASenha() {
        String admin = Api.admin();
        String email = "diego." + System.nanoTime() + "@hidra.com";
        String provisoria = como(admin)
                .body(Map.of("nome", "Diego Reis", "cpf", Api.cpfValido(900), "email", email))
                .when().post("/clientes").then().statusCode(201)
                .extract().path("senhaProvisoria");

        String tokenProvisorio = given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "senha", provisoria))
                .when().post("/auth/login").then().statusCode(200)
                .body("trocarSenha", is(true))
                .extract().path("token");

        como(tokenProvisorio).when().get("/minha-conta").then().statusCode(403);

        // Senha fraca é recusada
        como(tokenProvisorio).body(Map.of("senhaAtual", provisoria, "novaSenha", "123"))
                .when().put("/auth/senha").then().statusCode(422);

        String token = como(tokenProvisorio).body(Map.of("senhaAtual", provisoria, "novaSenha", "minhaSenha9"))
                .when().put("/auth/senha").then().statusCode(200)
                .body("trocarSenha", is(false))
                .extract().path("token");
        como(token).when().get("/minha-conta").then().statusCode(200);

        // A provisória deixou de funcionar
        tentarLogin(email, provisoria, 401);
    }

    @Test
    void bloqueiaDepoisDeCincoSenhasErradas() {
        Cliente cliente = Api.novoCliente("Elisa Prado");
        for (int i = 0; i < 5; i++) {
            tentarLogin(cliente.email(), "errada" + i + "x", 401);
        }
        // Mesmo com a senha certa, fica bloqueado
        given().contentType(ContentType.JSON).body(Map.of("email", cliente.email(), "senha", Api.SENHA_CLIENTE))
                .when().post("/auth/login").then().statusCode(401)
                .body("erro", containsString("bloqueado"));

        // O administrador gera nova senha provisória, o que libera o acesso
        String nova = como(Api.admin()).when().post("/clientes/" + cliente.id() + "/senha-provisoria")
                .then().statusCode(200).extract().path("senhaProvisoria");
        given().contentType(ContentType.JSON).body(Map.of("email", cliente.email(), "senha", nova))
                .when().post("/auth/login").then().statusCode(200).body("trocarSenha", equalTo(true));
    }

    @Test
    void clienteNaoVeComprovanteDosOutros() {
        String admin = Api.admin();
        Cliente fabio = Api.novoCliente("Fábio Melo");
        Cliente gabi = Api.novoCliente("Gabi Nunes");
        Cliente hugo = Api.novoCliente("Hugo Alves");
        como(admin).body(Map.of("valor", 50)).when().post("/contas/" + fabio.contaId() + "/depositos")
                .then().statusCode(201);
        String autenticacao = como(fabio.token())
                .body(Map.of("agencia", gabi.agencia(), "numero", gabi.numero(), "valor", 10))
                .when().post("/minha-conta/transferencias").then().statusCode(201)
                .extract().path("autenticacao");

        como(gabi.token()).when().get("/minha-conta/comprovantes/" + autenticacao).then().statusCode(200);
        como(hugo.token()).when().get("/minha-conta/comprovantes/" + autenticacao).then().statusCode(404);
    }
}
