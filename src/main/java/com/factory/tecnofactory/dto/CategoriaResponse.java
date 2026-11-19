package com.factory.tecnofactory.dto;

import java.util.List;

/** cantidadProductos cuenta solo los productos activos de la categoría. */
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        long cantidadProductos,
        List<String> imagenes
) {
}
