package com.factory.tecnofactory.util;

import org.springframework.security.core.Authentication;

/** Consultas sobre el rol de quien hace la petición. */
public final class Roles {

    private Roles() {
    }

    /** true si es ADMINISTRADOR o VENDEDOR. Acepta null (petición sin sesión). */
    public static boolean esPersonal(Authentication autenticacion) {
        if (autenticacion == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR")
                        || a.getAuthority().equals("ROLE_VENDEDOR"));
    }
}
