package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.LoginRequest;
import com.factory.tecnofactory.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    private static final LoginRequest LOGIN = new LoginRequest("ana@correo.cl", "Clave12345");

    @Test
    void loginCorrectoDevuelveTokenYElRolSinPrefijo() {
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(
                        "ana@correo.cl", null, List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR"))));
        when(jwtService.generarToken("ana@correo.cl", "VENDEDOR")).thenReturn("jwt-de-prueba");

        AuthResponse r = service.login(LOGIN);

        assertEquals("jwt-de-prueba", r.token());
        assertEquals("Bearer", r.tipo());
        assertEquals("ana@correo.cl", r.correo());
        assertEquals("VENDEDOR", r.rol());
    }

    @Test
    void credencialesIncorrectasLanzanBadCredentialsYNoGeneranToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("mal"));

        assertThrows(BadCredentialsException.class, () -> service.login(LOGIN));
        verify(jwtService, never()).generarToken(any(), any());
    }

    @Test
    void usuarioDesactivadoRecibeElMismoErrorQueUnaClaveIncorrecta() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("desactivado"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () -> service.login(LOGIN));
        assertEquals("Credenciales inválidas", ex.getMessage());
        verify(jwtService, never()).generarToken(any(), any());
    }
}
