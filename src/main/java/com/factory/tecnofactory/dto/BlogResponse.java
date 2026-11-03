package com.factory.tecnofactory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BlogResponse(
        Long id,
        String titulo,
        String contenido,
        Boolean visible,
        LocalDateTime fechaCreacion,
        Long autorId,
        String autorNombre,
        String autorCorreo,
        ImagenResumen imagenPrincipal,
        List<ImagenResumen> imagenes
) {
}
