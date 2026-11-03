package com.factory.tecnofactory.dto;

import java.util.List;

/** Ficha completa de una imagen y todos los dueños que la usan. */
public record ImagenResponse(
        Long id,
        String url,
        List<DuenoResponse> duenos
) {
}
