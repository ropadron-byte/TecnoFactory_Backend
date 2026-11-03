package com.factory.tecnofactory.dto;

import java.time.LocalDateTime;

/** Versión liviana para el listado de blogs (sin contenido ni galería). */
public record BlogResumenResponse(
        Long id,
        String titulo,
        LocalDateTime fechaCreacion,
        String autorNombre,
        ImagenResumen imagenPrincipal
) {
}
