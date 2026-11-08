package com.factory.tecnofactory.dto;

import java.util.List;

public record CarritoResponse(
        Long id,
        List<DetalleCarritoResponse> items,
        Long total
) {
}
