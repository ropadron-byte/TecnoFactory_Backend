package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.BlogGestionResponse;
import com.factory.tecnofactory.dto.BlogPublicoResponse;
import com.factory.tecnofactory.dto.BlogRequest;
import com.factory.tecnofactory.dto.BlogResponse;
import com.factory.tecnofactory.dto.BlogResumenResponse;
import com.factory.tecnofactory.dto.ImagenResumen;
import com.factory.tecnofactory.entity.Blog;
import com.factory.tecnofactory.entity.ImagenUrl;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(source = "usuario.id", target = "autorId")
    @Mapping(source = "usuario.nombre", target = "autorNombre")
    @Mapping(source = "usuario.correo", target = "autorCorreo")
    BlogResponse toResponse(Blog blog);

    BlogPublicoResponse toPublico(Blog blog);

    BlogResumenResponse toResumen(Blog blog);

    @Mapping(source = "usuario.nombre", target = "autorNombre")
    BlogGestionResponse toGestion(Blog blog);

    ImagenResumen toImagenResumen(ImagenUrl imagen);

    default List<ImagenResumen> toImagenes(Set<ImagenUrl> imagenes) {
        if (imagenes == null) {
            return null;
        }
        return imagenes.stream()
                .sorted(Comparator.comparing(ImagenUrl::getId))
                .map(this::toImagenResumen)
                .toList();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "imagenPrincipal", ignore = true)
    @Mapping(target = "imagenes", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    Blog toEntity(BlogRequest request);
}
