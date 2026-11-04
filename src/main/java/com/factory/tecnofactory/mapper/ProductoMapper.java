package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.ProductoRequest;
import com.factory.tecnofactory.dto.ProductoResponse;
import com.factory.tecnofactory.entity.Producto;
import com.factory.tecnofactory.entity.ProductoCategoria;
import com.factory.tecnofactory.util.Precios;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = Precios.class)
public interface ProductoMapper {

    @Mapping(target = "precioFinal",
            expression = "java(Precios.conDescuento(producto.getPrecio(), producto.getDescuento()))")
    ProductoResponse toResponse(Producto producto);

    // MapStruct lo usa para convertir cada ProductoCategoria de la lista "categorias"
    @Mapping(source = "categoria.id", target = "id")
    @Mapping(source = "categoria.nombre", target = "nombre")
    ProductoResponse.CategoriaResumen toResumen(ProductoCategoria productoCategoria);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "categorias", ignore = true)
    @Mapping(target = "descuento", source = "descuento", defaultValue = "0")
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "categorias", ignore = true)
    @Mapping(target = "descuento", source = "descuento", defaultValue = "0")
    void actualizar(ProductoRequest request, @MappingTarget Producto producto);
}
