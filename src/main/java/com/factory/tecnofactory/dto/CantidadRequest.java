package com.factory.tecnofactory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cambiar la cantidad de un producto que ya está en el carrito. */
public record CantidadRequest(

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer cantidad
) {
}
