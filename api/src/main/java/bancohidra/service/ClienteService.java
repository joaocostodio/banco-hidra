package bancohidra.service;

import bancohidra.dto.AuthDtos.CadastroRequest;
import bancohidra.dto.ClienteDtos.EditarClienteRequest;
import bancohidra.dto.ClienteDtos.NovoClienteRequest;
import bancohidra.entity.Conta;
import bancohidra.entity.Perfil;
import bancohidra.entity.ProdutoInvestimento;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoConta;
import bancohidra.entity.Usuario;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import bancohidra.security.SenhaProvisoria;
import bancohidra.util.Cpf;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.concurrent.ThreadLocalRandom;

/** Cadastro de clientes (pelo administrador ou pela própria pessoa) e abertura de contas. */
@ApplicationScoped
public class ClienteService {

    /** A senha provisória só existe aqui, em texto: no banco fica apenas o hash. */
    public record Cadastro(Conta conta, String senhaProvisoria) {
    }

    /** Cadastro pelo administrador: abre a conta corrente (saldo zero) e gera a senha provisória. */
    @Transactional
    public Cadastro cadastrar(NovoClienteRequest req) {
        Usuario usuario = novoCliente(req.nome(), req.cpf(), req.email());
        String senha = SenhaProvisoria.gerar();
        usuario.definirSenhaProvisoria(senha);
        usuario.persist();
        return new Cadastro(abrirConta(usuario, Conta.NOME_PRINCIPAL, TipoConta.CORRENTE, null), senha);
    }

    /** Cadastro pela própria pessoa ("Criar conta"): ela já escolhe a senha, que precisa ser forte. */
    @Transactional
    public Usuario cadastrarProprio(CadastroRequest req) {
        Usuario usuario = novoCliente(req.nome(), req.cpf(), req.email());
        usuario.definirSenha(req.senha());
        usuario.persist();
        abrirConta(usuario, Conta.NOME_PRINCIPAL, TipoConta.CORRENTE, null);
        return usuario;
    }

    /**
     * Abre mais uma conta para o cliente, com saldo zero. A fábrica {@link Conta#nova} cria a subclasse certa
     * (o investimento exige o produto). Sem nome, fica o padrão do tipo ("Poupança", "Carteira"...).
     */
    @Transactional
    public Conta abrirConta(Usuario titular, String nome, TipoConta tipo, ProdutoInvestimento produto) {
        if (Conta.abertasDe(titular) >= Conta.LIMITE_POR_CLIENTE) {
            throw new RegraNegocioException("Limite de " + Conta.LIMITE_POR_CLIENTE + " contas abertas atingido");
        }
        Conta conta = Conta.nova(tipo, produto);
        conta.titular = titular;
        if (nome != null && !nome.isBlank()) {
            conta.nome = nome.trim();
        }
        conta.numero = numeroLivre();
        conta.persist();
        return conta;
    }

    /** Encerra uma conta do cliente. A principal fica: é a das chaves Pix e dos boletos. */
    @Transactional
    public Conta encerrarConta(Usuario titular, Long contaId) {
        Conta conta = contaDo(titular, contaId);
        if (conta.id.equals(Conta.principalDe(titular).id)) {
            throw new RegraNegocioException("A conta principal não pode ser encerrada");
        }
        conta.encerrar(); // só com saldo zero
        return conta;
    }

    /** Conta do cliente pelo id. Conta de outra pessoa responde "não encontrada" (não revela que existe). */
    public Conta contaDo(Usuario titular, Long contaId) {
        Conta conta = contaId == null ? null : Conta.findById(contaId);
        if (conta == null || !conta.titular.id.equals(titular.id)) {
            throw new NaoEncontradoException("Conta não encontrada");
        }
        return conta;
    }

    /** Conta do cliente que pode movimentar dinheiro (não encerrada). */
    public Conta contaAbertaDo(Usuario titular, Long contaId) {
        Conta conta = contaDo(titular, contaId);
        if (conta.status == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("A conta " + conta.nome + " está encerrada");
        }
        return conta;
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

    public Usuario buscar(Long id) {
        Usuario usuario = Usuario.findById(id);
        if (usuario == null || usuario.perfil != Perfil.CLIENTE) {
            throw new NaoEncontradoException("Cliente não encontrado");
        }
        return usuario;
    }

    /** Valida os dados comuns aos dois cadastros e monta o usuário (ainda sem senha e sem gravar). */
    private static Usuario novoCliente(String nome, String cpfInformado, String emailInformado) {
        String cpf = Cpf.somenteDigitos(cpfInformado);
        if (!Cpf.valido(cpf)) {
            throw new RegraNegocioException("CPF inválido");
        }
        if (Usuario.existeCpf(cpf)) {
            throw new RegraNegocioException("Já existe um cliente com este CPF");
        }
        String email = emailInformado.trim().toLowerCase();
        if (Usuario.existeEmail(email, null)) {
            throw new RegraNegocioException("Já existe um cliente com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.nome = nome.trim();
        usuario.cpf = cpf;
        usuario.email = email;
        usuario.perfil = Perfil.CLIENTE;
        return usuario;
    }

    /**
     * Sorteia um número de conta ainda não usado. Os bancos não usam números sequenciais para
     * não revelar quantos clientes têm; a coluna é única no banco, então repetição nunca é gravada.
     */
    private static String numeroLivre() {
        String numero;
        do {
            numero = Conta.gerarNumero(ThreadLocalRandom.current().nextInt(1, 1_000_000));
        } while (Conta.porNumero(numero) != null);
        return numero;
    }
}
