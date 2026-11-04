package com.factory.tecnofactory.dto;

import java.time.LocalDateTime;

/** Fila del listado de gestión: incluye 'visible' para ver también los blogs desactivados. */
public record BlogGestionResponse(
        Long id,
        String titulo,
        Boolean visible,
        LocalDateTime fechaCreacion,
        String autorNombre,
        ImagenResumen imagenPrincipal
) {
}
