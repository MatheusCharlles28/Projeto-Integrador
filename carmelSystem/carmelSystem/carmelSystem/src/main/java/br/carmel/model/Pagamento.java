package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de pagamento vinculado a um pedido.
 * Um pedido pode ter um pagamento associado.
 */
@Getter
@Setter
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "valor_pago", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "desconto", precision = 10, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    /** Valor final após desconto. É o valor cobrado de fato. */
    @Column(name = "valor_final", precision = 10, scale = 2)
    private BigDecimal valorFinal;

    @Column(name = "troco", precision = 10, scale = 2)
    private BigDecimal troco;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDateTime dataPagamento;

    @Column(name = "observacoes", length = 300)
    private String observacoes;

    @PrePersist
    protected void onCreate() {
        if (dataPagamento == null) dataPagamento = LocalDateTime.now();
        if (troco == null) troco = BigDecimal.ZERO;
        if (desconto == null) desconto = BigDecimal.ZERO;
        if (valorFinal == null) valorFinal = valorPago;
    }
}