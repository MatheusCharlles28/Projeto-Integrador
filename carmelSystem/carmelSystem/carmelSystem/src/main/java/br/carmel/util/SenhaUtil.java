package br.carmel.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utilitário para hash de senhas com SHA-256.
 */
public final class SenhaUtil {

    /** Senha do administrador do sistema (para cadastrar/excluir usuários). */
    public static final String SENHA_ADMIN = "matheus";

    private SenhaUtil() {}

    /** Converte uma string para SHA-256 em hexadecimal. */
    public static String hash(String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash da senha", e);
        }
    }

    /** Verifica se a senha em texto bate com o hash armazenado. */
    public static boolean verificar(String senhaTexto, String hashArmazenado) {
        return hash(senhaTexto).equals(hashArmazenado);
    }

    /** Verifica se é a senha de administrador. */
    public static boolean isAdmin(String senhaTexto) {
        return SENHA_ADMIN.equals(senhaTexto);
    }
}