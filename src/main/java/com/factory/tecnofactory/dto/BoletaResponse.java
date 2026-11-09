package com.factory.tecnofactory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BoletaResponse(
        Long id,
        Long idUsuario,
        LocalDateTime fecha,
        Long total,
        Boolean estado,
        List<DetalleBoletaResponse> detalles
) {
}
