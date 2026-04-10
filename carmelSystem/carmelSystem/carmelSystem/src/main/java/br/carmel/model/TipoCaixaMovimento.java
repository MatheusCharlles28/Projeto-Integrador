package br.carmel.model;

public enum TipoCaixaMovimento {
    ABERTURA,
    VENDA,
    SANGRIA,
    SUPRIMENTO,
    FECHAMENTO;

    @Override
    public String toString() {
        return switch (this) {
            case ABERTURA    -> "Abertura";
            case VENDA       -> "Venda";
            case SANGRIA     -> "Sangria";
            case SUPRIMENTO  -> "Suprimento";
            case FECHAMENTO  -> "Fechamento";
        };
    }
}