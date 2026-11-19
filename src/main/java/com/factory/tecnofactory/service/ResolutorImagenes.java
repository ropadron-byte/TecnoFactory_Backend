package com.factory.tecnofactory.service;

import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.repository.ImagenUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Convierte una lista de URLs en filas de imagen_url: reutiliza la que ya existe y crea la que
 * falta. Lo usan Producto y Categoria para asociar imágenes por URL.
 */
@Component
@RequiredArgsConstructor
public class ResolutorImagenes {

    private final ImagenUrlRepository imagenUrlRepository;

    /** Ignora URLs vacías y repetidas. Debe llamarse dentro de una transacción. */
    public Set<ImagenUrl> resolver(List<String> urls) {
        Set<ImagenUrl> resultado = new LinkedHashSet<>();
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            String limpia = url.trim();
            resultado.add(imagenUrlRepository.findByUrl(limpia).orElseGet(() -> crear(limpia)));
        }
        return resultado;
    }

    /** Las URLs en orden estable (por id), para que la primera sea siempre la portada. */
    public static List<String> urls(Set<ImagenUrl> imagenes) {
        return imagenes.stream()
                .sorted(Comparator.comparing(ImagenUrl::getId))
                .map(ImagenUrl::getUrl)
                .toList();
    }

    private ImagenUrl crear(String url) {
        ImagenUrl imagen = new ImagenUrl();
        imagen.setUrl(url);
        return imagenUrlRepository.save(imagen);
    }
}
