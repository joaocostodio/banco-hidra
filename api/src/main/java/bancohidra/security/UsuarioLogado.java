package bancohidra.security;

import bancohidra.entity.Conta;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoAutorizadoException;
import bancohidra.exception.NaoEncontradoException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Quem está fazendo a requisição, lido do token. É daqui (e nunca do corpo da requisição)
 * que sai a conta do cliente: assim ninguém movimenta a conta de outra pessoa.
 */
@RequestScoped
public class UsuarioLogado {

    @Inject
    JsonWebToken token;

    public Long id() {
        if (token == null || token.getSubject() == null) {
            throw new NaoAutorizadoException("Faça login para continuar");
        }
        return Long.valueOf(token.getSubject());
    }

    public Usuario usuario() {
        Usuario usuario = Usuario.findById(id());
        if (usuario == null) {
            throw new NaoAutorizadoException("Usuário não existe mais; faça login novamente");
        }
        return usuario;
    }

    public Conta conta() {
        Conta conta = Conta.doTitular(usuario());
        if (conta == null) {
            throw new NaoEncontradoException("Você não tem conta no Banco Hidra");
        }
        return conta;
    }
}
