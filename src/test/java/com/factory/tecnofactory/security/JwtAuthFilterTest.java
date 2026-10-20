package com.factory.tecnofactory.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthFilter filtro;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain cadena = new MockFilterChain();

    @AfterEach
    void limpiarContexto() {
        // El contexto de seguridad es global por hilo: si no se limpia, un test contamina al siguiente
        SecurityContextHolder.clearContext();
    }

    private static UserDetails usuario(boolean activo) {
        return User.builder().username("ana@correo.cl").password("hash").roles("CLIENTE").disabled(!activo).build();
    }

    @Test
    void tokenValidoDejaAutenticadoAlUsuario() throws Exception {
        request.addHeader("Authorization", "Bearer token-bueno");
        when(jwtService.esValido("token-bueno")).thenReturn(true);
        when(jwtService.obtenerCorreo("token-bueno")).thenReturn("ana@correo.cl");
        when(userDetailsService.loadUserByUsername("ana@correo.cl")).thenReturn(usuario(true));

        filtro.doFilter(request, response, cadena);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("ana@correo.cl", auth.getName());
        assertEquals("ROLE_CLIENTE", auth.getAuthorities().iterator().next().getAuthority());
        assertNotNull(cadena.getRequest(), "la petición debe seguir su camino");
    }

    @Test
    void sinCabeceraNoAutenticaYContinua() throws Exception {
        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
        verify(jwtService, never()).esValido(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cabeceraQueNoEsBearerSeIgnora() throws Exception {
        request.addHeader("Authorization", "Basic YW5hOmNsYXZl");

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).esValido(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tokenInvalidoNoAutenticaNiConsultaLaBaseDeDatos() throws Exception {
        request.addHeader("Authorization", "Bearer token-malo");
        when(jwtService.esValido("token-malo")).thenReturn(false);

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
        verify(userDetailsService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioDesactivadoNoQuedaAutenticado() throws Exception {
        request.addHeader("Authorization", "Bearer token-bueno");
        when(jwtService.esValido("token-bueno")).thenReturn(true);
        when(jwtService.obtenerCorreo("token-bueno")).thenReturn("ana@correo.cl");
        when(userDetailsService.loadUserByUsername("ana@correo.cl")).thenReturn(usuario(false));

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void usuarioInexistenteNoLanzaYNoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer token-bueno");
        when(jwtService.esValido("token-bueno")).thenReturn(true);
        when(jwtService.obtenerCorreo("token-bueno")).thenReturn("borrado@correo.cl");
        when(userDetailsService.loadUserByUsername("borrado@correo.cl"))
                .thenThrow(new UsernameNotFoundException("Usuario no encontrado"));

        filtro.doFilter(request, response, cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }
}
