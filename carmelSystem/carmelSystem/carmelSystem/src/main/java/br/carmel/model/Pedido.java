package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItensPedido> itensPedidos = new ArrayList<>();

    @Column(name = "data_pedido", nullable = false)
    private LocalDateTime dataPedido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPedido status;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "observacoes", length = 500)
    private String observacoes;

    public void calcularValorTotal() {
        this.valorTotal = itensPedidos.stream()
                .map(item -> item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void adicionarItem(ItensPedido item) {
        item.calcularSubtotal(); // Calcular subtotal ANTES de adicionar
        itensPedidos.add(item);
        item.setPedido(this);
        calcularValorTotal();
    }

    public void removerItem(ItensPedido item) {
        itensPedidos.remove(item);
        item.setPedido(null);
        calcularValorTotal();
    }

    @PrePersist
    protected void onCreate() {
        if (dataPedido == null) {
            dataPedido = LocalDateTime.now();
        }
        if (status == null) {
            status = StatusPedido.PENDENTE;
        }
    }
}