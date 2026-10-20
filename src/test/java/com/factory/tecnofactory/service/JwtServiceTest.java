package com.factory.tecnofactory.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRETO = "una-clave-de-prueba-con-al-menos-32-caracteres";
    private static final long UNA_HORA = 3_600_000L;

    private final JwtService jwt = new JwtService(SECRETO, UNA_HORA);

    @Test
    void tokenGeneradoEsValidoYGuardaCorreoYRol() {
        String token = jwt.generarToken("ana@correo.cl", "CLIENTE");

        assertTrue(jwt.esValido(token));
        assertEquals("ana@correo.cl", jwt.obtenerCorreo(token));
        assertEquals("CLIENTE", jwt.obtenerRol(token));
    }

    @Test
    void tokenExpiradoNoEsValido() {
        JwtService conExpiracionPasada = new JwtService(SECRETO, -1000L);

        assertFalse(jwt.esValido(conExpiracionPasada.generarToken("ana@correo.cl", "CLIENTE")));
    }

    @Test
    void tokenFirmadoConOtraClaveNoEsValido() {
        JwtService otro = new JwtService("otra-clave-distinta-de-al-menos-32-caracteres", UNA_HORA);

        assertFalse(jwt.esValido(otro.generarToken("ana@correo.cl", "CLIENTE")));
    }

    @Test
    void tokenAlteradoNoEsValido() {
        String token = jwt.generarToken("ana@correo.cl", "CLIENTE");

        assertFalse(jwt.esValido(token + "a"));
    }

    @Test
    void textoQueNoEsTokenNoEsValido() {
        assertFalse(jwt.esValido("esto-no-es-un-token"));
        assertFalse(jwt.esValido(""));
        assertFalse(jwt.esValido(null));
    }

    @Test
    void secretoDeMenosDe32BytesNoSePermite() {
        assertThrows(JwtException.class, () -> new JwtService("corto", UNA_HORA));
    }
}
