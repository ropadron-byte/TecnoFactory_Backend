package com.factory.tecnofactory.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CabecerasUploadsFilterTest {

    private final CabecerasUploadsFilter filtro = new CabecerasUploadsFilter();

    @Test
    void agregaCabecerasDeBlindajeAUploads() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/blog/a.jpg");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("default-src 'none'", response.getHeader("Content-Security-Policy"));
    }

    @Test
    void noAgregaCabecerasAlRestoDeRutas() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/blogs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("X-Content-Type-Options"));
        assertNull(response.getHeader("Content-Security-Policy"));
    }
}
