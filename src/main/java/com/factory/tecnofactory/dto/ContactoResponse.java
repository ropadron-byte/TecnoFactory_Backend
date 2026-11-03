package com.factory.tecnofactory.dto;

public record ContactoResponse(
        Long id,
        String nombre,
        String correo,
        String comentario
) {
}
