package com.factory.tecnofactory.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void elMensajeDeTamanoUsaLaConfiguracion() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(DataSize.ofMegabytes(2));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/imagenes/productos");

        ErrorResponse respuesta = handler.archivoMuyGrande(
                new MaxUploadSizeExceededException(2 * 1024 * 1024), request).getBody();

        assertEquals("El archivo supera el tamaño máximo permitido (2 MB)", respuesta.mensaje());
    }

    @Test
    void elMensajeFuncionaConKilobytes() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(DataSize.ofKilobytes(512));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/imagenes/productos");

        ErrorResponse respuesta = handler.archivoMuyGrande(
                new MaxUploadSizeExceededException(512 * 1024), request).getBody();

        assertEquals("El archivo supera el tamaño máximo permitido (512 KB)", respuesta.mensaje());
    }
}
