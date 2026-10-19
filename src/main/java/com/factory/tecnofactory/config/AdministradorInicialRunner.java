package com.factory.tecnofactory.config;

import com.factory.tecnofactory.service.DatosAdministrador;
import com.factory.tecnofactory.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdministradorInicialRunner implements ApplicationRunner {

    private final UsuarioService usuarioService;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("ADMIN_EMAIL o ADMIN_PASSWORD sin definir: no se crea el administrador inicial");
            return;
        }

        DatosAdministrador datos = new DatosAdministrador(
                11111111, "1",
                "Administrador", "TecnoFactory",
                adminEmail, adminPassword,
                LocalDate.of(2000, 1, 1),
                "Por definir", "+56900000000",
                13101
        );

        usuarioService.crearAdministradorSiNoExiste(datos);
    }
}