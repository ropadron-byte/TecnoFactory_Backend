package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.NotBlank;

/** ID token que Google Identity Services entrega al frontend. */
public record GoogleLoginRequest(

        @NotBlank(message = "El token de Google es obligatorio")
        String credential
) {

    @Override
    public String toString() {
        return "GoogleLoginRequest[credential=<oculto>]";
    }
}
