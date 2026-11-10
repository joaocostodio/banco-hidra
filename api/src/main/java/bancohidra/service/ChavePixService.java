package bancohidra.service;

import bancohidra.entity.ChavePix;
import bancohidra.entity.Conta;
import bancohidra.entity.StatusConta;
import bancohidra.entity.TipoChavePix;
import bancohidra.exception.NaoEncontradoException;
import bancohidra.exception.RegraNegocioException;
import bancohidra.util.Cpf;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class ChavePixService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern UUID_FORMATO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    @Transactional
    public ChavePix cadastrar(Long contaId, TipoChavePix tipo, String valorInformado) {
        Conta conta = Conta.findById(contaId);
        if (conta == null) {
            throw new NaoEncontradoException("Conta não encontrada");
        }
        if (conta.status == StatusConta.ENCERRADA) {
            throw new RegraNegocioException("Conta encerrada não pode ter chaves Pix");
        }
        if (ChavePix.daConta(conta).size() >= ChavePix.LIMITE_POR_CONTA) {
            throw new RegraNegocioException("Limite de " + ChavePix.LIMITE_POR_CONTA + " chaves por conta atingido");
        }

        String valor = tipo == TipoChavePix.ALEATORIA ? UUID.randomUUID().toString() : normalizar(tipo, valorInformado);
        if (tipo == TipoChavePix.CPF && !valor.equals(conta.titular.cpf)) {
            throw new RegraNegocioException("A chave CPF deve ser o CPF do titular da conta");
        }
        if (ChavePix.porValor(valor) != null) {
            throw new RegraNegocioException("Esta chave já está cadastrada");
        }

        ChavePix chave = new ChavePix();
        chave.tipo = tipo;
        chave.valor = valor;
        chave.conta = conta;
        chave.persist();
        return chave;
    }

    @Transactional
    public void excluir(Long contaId, Long chaveId) {
        ChavePix chave = ChavePix.findById(chaveId);
        if (chave == null || !chave.conta.id.equals(contaId)) {
            throw new NaoEncontradoException("Chave Pix não encontrada");
        }
        chave.delete();
    }

    /**
     * Encontra a chave a partir do que o usuário digitou, tentando o formato de cada tipo
     * (ex.: "123.456.789-09" acha a chave CPF; "(55) 99999-0000" acha a chave telefone).
     */
    public ChavePix resolver(String digitado) {
        String texto = digitado == null ? "" : digitado.trim();
        for (TipoChavePix tipo : tiposPossiveis(texto)) {
            try {
                ChavePix chave = ChavePix.porValor(normalizar(tipo, texto));
                if (chave != null && chave.tipo == tipo) {
                    return chave;
                }
            } catch (RegraNegocioException formatoNaoServe) {
                // o texto não tem o formato deste tipo; tenta o próximo
            }
        }
        throw new NaoEncontradoException("Chave Pix não encontrada");
    }

    /**
     * Pelo formato do texto, quais tipos de chave ele pode ser. Só números com 11 dígitos é
     * ambíguo (CPF ou celular), então tenta os dois.
     */
    private static List<TipoChavePix> tiposPossiveis(String texto) {
        if (UUID_FORMATO.matcher(texto.toLowerCase(Locale.ROOT)).matches()) {
            return List.of(TipoChavePix.ALEATORIA);
        }
        if (texto.contains("@")) {
            return List.of(TipoChavePix.EMAIL);
        }
        if (texto.startsWith("+")) {
            return List.of(TipoChavePix.TELEFONE);
        }
        return List.of(TipoChavePix.CPF, TipoChavePix.TELEFONE);
    }

    /** Deixa a chave num formato único para guardar e comparar. */
    static String normalizar(TipoChavePix tipo, String valor) {
        String v = valor == null ? "" : valor.trim();
        switch (tipo) {
            case CPF -> {
                if (!Cpf.valido(v)) {
                    throw new RegraNegocioException("CPF inválido");
                }
                return Cpf.somenteDigitos(v);
            }
            case EMAIL -> {
                String email = v.toLowerCase(Locale.ROOT);
                if (!EMAIL.matcher(email).matches()) {
                    throw new RegraNegocioException("E-mail inválido");
                }
                return email;
            }
            case TELEFONE -> {
                String digitos = v.replaceAll("\\D", "");
                if (digitos.length() == 13 && digitos.startsWith("55")) {
                    digitos = digitos.substring(2);
                }
                if (digitos.length() != 11 || digitos.charAt(2) != '9') {
                    throw new RegraNegocioException("Telefone inválido: use DDD + celular, ex. (55) 99999-0000");
                }
                return "+55" + digitos;
            }
            case ALEATORIA -> {
                String chave = v.toLowerCase(Locale.ROOT);
                if (!UUID_FORMATO.matcher(chave).matches()) {
                    throw new RegraNegocioException("Chave aleatória inválida");
                }
                return chave;
            }
            default -> throw new IllegalArgumentException(tipo.name());
        }
    }
}
