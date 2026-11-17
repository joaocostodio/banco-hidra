package bancohidra.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Carteira: o dinheiro em espécie, para registrar gastos feitos em dinheiro. Não rende. */
@Entity
@DiscriminatorValue("CARTEIRA")
public class ContaCarteira extends Conta {

    public ContaCarteira() {
        nome = "Carteira";
    }

    @Override
    public TipoConta getTipo() {
        return TipoConta.CARTEIRA;
    }
}
