package com.factory.tecnofactory.dto;

import java.util.List;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        Long precio,
        Long stock,
        Long stockCritico,
        Integer descuento,
        Long precioFinal,
        Boolean activo,
        List<CategoriaResumen> categorias
) {

    public record CategoriaResumen(Long id, String nombre) {
    }
}
