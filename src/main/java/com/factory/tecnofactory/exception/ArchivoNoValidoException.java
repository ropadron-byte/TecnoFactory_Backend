package com.factory.tecnofactory.exception;

/**
 * El archivo subido no sirve: está vacío, no es JPG/PNG/WEBP o su contenido no coincide
 * con el tipo que declara. Es un error de quien llama (400), no un conflicto de negocio (409).
 * Extiende ReglaNegocioException para no romper el código que ya la atrapa.
 */
public class ArchivoNoValidoException extends ReglaNegocioException {
    public ArchivoNoValidoException(String mensaje) {
        super(mensaje);
    }
}
