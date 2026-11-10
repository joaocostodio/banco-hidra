package bancohidra.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/** Pessoa que acessa o banco: um cliente (dono de conta) ou um administrador. */
@Entity
public class Usuario extends PanacheEntity {

    @Column(nullable = false, length = 150)
    public String nome;

    /** Somente os 11 dígitos. */
    @Column(nullable = false, unique = true, length = 11)
    public String cpf;

    @Column(nullable = false, unique = true, length = 150)
    public String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Perfil perfil = Perfil.CLIENTE;

    @Column(nullable = false)
    public LocalDateTime criadoEm = LocalDateTime.now();

    public static boolean existeCpf(String cpf) {
        return count("cpf", cpf) > 0;
    }

    public static boolean existeEmail(String email, Long ignorarId) {
        return count("lower(email) = lower(?1) and id <> ?2", email, ignorarId == null ? -1L : ignorarId) > 0;
    }
}
