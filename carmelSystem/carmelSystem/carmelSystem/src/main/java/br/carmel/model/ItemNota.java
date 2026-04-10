package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "item_nota")
public class ItemNota {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "nota_id")
    private NotaTransferencia nota;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_unitario", precision = 10, scale = 2)
    private BigDecimal precoUnitario; // custo unitário para entradas

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    public void calcularSubtotal() {
        if (precoUnitario != null && quantidade != null)
            subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        else
            subtotal = BigDecimal.ZERO;
    }
}