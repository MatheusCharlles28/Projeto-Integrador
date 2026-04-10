package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Nota de transferência interna de estoque (não fiscal).
 * Pode ser entrada (recebimento de fornecedor) ou saída (transferência/ajuste).
 */
@Getter @Setter
@Entity
@Table(name = "nota_transferencia")
public class NotaTransferencia {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ENTRADA = adiciona estoque | SAIDA = retira estoque */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoNota tipo;

    @ManyToOne
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor; // opcional — pode ser transferência interna

    @Column(name = "data_nota", nullable = false)
    private LocalDateTime dataNota;

    @Column(length = 500)
    private String observacoes;

    @Column(name = "numero_nota", length = 50)
    private String numeroNota; // número de referência livre

    @OneToMany(mappedBy = "nota", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemNota> itens = new ArrayList<>();

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    public void calcularTotal() {
        valorTotal = itens.stream()
                .map(ItemNota::getSubtotal)
                .filter(s -> s != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}