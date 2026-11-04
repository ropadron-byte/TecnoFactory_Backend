package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

/** Datos para crear o editar un producto. No lleva id ni activo: los decide el sistema. */
public record ProductoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 0, message = "El precio no puede ser negativo")
        Long precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Long stock,

        @NotNull(message = "El stock crítico es obligatorio")
        @Min(value = 0, message = "El stock crítico no puede ser negativo")
        Long stockCritico,

        // Opcional: si no viene, queda en 0
        @Min(value = 0, message = "El descuento no puede ser menor a 0")
        @Max(value = 100, message = "El descuento no puede ser mayor a 100")
        Integer descuento,

        @NotEmpty(message = "Debe indicar al menos una categoría")
        Set<@NotNull(message = "El id de categoría no puede ser nulo") Long> categoriaIds
) {
}
