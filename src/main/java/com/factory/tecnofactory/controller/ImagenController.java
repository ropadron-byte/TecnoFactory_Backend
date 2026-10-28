package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.service.AlmacenamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
@Tag(name = "Imágenes", description = "Subida de imágenes (solo administrador)")
public class ImagenController {

    private final AlmacenamientoService almacenamiento;

    @PostMapping(value = "/productos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen de producto")
    public ResponseEntity<Map<String, String>> subirProducto(@RequestParam("archivo") MultipartFile archivo) {
        String url = almacenamiento.guardar(archivo, "productos");
        return ResponseEntity.ok(Map.of("url", url));
    }
}