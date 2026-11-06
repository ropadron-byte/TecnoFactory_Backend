package com.factory.tecnofactory.dto;

import java.time.LocalDateTime;

/** Versión liviana para el listado público: sin contenido, galería ni datos del autor. */
public record BlogResumenResponse(
        Long id,
        String titulo,
        LocalDateTime fechaCreacion,
        ImagenResumen imagenPrincipal
) {
}
