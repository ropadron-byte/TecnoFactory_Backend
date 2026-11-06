package com.factory.tecnofactory.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Blog visible para el público: no expone datos del autor. */
public record BlogPublicoResponse(
        Long id,
        String titulo,
        String contenido,
        LocalDateTime fechaCreacion,
        ImagenResumen imagenPrincipal,
        List<ImagenResumen> imagenes
) {
}
