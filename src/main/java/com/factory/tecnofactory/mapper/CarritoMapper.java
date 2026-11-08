package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.CarritoResponse;
import com.factory.tecnofactory.dto.DetalleCarritoResponse;
import com.factory.tecnofactory.entity.Carrito;
import com.factory.tecnofactory.entity.DetalleCarrito;
import com.factory.tecnofactory.util.Precios;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;

@Mapper(componentModel = "spring", imports = Precios.class)
public interface CarritoMapper {

    @Mapping(source = "producto.id", target = "idProducto")
    @Mapping(source = "producto.nombre", target = "nombre")
    @Mapping(source = "producto.precio", target = "precioUnitario")
    @Mapping(source = "producto.descuento", target = "descuento")
    @Mapping(target = "precioFinalUnitario",
            expression = "java(Precios.conDescuento(detalle.getProducto().getPrecio(), detalle.getProducto().getDescuento()))")
    @Mapping(target = "subtotal",
            expression = "java(Precios.conDescuento(detalle.getProducto().getPrecio(), detalle.getProducto().getDescuento()) * detalle.getCantidad())")
    DetalleCarritoResponse toItem(DetalleCarrito detalle);

    // Escrito a mano: ordena los productos por nombre y suma el total
    default CarritoResponse toResponse(Carrito carrito) {
        List<DetalleCarritoResponse> items = carrito.getDetalles().stream()
                .map(this::toItem)
                .sorted(Comparator.comparing(DetalleCarritoResponse::nombre))
                .toList();
        long total = items.stream().mapToLong(DetalleCarritoResponse::subtotal).sum();
        return new CarritoResponse(carrito.getId(), items, total);
    }
}
