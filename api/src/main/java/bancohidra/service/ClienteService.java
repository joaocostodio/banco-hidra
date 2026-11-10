package bancohidra.service;

import bancohidra.dto.ClienteDtos.EditarClienteRequest;
import bancohidra.dto.ClienteDtos.NovoClienteRequest;
import bancohidra.entity.Conta;
import bancohidra.entity.Perfil;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import bancohidra.security.SenhaProvisoria;
import bancohidra.util.Cpf;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class ClienteService {

    /** A senha provisória só existe aqui, em texto: no banco fica apenas o hash. */
    public record Cadastro(Conta conta, String senhaProvisoria) {
    }

    /** Cadastra o cliente, abre a conta corrente dele (saldo zero) e gera a senha provisória. */
    @Transactional
    public Cadastro cadastrar(NovoClienteRequest req) {
        String cpf = Cpf.somenteDigitos(req.cpf());
        if (!Cpf.valido(cpf)) {
            throw new RegraNegocioException("CPF inválido");
        }
        if (Usuario.existeCpf(cpf)) {
            throw new RegraNegocioException("Já existe um cliente com este CPF");
        }
        String email = req.email().trim().toLowerCase();
        if (Usuario.existeEmail(email, null)) {
            throw new RegraNegocioException("Já existe um cliente com este e-mail");
        }

        Usuario usuario = new Usuario();
        usuario.nome = req.nome().trim();
        usuario.cpf = cpf;
        usuario.email = email;
        usuario.perfil = Perfil.CLIENTE;
        String senha = SenhaProvisoria.gerar();
        usuario.definirSenhaProvisoria(senha);
        usuario.persist();

        Conta conta = new Conta();
        conta.titular = usuario;
        conta.numero = numeroLivre();
        conta.persist();
        return new Cadastro(conta, senha);
    }

    /** Cliente esqueceu a senha: o administrador gera outra provisória (e o acesso é desbloqueado). */
    @Transactional
    public String novaSenhaProvisoria(Long id) {
        Usuario usuario = buscar(id);
        String senha = SenhaProvisoria.gerar();
        usuario.definirSenhaProvisoria(senha);
        return senha;
    }

    @Transactional
    public Usuario editar(Long id, EditarClienteRequest req) {
        Usuario usuario = buscar(id);
        String email = req.email().trim().toLowerCase();
        if (Usuario.existeEmail(email, id)) {
            throw new RegraNegocioException("Já existe um cliente com este e-mail");
        }
        usuario.nome = req.nome().trim();
        usuario.email = email;
        return usuario;
    }

    /**
     * Sorteia um número de conta ainda não usado. Os bancos não usam números sequenciais para
     * não revelar quantos clientes têm; a coluna é única no banco, então repetição nunca é gravada.
     */
    private String numeroLivre() {
        String numero;
        do {
            numero = Conta.gerarNumero(ThreadLocalRandom.current().nextInt(1, 1_000_000));
        } while (Conta.porNumero(numero) != null);
        return numero;
    }

    public Usuario buscar(Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null || usuario.perfil != Perfil.CLIENTE) {
            throw new NaoEncontradoException("Cliente não encontrado");
        }
        return usuario;
    }
}
