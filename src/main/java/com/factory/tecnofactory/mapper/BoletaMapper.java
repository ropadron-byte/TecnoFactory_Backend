package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.BoletaResponse;
import com.factory.tecnofactory.dto.DetalleBoletaResponse;
import com.factory.tecnofactory.entity.Boleta;
import com.factory.tecnofactory.entity.DetalleBoleta;
import com.factory.tecnofactory.util.Precios;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = Precios.class)
public interface BoletaMapper {

    @Mapping(source = "usuario.id", target = "idUsuario")
    @Mapping(source = "totalBoleta", target = "total")
    BoletaResponse toResponse(Boleta boleta);

    @Mapping(source = "producto.id", target = "idProducto")
    @Mapping(target = "precioFinalUnitario",
            expression = "java(Precios.conDescuento(detalle.getPrecioUnitario(), detalle.getDescuento()))")
    @Mapping(target = "subtotal",
            expression = "java(Precios.conDescuento(detalle.getPrecioUnitario(), detalle.getDescuento()) * detalle.getCantidad())")
    DetalleBoletaResponse toDetalle(DetalleBoleta detalle);
}
