package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.dto.TipoDueno;
import com.factory.tecnofactory.service.ImagenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
@Tag(name = "Imágenes", description = "Consulta de imágenes y subida de archivos")
public class ImagenController {

    private final ImagenService imagenService;

    @GetMapping
    @Operation(summary = "Listar todas las imágenes registradas (ADMINISTRADOR o VENDEDOR)")
    public PageResponse<ImagenResponse> listar(@ParameterObject Pageable pageable) {
        return imagenService.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una imagen por id con todos sus dueños (ADMINISTRADOR o VENDEDOR)")
    public ImagenResponse buscarPorId(@PathVariable Long id) {
        return imagenService.buscarPorId(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una imagen sin dueño (ADMINISTRADOR o VENDEDOR)",
            description = "Guarda el archivo, lo registra y devuelve su id. Después se asigna por id, "
                    + "por ejemplo a un blog con POST /api/blogs/{id}/imagenes/{idImagen}.")
    public ResponseEntity<ImagenResponse> subir(@RequestParam("archivo") MultipartFile archivo) {
        return creada(imagenService.subir(archivo, "general"));
    }

    @PostMapping(value = "/productos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen de producto, sin dueño todavía (ADMINISTRADOR o VENDEDOR)",
            description = "Igual que POST /api/imagenes, pero guarda el archivo en la carpeta productos.")
    public ResponseEntity<ImagenResponse> subirProducto(@RequestParam("archivo") MultipartFile archivo) {
        return creada(imagenService.subir(archivo, "productos"));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Reemplazar el archivo de una imagen (ADMINISTRADOR o VENDEDOR)",
            description = "Conserva el id y todas las relaciones; solo cambia la URL.")
    public ImagenResponse reemplazar(@PathVariable Long id,
                                     @RequestParam("archivo") MultipartFile archivo,
                                     Authentication autenticacion) {
        return imagenService.reemplazar(id, archivo, autenticacion);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar una imagen sin relaciones (ADMINISTRADOR o VENDEDOR)",
            description = "Responde 409 si la imagen todavía está asociada a algún blog, producto o categoría: "
                    + "primero hay que desacoplarla.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        imagenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/duenos")
    @Operation(summary = "Desacoplar la imagen de TODOS sus dueños (ADMINISTRADOR o VENDEDOR)",
            description = "La imagen queda libre y sigue existiendo.")
    public ImagenResponse desacoplarDeTodos(@PathVariable Long id, Authentication autenticacion) {
        return imagenService.desacoplarDeTodos(id, autenticacion);
    }

    @DeleteMapping("/{id}/duenos/{tipo}/{idDueno}")
    @Operation(summary = "Desacoplar la imagen de UN dueño (ADMINISTRADOR o VENDEDOR)",
            description = "tipo = BLOG, PRODUCTO o CATEGORIA (los mismos valores que devuelve 'duenos').")
    public ImagenResponse desacoplarDe(@PathVariable Long id,
                                       @PathVariable TipoDueno tipo,
                                       @PathVariable Long idDueno,
                                       Authentication autenticacion) {
        return imagenService.desacoplarDe(id, tipo, idDueno, autenticacion);
    }

    private static ResponseEntity<ImagenResponse> creada(ImagenResponse imagen) {
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/imagenes/{id}")
                .buildAndExpand(imagen.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(imagen);
    }
}
