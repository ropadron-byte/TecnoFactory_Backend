package com.factory.tecnofactory.config;

import com.factory.tecnofactory.service.DatosAdministrador;
import com.factory.tecnofactory.service.UsuarioService;
import com.factory.tecnofactory.util.ValidadorClaves;
import com.factory.tecnofactory.validation.RunUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdministradorInicialRunner implements ApplicationRunner {

    private final UsuarioService usuarioService;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_RUN:11111111}")
    private String adminRun;

    @Value("${ADMIN_RUN_DV:1}")
    private String adminRunDv;

    @Value("${ADMIN_NOMBRE:Administrador}")
    private String adminNombre;

    @Value("${ADMIN_APELLIDOS:TecnoFactory}")
    private String adminApellidos;

    @Value("${ADMIN_FECHA_NACIMIENTO:2000-01-01}")
    private String adminFechaNacimiento;

    @Value("${ADMIN_DIRECCION:Por definir}")
    private String adminDireccion;

    @Value("${ADMIN_TELEFONO:+56900000000}")
    private String adminTelefono;

    @Value("${ADMIN_COMUNA:13101}")
    private String adminComuna;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL o ADMIN_PASSWORD sin definir: no se crea el administrador inicial");
            return;
        }

        validarDatos();
        DatosAdministrador datos = new DatosAdministrador(
                parseEntero("ADMIN_RUN", adminRun, 1_000_000, 99_999_999),
                adminRunDv.trim().toUpperCase(),
                adminNombre.trim(),
                adminApellidos.trim(),
                adminEmail.trim(),
                adminPassword,
                parseFecha(),
                adminDireccion.trim(),
                adminTelefono.trim(),
                parseEntero("ADMIN_COMUNA", adminComuna, 1, Integer.MAX_VALUE)
        );

        if ("Por definir".equalsIgnoreCase(adminDireccion.trim())
                || "+56900000000".equals(adminTelefono.trim())) {
            log.warn("El administrador inicial usa datos de relleno; define ADMIN_DIRECCION y ADMIN_TELEFONO");
        }

        usuarioService.crearAdministradorSiNoExiste(datos);
    }

    private void validarDatos() {
        if (ValidadorClaves.esPlaceholder(adminPassword)) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD conserva el valor de ejemplo de .env.example; genera una clave real");
        }
        ValidadorClaves.validar("ADMIN_PASSWORD", adminPassword);

        if (!adminEmail.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalStateException("ADMIN_EMAIL no tiene un formato válido");
        }
        if (adminNombre.isBlank() || adminApellidos.isBlank()) {
            throw new IllegalStateException("ADMIN_NOMBRE y ADMIN_APELLIDOS son obligatorios");
        }
        if (adminDireccion.isBlank()) {
            throw new IllegalStateException("ADMIN_DIRECCION es obligatoria");
        }
        if (!adminTelefono.trim().matches("\\+?[0-9]{8,14}")) {
            throw new IllegalStateException("ADMIN_TELEFONO debe tener entre 8 y 14 dígitos, con + opcional");
        }
    }

    private int parseEntero(String nombreVariable, String valor, int minimo, int maximo) {
        try {
            int numero = Integer.parseInt(valor.trim());
            if (numero < minimo || numero > maximo) {
                throw new NumberFormatException();
            }
            return numero;
        } catch (NumberFormatException e) {
            throw new IllegalStateException(nombreVariable + " no es un número válido");
        }
    }

    private LocalDate parseFecha() {
        try {
            LocalDate fecha = LocalDate.parse(adminFechaNacimiento.trim());
            if (!fecha.isBefore(LocalDate.now())) {
                throw new IllegalStateException("ADMIN_FECHA_NACIMIENTO debe ser anterior a hoy");
            }
            return fecha;
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("ADMIN_FECHA_NACIMIENTO debe tener formato AAAA-MM-DD");
        }
    }
}
