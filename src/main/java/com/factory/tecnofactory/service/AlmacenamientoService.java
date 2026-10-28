package com.factory.tecnofactory.service;

import com.factory.tecnofactory.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AlmacenamientoService {

    private static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private static final Set<String> SUBCARPETAS = Set.of("productos", "categorias", "blog");

    private final Path raiz;

    public AlmacenamientoService(@Value("${app.uploads.directorio}") String directorio) {
        this.raiz = Paths.get(directorio).toAbsolutePath().normalize();
    }

    /** Guarda el archivo y devuelve la URL relativa, ej: /uploads/productos/uuid.jpg */
    public String guardar(MultipartFile archivo, String subcarpeta) {
        if (!SUBCARPETAS.contains(subcarpeta)) {
            throw new ReglaNegocioException("Carpeta de destino no válida");
        }
        String extension = TIPOS.get(archivo.getContentType());
        if (archivo.isEmpty() || extension == null) {
            throw new ReglaNegocioException("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        try {
            Path carpeta = raiz.resolve(subcarpeta);
            Files.createDirectories(carpeta);
            String nombre = UUID.randomUUID() + extension;
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, carpeta.resolve(nombre));
            }
            return "/uploads/" + subcarpeta + "/" + nombre;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
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