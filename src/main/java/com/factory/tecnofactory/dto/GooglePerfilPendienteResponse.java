package com.factory.tecnofactory.dto;

/** Respuesta 202 cuando la cuenta de Google es nueva y falta completar el perfil. */
public record GooglePerfilPendienteResponse(
        boolean requiereCompletarPerfil,
        String registroToken,
        String correo,
        String nombre,
        String apellidos
) {
}
