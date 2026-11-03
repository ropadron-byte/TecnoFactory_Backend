package com.factory.tecnofactory.service;

import com.factory.tecnofactory.exception.ArchivoNoValidoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenamientoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};

    @TempDir
    Path carpeta;

    private AlmacenamientoService service;

    @BeforeEach
    void crearService() {
        service = new AlmacenamientoService(carpeta.toString());
    }

    private static byte[] webp() {
        return "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);
    }

    @Test
    void guardaUnPngValidoYDevuelveLaUrl() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", PNG);

        String url = service.guardar(archivo, "blog");

        assertTrue(url.startsWith("/uploads/blog/") && url.endsWith(".png"));
        Path guardado = carpeta.resolve(url.replaceFirst("^/uploads/", ""));
        assertTrue(Files.exists(guardado));
        assertEquals(PNG.length, Files.size(guardado), "el archivo se copia completo, sin perder los bytes leídos");
    }

    @Test
    void guardaJpegYWebp() {
        assertTrue(service.guardar(new MockMultipartFile("archivo", "a.jpg", "image/jpeg", JPEG), "productos").endsWith(".jpg"));
        assertTrue(service.guardar(new MockMultipartFile("archivo", "a.webp", "image/webp", webp()), "productos").endsWith(".webp"));
    }

    @Test
    void rechazaTipoNoPermitido() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.gif", "image/gif", new byte[]{'G', 'I', 'F'});

        assertThrows(ArchivoNoValidoException.class, () -> service.guardar(archivo, "blog"));
    }

    @Test
    void rechazaArchivoSinContentTypeEnVezDeLanzarNullPointer() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", null, PNG);

        assertThrows(ArchivoNoValidoException.class, () -> service.guardar(archivo, "blog"));
    }

    @Test
    void rechazaArchivoVacio() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", new byte[0]);

        assertThrows(ArchivoNoValidoException.class, () -> service.guardar(archivo, "blog"));
    }

    @Test
    void rechazaContenidoQueNoCoincideConElContentType() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        assertThrows(ArchivoNoValidoException.class, () -> service.guardar(archivo, "blog"));
    }

    @Test
    void rechazaJpegDeclaradoComoPng() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", JPEG);

        assertThrows(ArchivoNoValidoException.class, () -> service.guardar(archivo, "blog"));
    }

    @Test
    void noGuardaNadaCuandoElContenidoEsInvalido() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThrows(ArchivoNoValidoException.class, () -> service.guardar(archivo, "blog"));

        try (var contenido = Files.list(carpeta)) {
            assertEquals(0, contenido.count());
        }
    }

    @Test
    void rechazaSubcarpetaNoPermitida() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "a.png", "image/png", PNG);

        assertThrows(ReglaNegocioException.class, () -> service.guardar(archivo, "../fuera"));
    }

    @Test
    void eliminaElArchivoGuardado() {
        String url = service.guardar(new MockMultipartFile("archivo", "a.png", "image/png", PNG), "blog");

        service.eliminar(url);

        assertTrue(Files.notExists(carpeta.resolve(url.replaceFirst("^/uploads/", ""))));
    }
}
