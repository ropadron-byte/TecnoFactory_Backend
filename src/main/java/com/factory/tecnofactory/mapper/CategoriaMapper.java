package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.CategoriaRequest;
import com.factory.tecnofactory.dto.CategoriaResponse;
import com.factory.tecnofactory.entity.Categoria;
import com.factory.tecnofactory.service.ResolutorImagenes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    /** La cantidad de productos no está en la entidad: la calcula el service y llega aquí. */
    default CategoriaResponse toResponse(Categoria categoria, long cantidadProductos) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                cantidadProductos,
                ResolutorImagenes.urls(categoria.getImagenesCategoria()));
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imagenesCategoria", ignore = true)
    Categoria toEntity(CategoriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imagenesCategoria", ignore = true)
    void actualizar(CategoriaRequest request, @MappingTarget Categoria categoria);
}
