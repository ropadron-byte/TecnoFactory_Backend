package com.factory.tecnofactory.mapper;

import com.factory.tecnofactory.dto.ContactoRequest;
import com.factory.tecnofactory.dto.ContactoResponse;
import com.factory.tecnofactory.entity.Contacto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContactoMapper {

    ContactoResponse toResponse(Contacto contacto);

    @Mapping(target = "id", ignore = true)
    Contacto toEntity(ContactoRequest request);
}
