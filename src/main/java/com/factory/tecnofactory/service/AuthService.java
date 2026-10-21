package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.LoginRequest;
import com.factory.tecnofactory.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String PREFIJO_ROL = "ROLE_";
    private static final String TIPO_TOKEN = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {

        Authentication autenticacion;

        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.correo(), request.clave()));
        } catch (AuthenticationException e) {
            // Correo inexistente, clave incorrecta o usuario desactivado: mismo error para los tres,
            // así nadie puede averiguar qué correos están registrados
            log.warn("Intento de login fallido");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String rol = autenticacion.getAuthorities().iterator().next().getAuthority().substring(PREFIJO_ROL.length());
        String token = jwtService.generarToken(autenticacion.getName(), rol);
        return new AuthResponse(token, TIPO_TOKEN, autenticacion.getName(), rol);

    }
}
