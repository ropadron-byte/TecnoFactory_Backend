package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "El correo es obligatorio")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres")
        String correo,

        @NotBlank(message = "La clave es obligatoria")
        @Size(max = 72, message = "La clave no puede superar los 72 caracteres")
        String clave
) {

    // La clave nunca debe aparecer en un log
    @Override
    public String toString() {
        return "LoginRequest[correo=" + correo + "]";
    }
}
