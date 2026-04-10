package br.carmel.model;

public enum FormaPagamento {
    DINHEIRO,
    CARTAO_DEBITO,
    CARTAO_CREDITO,
    PIX,
    BOLETO;

    @Override
    public String toString() {
        return switch (this) {
            case DINHEIRO       -> "Dinheiro";
            case CARTAO_DEBITO  -> "Cartão de Débito";
            case CARTAO_CREDITO -> "Cartão de Crédito";
            case PIX            -> "PIX";
            case BOLETO         -> "Boleto";
        };
    }
}