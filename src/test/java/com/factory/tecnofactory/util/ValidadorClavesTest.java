package com.factory.tecnofactory.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorClavesTest {

    @Test
    void aceptaClavesDeEntreOchoYSetentaYDosCaracteres() {
        assertDoesNotThrow(() -> ValidadorClaves.validar("ADMIN_PASSWORD", "12345678"));
        assertDoesNotThrow(() -> ValidadorClaves.validar("ADMIN_PASSWORD", "a".repeat(72)));
    }

    @Test
    void rechazaClavesCortasOLargas() {
        assertThrows(IllegalStateException.class,
                () -> ValidadorClaves.validar("ADMIN_PASSWORD", "1234567"));
        assertThrows(IllegalStateException.class,
                () -> ValidadorClaves.validar("ADMIN_PASSWORD", "a".repeat(73)));
    }

    @Test
    void rechazaClavesQueSuperanLosSetentaYDosBytesUtf8() {
        // Cada 'á' ocupa dos bytes en UTF-8: 40 caracteres ya superan los 72 bytes de BCrypt.
        assertThrows(IllegalStateException.class,
                () -> ValidadorClaves.validar("ADMIN_PASSWORD", "á".repeat(40)));
    }

    @Test
    void detectaLosValoresDeEjemplo() {
        assertTrue(ValidadorClaves.esPlaceholder("cambia_esta_clave"));
        assertTrue(ValidadorClaves.esPlaceholder("CAMBIAR_ESTA_CLAVE_POR_UNA_ALEATORIA"));
        assertTrue(ValidadorClaves.esPlaceholder("CAMBIAR"));
        assertTrue(ValidadorClaves.esPlaceholder("<CAMBIAR>"));
        assertFalse(ValidadorClaves.esPlaceholder("ClaveReal12345"));
    }
}
