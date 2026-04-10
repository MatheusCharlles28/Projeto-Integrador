package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Cliente Pessoa Jurídica (empresa que compra).
 * Diferente de Fornecedor — este é cliente, não vendedor.
 */
@Getter @Setter
@Entity
@Table(name = "cliente_juridico")
public class ClienteJuridico {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String razaoSocial;

    @Column(length = 150)
    private String nomeFantasia;

    @Column(unique = true, length = 20)
    private String cnpj;

    @Column(name = "inscricao_estadual", length = 30)
    private String inscricaoEstadual;

    @Column(length = 20)
    private String telefone;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String contato; // nome do responsável

    @Column(length = 10)
    private String cep;

    @Column(length = 200)
    private String logradouro;

    @Column(length = 10)
    private String numero;

    @Column(length = 100)
    private String bairro;

    @Column(length = 100)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Override
    public String toString() {
        return nomeFantasia != null && !nomeFantasia.isEmpty() ? nomeFantasia : razaoSocial;
    }
}