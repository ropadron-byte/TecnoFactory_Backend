package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Agregar un producto al carrito. */
public record CarritoItemRequest(

        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer cantidad
) {
}
