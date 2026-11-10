package bancohidra.controller;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;

/** Atalhos para os testes da API: login, cadastro de clientes e CPFs válidos únicos. */
final class Api {

    static final String ADMIN_EMAIL = "admin@hidra.com";
    static final String ADMIN_SENHA = "hidra123";
    static final String SENHA_CLIENTE = "senhaForte1";

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    private Api() {
    }

    record Cliente(long id, long contaId, String agencia, String numero, String cpf, String email,
                   String senhaProvisoria, String token) {
    }

    static RequestSpecification como(String token) {
        return given().auth().oauth2(token).contentType(ContentType.JSON);
    }

    static String login(String email, String senha) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "senha", senha))
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    static String admin() {
        return login(ADMIN_EMAIL, ADMIN_SENHA);
    }

    /** Admin cadastra; o cliente entra com a senha provisória e troca pela definitiva. */
    static Cliente novoCliente(String nome) {
        int n = SEQUENCIA.incrementAndGet();
        String cpf = cpfValido(n);
        String email = "cliente" + n + "." + System.nanoTime() + "@hidra.com";
        var resposta = como(admin())
                .body(Map.of("nome", nome, "cpf", cpf, "email", email))
                .when().post("/clientes")
                .then().statusCode(201)
                .extract().jsonPath();
        String provisoria = resposta.getString("senhaProvisoria");
        String tokenProvisorio = login(email, provisoria);
        String token = como(tokenProvisorio)
                .body(Map.of("senhaAtual", provisoria, "novaSenha", SENHA_CLIENTE))
                .when().put("/auth/senha")
                .then().statusCode(200)
                .extract().path("token");
        return new Cliente(resposta.getLong("cliente.id"), resposta.getLong("cliente.conta.id"),
                resposta.getString("cliente.conta.agencia"), resposta.getString("cliente.conta.numero"),
                cpf, email, provisoria, token);
    }

    /** Gera um CPF válido diferente para cada n (calcula os dígitos verificadores). */
    static String cpfValido(int n) {
        String base = String.format("%09d", 100_000_000 + n * 7_919);
        int d1 = digito(base);
        int d2 = digito(base + d1);
        return base + d1 + d2;
    }

    private static int digito(String numeros) {
        int soma = 0;
        int peso = numeros.length() + 1;
        for (char c : numeros.toCharArray()) {
            soma += (c - '0') * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
