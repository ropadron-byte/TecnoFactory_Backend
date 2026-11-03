package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un blog. El autor se obtiene del token, no del cuerpo.
 * idImagenPrincipal es opcional: si viene, la imagen se usa como portada
 * y también se agrega a la galería.
 */
public record BlogRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(max = 255, message = "El título no puede superar los 255 caracteres")
        String titulo,

        @NotBlank(message = "El contenido es obligatorio")
        @Size(max = 16000, message = "El contenido no puede superar los 16000 caracteres")
        String contenido,

        Boolean visible,

        Long idImagenPrincipal
) {
}
