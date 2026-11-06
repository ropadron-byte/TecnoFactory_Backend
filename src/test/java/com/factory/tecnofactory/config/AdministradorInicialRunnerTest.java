package com.factory.tecnofactory.config;

import com.factory.tecnofactory.service.DatosAdministrador;
import com.factory.tecnofactory.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AdministradorInicialRunnerTest {

    private UsuarioService usuarioService;
    private AdministradorInicialRunner runner;

    @BeforeEach
    void preparar() {
        usuarioService = mock(UsuarioService.class);
        runner = new AdministradorInicialRunner(usuarioService);
        ReflectionTestUtils.setField(runner, "adminEmail", "admin@tecnofactory.cl");
        ReflectionTestUtils.setField(runner, "adminPassword", "ClaveSegura123");
        ReflectionTestUtils.setField(runner, "adminRun", "11111111");
        ReflectionTestUtils.setField(runner, "adminRunDv", "1");
        ReflectionTestUtils.setField(runner, "adminNombre", "Administrador");
        ReflectionTestUtils.setField(runner, "adminApellidos", "TecnoFactory");
        ReflectionTestUtils.setField(runner, "adminFechaNacimiento", "2000-01-01");
        ReflectionTestUtils.setField(runner, "adminDireccion", "Calle 1");
        ReflectionTestUtils.setField(runner, "adminTelefono", "+56912345678");
        ReflectionTestUtils.setField(runner, "adminComuna", "13101");
    }

    @Test
    void creaElAdministradorConLosDatosDelEntorno() {
        runner.run(null);

        ArgumentCaptor<DatosAdministrador> captor = ArgumentCaptor.forClass(DatosAdministrador.class);
        verify(usuarioService).crearAdministradorSiNoExiste(captor.capture());
        DatosAdministrador datos = captor.getValue();

        assertEquals(11111111, datos.run());
        assertEquals("1", datos.runDv());
        assertEquals("Administrador", datos.nombre());
        assertEquals("admin@tecnofactory.cl", datos.correo());
        assertEquals(LocalDate.of(2000, 1, 1), datos.fechaNacimiento());
        assertEquals(13101, datos.idComuna());
    }

    @Test
    void noCreaNadaSiFaltaElCorreoOLaClave() {
        ReflectionTestUtils.setField(runner, "adminPassword", "");

        runner.run(null);

        verify(usuarioService, never()).crearAdministradorSiNoExiste(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaUnaClaveDeAdministradorCorta() {
        ReflectionTestUtils.setField(runner, "adminPassword", "corta");

        assertThrows(IllegalStateException.class, () -> runner.run(null));
        verify(usuarioService, never()).crearAdministradorSiNoExiste(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaElPlaceholderDeLaClaveDelAdministrador() {
        ReflectionTestUtils.setField(runner, "adminPassword", "cambia_esta_clave");

        assertThrows(IllegalStateException.class, () -> runner.run(null));
    }

    @Test
    void rechazaUnaFechaDeNacimientoFutura() {
        ReflectionTestUtils.setField(runner, "adminFechaNacimiento", "2030-01-01");

        assertThrows(IllegalStateException.class, () -> runner.run(null));
    }

    @Test
    void rechazaUnCorreoInvalido() {
        ReflectionTestUtils.setField(runner, "adminEmail", "sin-arroba");

        assertThrows(IllegalStateException.class, () -> runner.run(null));
    }
}
