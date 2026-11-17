package bancohidra.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Conta corrente: a do dia a dia. A primeira do cliente é a principal (chaves Pix e boletos). Não rende. */
@Entity
@DiscriminatorValue("CORRENTE")
public class ContaCorrente extends Conta {

    @Override
    public TipoConta getTipo() {
        return TipoConta.CORRENTE;
    }
}
