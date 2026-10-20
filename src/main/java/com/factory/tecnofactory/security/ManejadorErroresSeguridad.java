package com.factory.tecnofactory.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Los errores de seguridad ocurren en los filtros, antes de llegar a un controller, y por eso
 * GlobalExceptionHandler no los ve solo. Esta clase los reenvía a ese mismo handler para que
 * el 401 y el 403 tengan el mismo JSON (ErrorResponse) que el resto de errores.
 *
 * No es @Component: SecurityConfig la crea.
 */
@RequiredArgsConstructor
public class ManejadorErroresSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    /** Ruta protegida sin token, o con token inválido o expirado: 401. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) {
        resolver.resolveException(request, response, null, ex);
    }

    /** Hay token válido, pero el rol no alcanza: 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
