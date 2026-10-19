package com.factory.tecnofactory.dto;

import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        Integer run,
        String runDv,
        String nombre,
        String apellidos,
        String correo,
        LocalDate fechaNacimiento,
        String direccion,
        String telefono,
        Boolean activo,
        String tipoUsuario,
        String comuna,
        String region
) {
}