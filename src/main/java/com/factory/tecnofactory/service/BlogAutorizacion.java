package com.factory.tecnofactory.service;

import com.factory.tecnofactory.entity.Blog;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/** Reglas de autoría compartidas por BlogService e ImagenService. */
final class BlogAutorizacion {

    private static final String ROL_ADMINISTRADOR = "ROLE_ADMINISTRADOR";

    private BlogAutorizacion() {
    }

    static boolean esAdministrador(Authentication autenticacion) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> ROL_ADMINISTRADOR.equals(a.getAuthority()));
    }

    /** Un blog oculto lo ven el administrador y su propio autor; para el resto no existe. */
    static boolean puedeVerBlogOculto(Blog blog, Authentication autenticacion) {
        if (esAdministrador(autenticacion)) {
            return true;
        }
        return autenticacion != null
                && blog.getUsuario() != null
                && blog.getUsuario().getCorreo() != null
                && blog.getUsuario().getCorreo().equals(autenticacion.getName());
    }

    /** Solo el autor del blog o un administrador pueden modificarlo. */
    static void verificarPropietarioOAdmin(Blog blog, Authentication autenticacion) {
        if (esAdministrador(autenticacion)) {
            return;
        }
        if (autenticacion == null || !blog.getUsuario().getCorreo().equals(autenticacion.getName())) {
            throw new AccessDeniedException("No tienes permiso para modificar este blog");
        }
    }
}
