package br.carmel.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Validações baseadas nas constraints do modelo JPA.
 * Centraliza todas as regras para reutilizar nos painéis.
 */
public final class Validator {

    private Validator() {}

    // ── Resultado de validação ─────────────────────────────────────────────────

    public static class Result {
        private final List<String> erros = new ArrayList<>();

        public void addErro(String msg) { erros.add("• " + msg); }
        public boolean isValido()       { return erros.isEmpty(); }
        public String getMensagem()     { return String.join("\n", erros); }
    }

    // ── Cliente ───────────────────────────────────────────────────────────────
    // nome: nullable=false, unique=true, length=100
    // cpf:  nullable=false, unique=true, length=100
    // telefone: nullable=false, length=100
    // email: opcional, length=100

    public static Result validarCliente(String nome, String cpf, String telefone,
                                        String email, String cep, String logradouro,
                                        String bairro, String cidade, String uf,
                                        String numeroStr) {
        Result r = new Result();

        // Nome
        if (isBlank(nome))           r.addErro("Nome é obrigatório.");
        else if (nome.length() > 100) r.addErro("Nome deve ter no máximo 100 caracteres.");

        // CPF
        if (isBlank(cpf))            r.addErro("CPF é obrigatório.");
        else if (!isCpfValido(cpf))  r.addErro("CPF inválido. Use o formato 000.000.000-00 ou somente números.");

        // Telefone
        if (isBlank(telefone))          r.addErro("Telefone é obrigatório.");
        else if (telefone.length() > 100) r.addErro("Telefone deve ter no máximo 100 caracteres.");

        // Email (opcional, mas se preenchido valida formato)
        if (!isBlank(email)) {
            if (email.length() > 100)    r.addErro("E-mail deve ter no máximo 100 caracteres.");
            else if (!email.contains("@") || !email.contains("."))
                r.addErro("E-mail inválido.");
        }

        // Endereço - pelo menos CEP ou logradouro obrigatório (numero nullable=false no model)
        if (isBlank(logradouro))     r.addErro("Logradouro é obrigatório.");
        if (isBlank(bairro))         r.addErro("Bairro é obrigatório.");
        if (isBlank(cidade))         r.addErro("Cidade é obrigatória.");
        if (isBlank(uf))             r.addErro("UF é obrigatória.");
        else if (uf.length() != 2)   r.addErro("UF deve ter 2 letras (ex: PR).");

        // Número (int nullable=false)
        if (isBlank(numeroStr))      r.addErro("Número do endereço é obrigatório.");
        else {
            try { Integer.parseInt(numeroStr.trim()); }
            catch (NumberFormatException e) { r.addErro("Número do endereço deve ser numérico."); }
        }

        return r;
    }

    // ── Produto ───────────────────────────────────────────────────────────────
    // nome:        nullable=false, unique=true, length=100
    // valor:       nullable=false
    // codBarras:   nullable=false, unique=true, length=20
    // numeroSerie: nullable=false, unique=true, length=20

    public static Result validarProduto(String nome, String valorVendaStr,
                                        String valorCustoStr,
                                        String codBarras, String numeroSerie,
                                        String descricao) {
        Result r = new Result();

        if (isBlank(nome))            r.addErro("Nome é obrigatório.");
        else if (nome.length() > 100) r.addErro("Nome deve ter no máximo 100 caracteres.");

        if (!isBlank(descricao) && descricao.length() > 500)
            r.addErro("Descrição deve ter no máximo 500 caracteres.");

        if (isBlank(valorVendaStr)) {
            r.addErro("Preço de venda é obrigatório.");
        } else {
            try {
                BigDecimal v = new BigDecimal(valorVendaStr.trim().replace(",", "."));
                if (v.compareTo(BigDecimal.ZERO) <= 0)
                    r.addErro("Preço de venda deve ser maior que zero.");
            } catch (NumberFormatException e) {
                r.addErro("Preço de venda inválido. Use ponto ou vírgula (ex: 10.50).");
            }
        }

        if (!isBlank(valorCustoStr)) {
            try {
                BigDecimal c = new BigDecimal(valorCustoStr.trim().replace(",", "."));
                if (c.compareTo(BigDecimal.ZERO) < 0)
                    r.addErro("Preço de custo não pode ser negativo.");
            } catch (NumberFormatException e) {
                r.addErro("Preço de custo inválido. Use ponto ou vírgula (ex: 5.00).");
            }
        }

        if (!isBlank(codBarras) && codBarras.length() > 20)
            r.addErro("Código de barras deve ter no máximo 20 caracteres.");

        if (!isBlank(numeroSerie) && numeroSerie.length() > 20)
            r.addErro("Número de série deve ter no máximo 20 caracteres.");

        return r;
    }

