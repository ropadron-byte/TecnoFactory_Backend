package com.factory.tecnofactory.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * Reglas de acceso compartidas por BlogService e ImagenService.
 * El rol decide, no la autoría: administradores y vendedores mantienen todos los blogs e imágenes.
 */
final class BlogAutorizacion {

    private static final String ROL_ADMINISTRADOR = "ROLE_ADMINISTRADOR";
    private static final String ROL_VENDEDOR = "ROLE_VENDEDOR";

    private BlogAutorizacion() {
    }

    static boolean esAdministrador(Authentication autenticacion) {
        return tieneRol(autenticacion, ROL_ADMINISTRADOR);
    }

    /** Administrador o vendedor: el personal encargado de mantener blogs e imágenes. */
    static boolean esPersonal(Authentication autenticacion) {
        return tieneRol(autenticacion, ROL_ADMINISTRADOR) || tieneRol(autenticacion, ROL_VENDEDOR);
    }

    /** Un blog desactivado lo ve el personal (para poder reactivarlo); para el resto no existe. */
    static boolean puedeVerBlogOculto(Authentication autenticacion) {
        return esPersonal(autenticacion);
    }

    /** Defensa en profundidad: SecurityConfig ya exige estos roles en los endpoints de gestión. */
    static void verificarPersonal(Authentication autenticacion) {
        if (!esPersonal(autenticacion)) {
            throw new AccessDeniedException("Solo administradores y vendedores pueden gestionar blogs e imágenes");
        }
    }

    private static boolean tieneRol(Authentication autenticacion, String rol) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> rol.equals(a.getAuthority()));
    }
}
