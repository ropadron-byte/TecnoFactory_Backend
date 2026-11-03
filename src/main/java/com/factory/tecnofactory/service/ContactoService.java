package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.ContactoRequest;
import com.factory.tecnofactory.dto.ContactoResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.Contacto;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.mapper.ContactoMapper;
import com.factory.tecnofactory.repository.ContactoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactoService {

    private final ContactoRepository contactoRepository;
    private final ContactoMapper contactoMapper;

    @Transactional
    public ContactoResponse crear(ContactoRequest request) {
        Contacto contacto = contactoMapper.toEntity(request);
        contacto.setNombre(contacto.getNombre().trim());
        contacto.setCorreo(contacto.getCorreo().trim());
        contacto.setComentario(contacto.getComentario().trim());

        Contacto guardado = contactoRepository.save(contacto);
        log.info("Contacto {} recibido", guardado.getId());
        return contactoMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactoResponse> listar(Pageable pageable) {
        Page<Contacto> pagina = contactoRepository.findAll(pageable);
        return PageResponse.de(pagina.map(contactoMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ContactoResponse buscarPorId(Long id) {
        Contacto contacto = contactoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Contacto", id));
        return contactoMapper.toResponse(contacto);
    }
}