    // Mantém compatibilidade com chamadas antigas sem descricao
    public static Result validarProduto(String nome, String valorVendaStr,
                                        String valorCustoStr,
                                        String codBarras, String numeroSerie) {
        return validarProduto(nome, valorVendaStr, valorCustoStr, codBarras, numeroSerie, "");
    }

    // ── ItensPedido ───────────────────────────────────────────────────────────
    // quantidade: nullable=false, deve ser > 0

    public static Result validarItem(String qtdStr) {
        Result r = new Result();
        if (isBlank(qtdStr)) {
            r.addErro("Quantidade é obrigatória.");
        } else {
            try {
                int q = Integer.parseInt(qtdStr.trim());
                if (q <= 0) r.addErro("Quantidade deve ser maior que zero.");
                if (q > 9999) r.addErro("Quantidade máxima é 9999.");
            } catch (NumberFormatException e) {
                r.addErro("Quantidade deve ser um número inteiro.");
            }
        }
        return r;
    }

    // ── CPF ───────────────────────────────────────────────────────────────────

    public static boolean isCpfValido(String cpf) {
        String s = cpf.replaceAll("[^0-9]", "");
        if (s.length() != 11) return false;
        // Rejeita sequências iguais (111.111.111-11 etc)
        if (s.chars().distinct().count() == 1) return false;
        // Valida dígitos verificadores
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (s.charAt(i) - '0') * (10 - i);
        int d1 = 11 - (sum % 11); if (d1 >= 10) d1 = 0;
        if (d1 != (s.charAt(9) - '0')) return false;
        sum = 0;
        for (int i = 0; i < 10; i++) sum += (s.charAt(i) - '0') * (11 - i);
        int d2 = 11 - (sum % 11); if (d2 >= 10) d2 = 0;
        return d2 == (s.charAt(10) - '0');
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Formata CPF para exibição: 000.000.000-00 */
    public static String formatarCpf(String cpf) {
        String s = cpf.replaceAll("[^0-9]", "");
        if (s.length() != 11) return cpf;
        return s.substring(0,3) + "." + s.substring(3,6) + "." +
                s.substring(6,9) + "-" + s.substring(9);
    }

    /** Formata CEP para exibição: 00000-000 */
    public static String formatarCep(String cep) {
        String s = cep.replaceAll("[^0-9]", "");
        if (s.length() != 8) return cep;
        return s.substring(0,5) + "-" + s.substring(5);
    }

    /** Formata telefone: (00) 00000-0000 ou (00) 0000-0000 */
    public static String formatarTelefone(String tel) {
        String s = tel.replaceAll("[^0-9]", "");
        if (s.length() == 11)
            return "(" + s.substring(0,2) + ") " + s.substring(2,7) + "-" + s.substring(7);
        if (s.length() == 10)
            return "(" + s.substring(0,2) + ") " + s.substring(2,6) + "-" + s.substring(6);
        return tel;
    }

    /** Converte string de preço aceitando vírgula ou ponto */
    public static BigDecimal parseBigDecimal(String s) {
        return new BigDecimal(s.trim().replace(",", "."));
    }
}