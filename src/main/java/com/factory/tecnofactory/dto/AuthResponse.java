package com.factory.tecnofactory.dto;

public record AuthResponse(
        String token,
        String tipo,
        String correo,
        String rol
) {
}
