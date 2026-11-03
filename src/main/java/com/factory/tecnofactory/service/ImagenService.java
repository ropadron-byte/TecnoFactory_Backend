package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.DuenoResponse;
import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.ImagenResumen;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.Blog;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.repository.BlogRepository;
import com.factory.tecnofactory.repository.ImagenUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImagenService {

    private static final String SUBCARPETA_BLOG = "blog";

    private final ImagenUrlRepository imagenUrlRepository;
    private final BlogRepository blogRepository;
    private final AlmacenamientoService almacenamiento;

    @Transactional(readOnly = true)
    public PageResponse<ImagenResponse> listar(Pageable pageable) {
        Page<ImagenUrl> pagina = imagenUrlRepository.findAll(pageable);
        return PageResponse.de(pagina.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ImagenResponse buscarPorId(Long id) {
        ImagenUrl imagen = imagenUrlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", id));
        return toResponse(imagen);
    }

    @Transactional(readOnly = true)
    public List<ImagenResumen> listarPorBlog(Long idBlog, Authentication autenticacion) {
        Blog blog = buscarBlogVisible(idBlog, autenticacion);
        return blog.getImagenes().stream()
                .sorted(Comparator.comparing(ImagenUrl::getId))
                .map(this::toResumen)
                .toList();
    }

    /** Devuelve null cuando el blog no tiene portada (el controller responde 204). */
    @Transactional(readOnly = true)
    public ImagenResumen obtenerPortada(Long idBlog, Authentication autenticacion) {
        Blog blog = buscarBlogVisible(idBlog, autenticacion);
        return blog.getImagenPrincipal() == null ? null : toResumen(blog.getImagenPrincipal());
    }

    /**
     * Sube un archivo y lo registra en imagen_url SIN dueño: después se asigna por id
     * (hoy a un blog con POST /api/blogs/{id}/imagenes/{idImagen}).
     */
    @Transactional
    public ImagenResponse subir(MultipartFile archivo, String subcarpeta) {
        String url = almacenamiento.guardar(archivo, subcarpeta);
        try {
            ImagenUrl imagen = new ImagenUrl();
            imagen.setUrl(url);
            imagen = imagenUrlRepository.saveAndFlush(imagen);
            log.info("Imagen {} subida sin dueño ({})", imagen.getId(), subcarpeta);
            return toResponse(imagen);
        } catch (RuntimeException e) {
            // La base no guardó la fila: el archivo quedaría huérfano en uploads/.
            almacenamiento.eliminar(url);
            throw e;
        }
    }

    @Transactional
    public ImagenResponse subirYAgregarABlog(Long idBlog, MultipartFile archivo, Authentication autenticacion) {
        Blog blog = buscarBlog(idBlog);
        BlogAutorizacion.verificarPropietarioOAdmin(blog, autenticacion);

        String url = almacenamiento.guardar(archivo, SUBCARPETA_BLOG);
        try {
            ImagenUrl imagen = new ImagenUrl();
            imagen.setUrl(url);
            imagen = imagenUrlRepository.saveAndFlush(imagen);

            blog.getImagenes().add(imagen);
            imagen.getBlogs().add(blog);
            blogRepository.saveAndFlush(blog);

            log.info("Imagen {} subida al blog {}", imagen.getId(), idBlog);
            return toResponse(imagen);
        } catch (RuntimeException e) {
            // La base no guardó la fila: el archivo quedaría huérfano en uploads/.
            almacenamiento.eliminar(url);
            throw e;
        }
    }

    @Transactional
    public ImagenResponse agregarExistenteABlog(Long idBlog, Long idImagen, Authentication autenticacion) {
        Blog blog = buscarBlog(idBlog);
        BlogAutorizacion.verificarPropietarioOAdmin(blog, autenticacion);

        ImagenUrl imagen = imagenUrlRepository.findById(idImagen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", idImagen));

        boolean yaEstaEnLaGaleria = blog.getImagenes().stream()
                .anyMatch(i -> i.getId().equals(idImagen));
        if (yaEstaEnLaGaleria) {
            throw new ReglaNegocioException("La imagen ya está en la galería de este blog");
        }

        blog.getImagenes().add(imagen);
        imagen.getBlogs().add(blog);
        blogRepository.saveAndFlush(blog);

        log.info("Imagen {} agregada a la galería del blog {}", idImagen, idBlog);
        return toResponse(imagen);
    }

    private Blog buscarBlog(Long idBlog) {
        return blogRepository.findById(idBlog)
                .orElseThrow(() -> new RecursoNoEncontradoException("Blog", idBlog));
    }

    private Blog buscarBlogVisible(Long idBlog, Authentication autenticacion) {
        Blog blog = buscarBlog(idBlog);
        if (!Boolean.TRUE.equals(blog.getVisible()) && !BlogAutorizacion.puedeVerBlogOculto(blog, autenticacion)) {
            throw new RecursoNoEncontradoException("Blog", idBlog);
        }
        return blog;
    }

    private ImagenResumen toResumen(ImagenUrl imagen) {
        return new ImagenResumen(imagen.getId(), imagen.getUrl());
    }

    private ImagenResponse toResponse(ImagenUrl imagen) {
        List<DuenoResponse> duenos = new ArrayList<>();
        imagen.getBlogs().forEach(b -> duenos.add(new DuenoResponse("BLOG", b.getId())));
        imagen.getProductos().forEach(p -> duenos.add(new DuenoResponse("PRODUCTO", p.getId())));
        imagen.getCategorias().forEach(c -> duenos.add(new DuenoResponse("CATEGORIA", c.getId())));
        duenos.sort(Comparator.comparing(DuenoResponse::tipo).thenComparing(DuenoResponse::id));

        return new ImagenResponse(imagen.getId(), imagen.getUrl(), duenos);
    }
}
