package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Datos para crear o editar una categoría. */
public record CategoriaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,

        // Opcional. Si no viene, no se tocan las imágenes; si viene vacía, se quitan todas.
        @Size(max = 5, message = "Una categoría admite hasta 5 imágenes")
        List<@NotBlank(message = "La URL de la imagen no puede estar vacía")
                @Size(max = 1000, message = "La URL de la imagen no puede superar los 1000 caracteres") String> imagenes
) {
}
