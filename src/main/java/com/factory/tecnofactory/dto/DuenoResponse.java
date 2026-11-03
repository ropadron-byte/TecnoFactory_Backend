package com.factory.tecnofactory.dto;

/** Dueño de una imagen: BLOG, PRODUCTO o CATEGORIA. */
public record DuenoResponse(
        String tipo,
        Long id
) {
}
