package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.BlogRequest;
import com.factory.tecnofactory.dto.BlogResponse;
import com.factory.tecnofactory.dto.BlogResumenResponse;
import com.factory.tecnofactory.dto.ImagenResumen;
import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.service.BlogService;
import com.factory.tecnofactory.service.ImagenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs", description = "Publicaciones con portada y galería de imágenes")
public class BlogController {

    private final BlogService blogService;
    private final ImagenService imagenService;

    @PostMapping
    @Operation(summary = "Crear un blog (ADMINISTRADOR o VENDEDOR)")
    public ResponseEntity<BlogResponse> crear(@Valid @RequestBody BlogRequest request,
                                              Authentication autenticacion) {
        BlogResponse creado = blogService.crear(request, autenticacion.getName());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    @Operation(summary = "Listar blogs visibles (público)")
    public PageResponse<BlogResumenResponse> listar(@ParameterObject Pageable pageable) {
        return blogService.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver un blog visible (público)")
    public BlogResponse buscar(@PathVariable Long id, Authentication autenticacion) {
        return blogService.buscarPorId(id, autenticacion);
    }

    @GetMapping("/{id}/imagen")
    @Operation(summary = "Obtener la portada de un blog (204 si no tiene)")
    public ResponseEntity<ImagenResumen> portada(@PathVariable Long id, Authentication autenticacion) {
        ImagenResumen imagen = imagenService.obtenerPortada(id, autenticacion);
        return imagen == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(imagen);
    }

    @GetMapping("/{id}/imagenes")
    @Operation(summary = "Listar la galería de un blog (público)")
    public List<ImagenResumen> imagenes(@PathVariable Long id, Authentication autenticacion) {
        return imagenService.listarPorBlog(id, autenticacion);
    }

    @PostMapping(value = "/{id}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una imagen nueva y agregarla a la galería")
    public ResponseEntity<ImagenResponse> subirImagen(@PathVariable Long id,
                                                      @RequestParam("archivo") MultipartFile archivo,
                                                      Authentication autenticacion) {
        ImagenResponse imagen = imagenService.subirYAgregarABlog(id, archivo, autenticacion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idImagen}")
                .buildAndExpand(imagen.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(imagen);
    }

    @PostMapping("/{id}/imagenes/{idImagen}")
    @Operation(summary = "Agregar una imagen existente a la galería")
    public ResponseEntity<ImagenResponse> agregarImagenExistente(@PathVariable Long id,
                                                                 @PathVariable Long idImagen,
                                                                 Authentication autenticacion) {
        ImagenResponse imagen = imagenService.agregarExistenteABlog(id, idImagen, autenticacion);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .buildAndExpand()
                .toUri();
        return ResponseEntity.created(ubicacion).body(imagen);
    }

    @PutMapping("/{id}/imagen-principal/{idImagen}")
    @Operation(summary = "Cambiar la portada del blog")
    public BlogResponse cambiarPortada(@PathVariable Long id,
                                       @PathVariable Long idImagen,
                                       Authentication autenticacion) {
        return blogService.asignarImagenPrincipal(id, idImagen, autenticacion);
    }
}
