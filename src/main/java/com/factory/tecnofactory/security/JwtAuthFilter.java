package com.factory.tecnofactory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lee "Authorization: Bearer <token>" en cada petición. Si el token es válido y el usuario
 * existe y está activo, lo deja autenticado. Si no, NO rechaza nada: deja la petición sin
 * autenticar y las reglas de SecurityConfig deciden si esa ruta la exige (401).
 *
 * No es @Component a propósito: SecurityConfig lo crea y lo agrega a la cadena (paso 5).
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain cadena) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.startsWith(PREFIJO)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = cabecera.substring(PREFIJO.length()).trim();

            if (jwtService.esValido(token)) {
                try {
                    // Se consulta la BD en cada petición: así un usuario desactivado o borrado
                    // pierde el acceso aunque su token siga sin expirar
                    UserDetails usuario = userDetailsService.loadUserByUsername(jwtService.obtenerCorreo(token));
                    if (usuario.isEnabled()) {
                        UsernamePasswordAuthenticationToken autenticacion =
                                UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
                        autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    }
                } catch (UsernameNotFoundException e) {
                    // El usuario ya no existe: se trata como petición sin autenticar
                }
            }
        }

        cadena.doFilter(request, response);
    }
}