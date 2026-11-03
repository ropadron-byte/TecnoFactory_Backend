package com.factory.tecnofactory.service;

import com.factory.tecnofactory.exception.ArchivoNoValidoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AlmacenamientoService {

    private static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private static final Set<String> SUBCARPETAS = Set.of("productos", "categorias", "blog", "general");

    private final Path raiz;

    public AlmacenamientoService(@Value("${app.uploads.directorio}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    /** Bytes con los que empieza cada formato (los "magic bytes"). */
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] FIRMA_RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIRMA_WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
    private static final int BYTES_A_LEER = 12;

    /** Guarda el archivo y devuelve la URL relativa, ej: /uploads/productos/uuid.jpg */
    public String guardar(MultipartFile archivo, String subcarpeta) {
        if (!SUBCARPETAS.contains(subcarpeta)) {
            throw new ReglaNegocioException("Carpeta de destino no válida");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoNoValidoException("El archivo está vacío");
        }
        // Map.of(...).get(null) lanza NullPointerException: un multipart sin Content-Type no debe llegar ahí
        String tipo = archivo.getContentType();
        String extension = tipo == null ? null : TIPOS.get(tipo);
        if (extension == null) {
            throw new ArchivoNoValidoException("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        try (InputStream in = archivo.getInputStream()) {
            // El Content-Type lo escribe el cliente: se comprueba que los primeros bytes lo respalden
            byte[] cabecera = in.readNBytes(BYTES_A_LEER);
            if (!coincideConTipo(cabecera, tipo)) {
                throw new ArchivoNoValidoException("El contenido del archivo no corresponde a una imagen " + extension.substring(1).toUpperCase());
            }
            Path carpeta = raiz.resolve(subcarpeta);
            Files.createDirectories(carpeta);
            String nombre = UUID.randomUUID() + extension;
            Files.copy(new SequenceInputStream(new ByteArrayInputStream(cabecera), in), carpeta.resolve(nombre));
            return "/uploads/" + subcarpeta + "/" + nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    static boolean coincideConTipo(byte[] cabecera, String tipo) {
        return switch (tipo) {
            case "image/jpeg" -> empiezaCon(cabecera, 0, FIRMA_JPEG);
            case "image/png" -> empiezaCon(cabecera, 0, FIRMA_PNG);
            case "image/webp" -> empiezaCon(cabecera, 0, FIRMA_RIFF) && empiezaCon(cabecera, 8, FIRMA_WEBP);
            default -> false;
        };
    }

    private static boolean empiezaCon(byte[] datos, int desde, byte[] prefijo) {
        if (datos.length < desde + prefijo.length) {
            return false;
        }
        for (int i = 0; i < prefijo.length; i++) {
            if (datos[desde + i] != prefijo[i]) {
                return false;
            }
        }
        return true;
    }

    public void eliminar(String url) {
        Path archivo = raiz.resolve(url.replaceFirst("^/uploads/", "")).normalize();
        if (!archivo.startsWith(raiz)) {
            return; // evita salirse de la carpeta de uploads
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo eliminar la imagen", e);
        }
    }
}