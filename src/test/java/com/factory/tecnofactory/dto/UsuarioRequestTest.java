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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioRequestTest {

    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static UsuarioRequest crear(String runDv, String correo, String clave,
                                        LocalDate fecha, String telefono) {
        return new UsuarioRequest(12345678, runDv, "Ana", "Pérez", correo, clave,
                fecha, "Calle Falsa 123", telefono, 13101);
    }

    private static UsuarioRequest valido() {
        return crear("5", "ana@correo.cl", "Clave12345",
                LocalDate.of(1990, 5, 20), "+56912345678");
    }

    private static Set<String> camposConError(UsuarioRequest request) {
        Set<ConstraintViolation<UsuarioRequest>> errores = validator.validate(request);
        return errores.stream()
                .map(e -> e.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void datosValidosNoGeneranErrores() {
        assertTrue(camposConError(valido()).isEmpty());
    }

    @Test
    void correoSinFormatoEsInvalido() {
        UsuarioRequest r = crear("5", "no-es-un-correo", "Clave12345",
                LocalDate.of(1990, 5, 20), "+56912345678");
        assertEquals(Set.of("correo"), camposConError(r));
    }

    @Test
    void claveCortaEsInvalida() {
        UsuarioRequest r = crear("5", "ana@correo.cl", "abc",
                LocalDate.of(1990, 5, 20), "+56912345678");
        assertEquals(Set.of("clave"), camposConError(r));
    }

    @Test
    void digitoVerificadorFueraDeRangoEsInvalido() {
        UsuarioRequest r = crear("Z", "ana@correo.cl", "Clave12345",
                LocalDate.of(1990, 5, 20), "+56912345678");
        assertEquals(Set.of("runDv"), camposConError(r));
    }

    @Test
    void fechaFuturaEsInvalida() {
        UsuarioRequest r = crear("5", "ana@correo.cl", "Clave12345",
                LocalDate.now().plusDays(1), "+56912345678");
        assertEquals(Set.of("fechaNacimiento"), camposConError(r));
    }

    @Test
    void telefonoConLetrasEsInvalido() {
        UsuarioRequest r = crear("5", "ana@correo.cl", "Clave12345",
                LocalDate.of(1990, 5, 20), "abc");
        assertEquals(Set.of("telefono"), camposConError(r));
    }

    @Test
    void todosLosCamposSonObligatorios() {
        UsuarioRequest vacio = new UsuarioRequest(null, null, null, null, null,
                null, null, null, null, null);
        assertEquals(10, camposConError(vacio).size());
    }

    @Test
    void toStringNoRevelaLaClave() {
        assertFalse(valido().toString().contains("Clave12345"));
    }
}