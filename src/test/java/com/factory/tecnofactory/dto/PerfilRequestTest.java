package com.factory.tecnofactory.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilRequestTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static PerfilRequest crear(String runDv, LocalDate fecha, String telefono) {
        return new PerfilRequest(12345678, runDv, "Ana", "Pérez",
                fecha, "Calle Falsa 123", telefono, 13101);
    }

    private static PerfilRequest valido() {
        return crear("5", LocalDate.of(1990, 5, 20), "+56912345678");
    }

    private static Set<String> camposConError(PerfilRequest request) {
        Set<ConstraintViolation<PerfilRequest>> errores = validator.validate(request);
        return errores.stream()
                .map(e -> e.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void datosValidosNoGeneranErrores() {
        assertTrue(camposConError(valido()).isEmpty());
    }

    @Test
    void todosLosCamposSonObligatorios() {
        PerfilRequest vacio = new PerfilRequest(null, null, null, null,
                null, null, null, null);
        assertEquals(8, camposConError(vacio).size());
    }

    @Test
    void digitoVerificadorQueNoCorrespondeEsInvalido() {
        assertEquals(Set.of("runDv"), camposConError(crear("9", LocalDate.of(1990, 5, 20), "+56912345678")));
    }

    @Test
    void fechaFuturaEsInvalida() {
        assertEquals(Set.of("fechaNacimiento"),
                camposConError(crear("5", LocalDate.now().plusDays(1), "+56912345678")));
    }

    @Test
    void telefonoConLetrasEsInvalido() {
        assertEquals(Set.of("telefono"),
                camposConError(crear("5", LocalDate.of(1990, 5, 20), "abc")));
    }
}
