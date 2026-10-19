package com.factory.tecnofactory.service;

import java.time.LocalDate;

public record DatosAdministrador(
        Integer run,
        String runDv,
        String nombre,
        String apellidos,
        String correo,
        String clave,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        Integer idComuna
) {
}