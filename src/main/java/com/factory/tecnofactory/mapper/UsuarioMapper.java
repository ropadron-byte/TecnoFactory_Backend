package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "tipoUsuario.nombre", target = "tipoUsuario")
    @Mapping(source = "comuna.nombre", target = "comuna")
    @Mapping(source = "comuna.region.nombre", target = "region")
    UsuarioResponse toResponse(Usuario usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contrasenia", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "tipoUsuario", ignore = true)
    @Mapping(target = "comuna", ignore = true)
    Usuario toEntity(UsuarioRequest request);
}