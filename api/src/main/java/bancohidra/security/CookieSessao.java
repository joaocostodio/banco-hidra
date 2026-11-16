package bancohidra.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.Date;

/**
 * O token JWT viaja num cookie, e não no JavaScript do site:
 * <ul>
 *   <li><b>HttpOnly</b>: o JavaScript da página não consegue ler o cookie. Se alguém conseguir injetar um
 *       script no site (XSS), ele não rouba a sessão, como aconteceria com o token no localStorage.</li>
 *   <li><b>SameSite=Strict</b>: o navegador não manda o cookie em requisições que vêm de outros sites,
 *       o que bloqueia CSRF (um site malicioso fazendo transferências em nome de quem está logado).</li>
 *   <li><b>Secure</b> quando o acesso é por HTTPS (túnel): o cookie nunca passa sem criptografia.</li>
 * </ul>
 * O nome do cookie é o mesmo configurado em {@code mp.jwt.token.cookie}, de onde o Quarkus lê o token.
 */
@ApplicationScoped
public class CookieSessao {

    public static final String NOME = "hidra_sessao";

    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan", defaultValue = "3600")
    long validadeSegundos;

    public NewCookie criar(String token, HttpHeaders cabecalhos) {
        return base(cabecalhos).value(token).maxAge((int) validadeSegundos).build();
    }

    /** Cookie vencido com o mesmo nome: o navegador apaga o da sessão. */
    public NewCookie apagar(HttpHeaders cabecalhos) {
        return base(cabecalhos).value("").maxAge(0).expiry(Date.from(Instant.EPOCH)).build();
    }

    private static NewCookie.Builder base(HttpHeaders cabecalhos) {
        return new NewCookie.Builder(NOME)
                .path("/")
                .httpOnly(true)
                .secure(viaHttps(cabecalhos))
                .sameSite(NewCookie.SameSite.STRICT);
    }

    /**
     * O site chega à API por um proxy (ng serve) e, pelo túnel, pela Cloudflare: quem fala HTTPS com o
     * navegador avisa pelo cabeçalho X-Forwarded-Proto. Se alguém forjar esse cabeçalho, só muda a flag
     * Secure do próprio cookie.
     */
    private static boolean viaHttps(HttpHeaders cabecalhos) {
        return "https".equalsIgnoreCase(cabecalhos.getHeaderString("X-Forwarded-Proto"));
    }
}
