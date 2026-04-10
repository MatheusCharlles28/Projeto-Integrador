package br.carmel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    @Column(nullable = false, length = 200)
    private String senha; // armazenado como hash simples SHA-256

    @Column(nullable = false, length = 100)
    private String nomeCompleto;
}