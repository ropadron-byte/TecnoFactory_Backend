package com.factory.tecnofactory.config;

import com.factory.tecnofactory.util.ValidadorClaves;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * Falla el arranque si se copió .env.example sin cambiar los valores de ejemplo.
 * Nunca muestra el valor de una clave: solo el nombre de la variable.
 */
@Component
@RequiredArgsConstructor
public class ConfiguracionValidator {

    private static final int BYTES_MINIMOS_JWT = 32;

    private final Environment environment;

    @PostConstruct
    public void validar() {
        validarPlaceholder("JWT_SECRET");
        validarPlaceholder("DB_PASSWORD");
        validarPlaceholder("MIGRATOR_PASSWORD");
        validarPlaceholder("APP_DB_PASSWORD");
        validarPlaceholder("ADMIN_PASSWORD");

        String jwt = environment.getProperty("JWT_SECRET");
        if (!StringUtils.hasText(jwt)) {
            throw new IllegalStateException("JWT_SECRET es obligatoria");
        }
        if (jwt.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_JWT) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }

        String adminPassword = environment.getProperty("ADMIN_PASSWORD");
        if (StringUtils.hasText(adminPassword)) {
            ValidadorClaves.validar("ADMIN_PASSWORD", adminPassword);
        }
    }

    private void validarPlaceholder(String nombreVariable) {
        String valor = environment.getProperty(nombreVariable);
        if (ValidadorClaves.esPlaceholder(valor)) {
            throw new IllegalStateException(
                    nombreVariable + " conserva el valor de ejemplo de .env.example; genera uno real");
        }
    }
}
