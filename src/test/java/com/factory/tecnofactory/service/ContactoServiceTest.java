package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.ContactoRequest;
import com.factory.tecnofactory.dto.ContactoResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.Contacto;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.mapper.ContactoMapperImpl;
import com.factory.tecnofactory.repository.ContactoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactoServiceTest {

    @Mock
    private ContactoRepository contactoRepository;

    private ContactoService service;

    @BeforeEach
    void crearService() {
        service = new ContactoService(contactoRepository, new ContactoMapperImpl());
    }

    private static Contacto contacto() {
        Contacto contacto = new Contacto();
        contacto.setId(1L);
        contacto.setNombre("Ana");
        contacto.setCorreo("ana@correo.cl");
        contacto.setComentario("Hola");
        return contacto;
    }

    @Test
    void crearGuardaYDevuelveElContacto() {
        when(contactoRepository.save(any(Contacto.class))).thenAnswer(inv -> {
            Contacto c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        ContactoResponse respuesta = service.crear(
                new ContactoRequest("  Ana ", " ana@correo.cl ", " Hola "));

        ArgumentCaptor<Contacto> captor = ArgumentCaptor.forClass(Contacto.class);
        verify(contactoRepository).save(captor.capture());
        assertEquals("Ana", captor.getValue().getNombre());
        assertEquals("ana@correo.cl", captor.getValue().getCorreo());
        assertEquals("Hola", captor.getValue().getComentario());
        assertEquals(1L, respuesta.id());
        assertEquals("Ana", respuesta.nombre());
    }

    @Test
    void listarDevuelveLaPagina() {
        when(contactoRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(contacto())));

        PageResponse<ContactoResponse> respuesta = service.listar(PageRequest.of(0, 20));

        assertEquals(1, respuesta.contenido().size());
        assertEquals("ana@correo.cl", respuesta.contenido().get(0).correo());
    }

    @Test
    void buscarPorIdDevuelveElContacto() {
        when(contactoRepository.findById(1L)).thenReturn(Optional.of(contacto()));

        assertEquals("Hola", service.buscarPorId(1L).comentario());
    }

    @Test
    void buscarPorIdInexistenteLanzaExcepcion() {
        when(contactoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }
}
