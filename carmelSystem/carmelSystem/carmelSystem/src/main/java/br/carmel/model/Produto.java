package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    /** Preço de venda (usado nos pedidos). */
    @Column(nullable = false)
    private BigDecimal valor;

    /** Preço de custo (para cálculo de margem). */
    @Column(name = "preco_custo", precision = 10, scale = 2)
    private BigDecimal precoCusto;

    /**
     * Preço médio ponderado de custo.
     * Calculado automaticamente ao atualizar o preço de custo:
     * novoMedio = (estoqueAtual * precoMedioAnterior + qtdAcumulada * novoPreco) / (estoqueAtual + qtdAcumulada)
     * Simplificado: média entre o médio anterior e o novo custo, ponderada pelo estoque.
     */
    @Column(name = "preco_medio", precision = 10, scale = 2)
    private BigDecimal precoMedio;

    /**
     * Quantidade total acumulada nas entradas de custo (para cálculo do preço médio).
     * Incrementada a cada vez que o preço de custo é alterado, usando o estoque atual como peso.
     */
    @Column(name = "qtd_historico_custo", precision = 10, scale = 2)
    private BigDecimal qtdHistoricoCusto = BigDecimal.ZERO;

    @Column(unique = true, length = 20)
    private String codBarras;

    @Column(unique = true, length = 20)
    private String numeroSerie;

    /** Quantidade em estoque. Só pode ser alterada via administrador. */
    @Column(name = "estoque", nullable = false)
    private Integer estoque = 0;

    @Override
    public String toString() {
        return nome != null ? nome : "";
    }
}