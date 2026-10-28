package com.factory.tecnofactory.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlmacenamientoService {

    private static final Map<String, String> TIPOS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path raiz;

    public AlmacenamientoService(@Value("${app.uploads.directorio}") String dir) {
        this.raiz = Paths.get(dir).toAbsolutePath().normalize();
    }

    /** Guarda el archivo y devuelve la URL relativa, ej: /uploads/productos/uuid.jpg */
    public String guardar(MultipartFile archivo, String subcarpeta) throws IOException {
        String extension = TIPOS.get(archivo.getContentType());
        if (archivo.isEmpty() || extension == null) {
            throw new ReglaNegocioException("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        Path carpeta = raiz.resolve(subcarpeta).normalize();
        Files.createDirectories(carpeta);

        String nombre = UUID.randomUUID() + extension;
        Files.copy(archivo.getInputStream(), carpeta.resolve(nombre));
        return "/uploads/" + subcarpeta + "/" + nombre;
    }

    public void eliminar(String url) throws IOException {
        Path archivo = raiz.resolve(url.replaceFirst("^/uploads/", "")).normalize();
        if (archivo.startsWith(raiz)) {          // evita salirse de la carpeta
            Files.deleteIfExists(archivo);
        }
    }
}