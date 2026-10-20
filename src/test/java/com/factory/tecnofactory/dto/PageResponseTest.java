package com.factory.tecnofactory.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageResponseTest {

    @Test
    void convierteUnaPageDeSpring() {
        PageResponse<String> r = PageResponse.de(new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5));

        assertEquals(List.of("a", "b"), r.contenido());
        assertEquals(1, r.pagina());
        assertEquals(2, r.tamanio());
        assertEquals(5, r.totalElementos());
        assertEquals(3, r.totalPaginas());
    }
}