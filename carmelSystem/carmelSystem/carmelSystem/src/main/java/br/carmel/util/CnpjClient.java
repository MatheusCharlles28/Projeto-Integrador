package br.carmel.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente da BrasilAPI para consulta de CNPJ.
 * Endpoint: https://brasilapi.com.br/api/cnpj/v1/{cnpj}
 * Gratuito, sem autenticação, sem limite declarado.
 */
public class CnpjClient {

    public static class DadosEmpresa {
        public String razaoSocial;
        public String nomeFantasia;
        public String telefone;
        public String email;
        public String cep;
        public String logradouro;
        public String numero;
        public String complemento;
        public String bairro;
        public String cidade;
        public String uf;
        public String situacao; // ATIVA, BAIXADA, etc
    }

    /**
     * Consulta dados da empresa pelo CNPJ via BrasilAPI.
     * @param cnpj apenas dígitos (14 números)
     * @return DadosEmpresa ou null se não encontrado
     */
    public static DadosEmpresa consultar(String cnpj) throws Exception {
        String cnpjLimpo = cnpj.replaceAll("[^0-9]", "");
        if (cnpjLimpo.length() != 14) throw new Exception("CNPJ deve ter 14 dígitos.");

        URL url = new URL("https://brasilapi.com.br/api/cnpj/v1/" + cnpjLimpo);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");

        int status = conn.getResponseCode();
        if (status == 404) return null; // CNPJ não encontrado
        if (status != 200) throw new Exception("Erro na consulta: HTTP " + status);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        return parsearJson(sb.toString());
    }

    /**
     * Parser JSON manual — evita dependência externa.
     */
    private static DadosEmpresa parsearJson(String json) {
        DadosEmpresa d = new DadosEmpresa();
        d.razaoSocial  = extrair(json, "razao_social");
        d.nomeFantasia = extrair(json, "nome_fantasia");
        d.email        = extrair(json, "email");
        d.cep          = extrair(json, "cep");
        d.logradouro   = extrair(json, "logradouro");
        d.numero       = extrair(json, "numero");
        d.complemento  = extrair(json, "complemento");
        d.bairro       = extrair(json, "bairro");
        d.cidade       = extrair(json, "municipio");
        d.uf           = extrair(json, "uf");
        d.situacao     = extrair(json, "descricao_situacao_cadastral");

        // Telefone: concatena DDD + número
        String ddd = extrair(json, "ddd_telefone_1");
        String tel = extrair(json, "telefone_1");
        if (ddd != null && !ddd.isEmpty() && tel != null && !tel.isEmpty())
            d.telefone = "(" + ddd.trim() + ") " + tel.trim();
        else if (tel != null && !tel.isEmpty())
            d.telefone = tel.trim();

        return d;
    }

    /** Extrai valor de campo JSON simples (string ou número). */
    private static String extrair(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int idx = json.indexOf(chave);
        if (idx < 0) return "";
        int colon = json.indexOf(":", idx);
        if (colon < 0) return "";
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        if (start >= json.length()) return "";
        char first = json.charAt(start);
        if (first == '"') {
            // Valor string
            int end = json.indexOf("\"", start + 1);
            if (end < 0) return "";
            return json.substring(start + 1, end).trim();
        } else if (first == 'n') {
            return ""; // null
        } else {
            // Valor numérico ou boolean
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
            return json.substring(start, end).trim();
        }
    }
}