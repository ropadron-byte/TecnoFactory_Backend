package com.factory.tecnofactory.exception;

/** El ID token de Google no se pudo verificar (firma, emisor, audiencia, expiración o correo). */
public class TokenGoogleInvalidoException extends RuntimeException {

    public TokenGoogleInvalidoException() {
        super("El token de Google no es válido o expiró");
    }
}
