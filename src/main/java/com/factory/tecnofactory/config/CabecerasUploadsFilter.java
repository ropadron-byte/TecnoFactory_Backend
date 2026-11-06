package com.factory.tecnofactory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Los archivos de /uploads/** se sirven sin sesión para que funcionen los <img>.
 * Estas cabeceras impiden que el navegador interprete una subida como HTML o script.
 */
@Component
public class CabecerasUploadsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ruta = request.getRequestURI();
        if (ruta != null && ruta.startsWith("/uploads/")) {
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Security-Policy", "default-src 'none'");
        }
        filterChain.doFilter(request, response);
    }
}
