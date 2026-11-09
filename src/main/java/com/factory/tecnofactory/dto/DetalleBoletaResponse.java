package com.factory.tecnofactory.dto;

public record DetalleBoletaResponse(
        Long id,
        Long idProducto,
        String nombre,
        Long precioUnitario,
        Integer descuento,
        Long precioFinalUnitario,
        Integer cantidad,
        Long subtotal
) {
}
