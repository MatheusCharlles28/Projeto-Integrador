package br.carmel.model;

public enum TipoNota {
    ENTRADA,
    SAIDA;

    @Override
    public String toString() {
        return this == ENTRADA ? "Entrada" : "Saída";
    }
}