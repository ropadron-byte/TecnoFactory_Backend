package com.factory.tecnofactory.util;

import java.nio.charset.StandardCharsets;

/** Validaciones de claves que llegan por configuración, fuera de un DTO. */
public final class ValidadorClaves {

    private static final int LARGO_MINIMO = 8;
    private static final int LARGO_MAXIMO = 72;
    private static final int BYTES_MAXIMOS_BCRYPT = 72;

    private ValidadorClaves() {
    }

    /**
     * BCrypt solo usa los primeros 72 bytes. Se valida el largo en caracteres y en bytes
     * para no aceptar una clave que luego se corte en silencio.
     */
    public static void validar(String nombreVariable, String clave) {
        int bytes = clave == null ? 0 : clave.getBytes(StandardCharsets.UTF_8).length;
        if (clave == null
                || clave.length() < LARGO_MINIMO
                || clave.length() > LARGO_MAXIMO
                || bytes > BYTES_MAXIMOS_BCRYPT) {
            throw new IllegalStateException(
                    nombreVariable + " debe tener entre 8 y 72 caracteres y no superar 72 bytes UTF-8");
        }
    }

    public static boolean esPlaceholder(String valor) {
        if (valor == null) {
            return false;
        }
        String normalizado = valor.trim().toLowerCase(java.util.Locale.ROOT);
        return normalizado.contains("cambia_esta_clave")
                || normalizado.contains("cambiar_esta_clave")
                || normalizado.equals("cambiar")
                || normalizado.equals("<cambiar>");
    }
}
