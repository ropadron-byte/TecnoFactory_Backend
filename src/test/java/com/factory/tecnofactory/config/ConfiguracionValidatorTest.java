package com.factory.tecnofactory.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfiguracionValidatorTest {

    private static final String JWT_VALIDO = "clave-de-firma-de-al-menos-32-bytes";
    private static final String GOOGLE_CLIENT_ID = "123.apps.googleusercontent.com";

    @Test
    void aceptaConfiguracionValida() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", JWT_VALIDO)
                .withProperty("GOOGLE_CLIENT_ID", GOOGLE_CLIENT_ID);

        assertDoesNotThrow(() -> new ConfiguracionValidator(entorno).validar());
    }

    @Test
    void rechazaGoogleClientIdFaltante() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", JWT_VALIDO);

        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionValidator(entorno).validar());
    }

    @Test
    void rechazaElPlaceholderDeGoogle() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", JWT_VALIDO)
                .withProperty("GOOGLE_CLIENT_ID", "CAMBIAR_CON_TU_CLIENT_ID.apps.googleusercontent.com");

        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionValidator(entorno).validar());
    }

    @Test
    void rechazaElPlaceholderDeJwt() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", "cambia_esta_clave_por_una_de_al_menos_32_caracteres");

        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionValidator(entorno).validar());
    }

    @Test
    void rechazaJwtDemasiadoCorto() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", "corta");

        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionValidator(entorno).validar());
    }

    @Test
    void rechazaClavesDeBaseDeDatosDeEjemplo() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", JWT_VALIDO)
                .withProperty("DB_PASSWORD", "CAMBIAR");

        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionValidator(entorno).validar());
    }

    @Test
    void rechazaUnaClaveDeAdministradorDemasiadoCorta() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("JWT_SECRET", JWT_VALIDO)
                .withProperty("ADMIN_PASSWORD", "corta");

        assertThrows(IllegalStateException.class,
                () -> new ConfiguracionValidator(entorno).validar());
    }
}
