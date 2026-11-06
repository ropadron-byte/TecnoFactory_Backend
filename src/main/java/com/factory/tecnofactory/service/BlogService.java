package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.BlogGestionResponse;
import com.factory.tecnofactory.dto.BlogPublicoResponse;
import com.factory.tecnofactory.dto.BlogRequest;
import com.factory.tecnofactory.dto.BlogResponse;
import com.factory.tecnofactory.dto.BlogResumenResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.Blog;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.mapper.BlogMapper;
import com.factory.tecnofactory.repository.BlogRepository;
import com.factory.tecnofactory.repository.ImagenUrlRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;
    private final ImagenUrlRepository imagenUrlRepository;
    private final UsuarioRepository usuarioRepository;
    private final BlogMapper blogMapper;

    @Transactional
    public BlogResponse crear(BlogRequest request, String correoAutenticado) {
        Usuario autor = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correoAutenticado));

        Blog blog = blogMapper.toEntity(request);
        blog.setUsuario(autor);
        blog.setVisible(request.visible() == null || request.visible());
        blog.setFechaCreacion(LocalDateTime.now());

        if (request.idImagenPrincipal() != null) {
            ImagenUrl imagen = imagenUrlRepository.findById(request.idImagenPrincipal())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", request.idImagenPrincipal()));
            blog.setImagenPrincipal(imagen);
            blog.getImagenes().add(imagen);
            imagen.getBlogs().add(blog);
        }

        Blog guardado = blogRepository.save(blog);
        log.info("Blog {} creado por {}", guardado.getId(), correoAutenticado);
        return blogMapper.toResponse(guardado);
    }

    /** Lectura pública: nunca expone datos del autor. */
    @Transactional(readOnly = true)
    public BlogPublicoResponse buscarPorId(Long id, Authentication autenticacion) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Blog", id));

        // Un blog invisible se comporta como inexistente, salvo para el personal (administrador o vendedor).
        if (!Boolean.TRUE.equals(blog.getVisible()) && !BlogAutorizacion.puedeVerBlogOculto(autenticacion)) {
            throw new RecursoNoEncontradoException("Blog", id);
        }
        return blogMapper.toPublico(blog);
    }

    /** Detalle de gestión: incluye los datos del autor. Solo lo invoca personal. */
    @Transactional(readOnly = true)
    public BlogResponse buscarGestionPorId(Long id, Authentication autenticacion) {
        BlogAutorizacion.verificarPersonal(autenticacion);
        return blogMapper.toResponse(buscarBlog(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogResumenResponse> listar(Pageable pageable) {
        Page<Blog> pagina = blogRepository.findByVisibleTrue(pageable);
        return PageResponse.de(pagina.map(blogMapper::toResumen));
    }

    /** Listado de gestión: todos los blogs, también los desactivados. */
    @Transactional(readOnly = true)
    public PageResponse<BlogGestionResponse> listarGestion(Pageable pageable) {
        return PageResponse.de(blogRepository.findAll(pageable).map(blogMapper::toGestion));
    }

    /**
     * Actualiza título y contenido. 'visible' e 'idImagenPrincipal' son opcionales:
     * si no vienen se conservan los valores actuales (para quitar una portada se usa
     * DELETE /api/blogs/{id}/imagenes/{idImagen}).
     */
    @Transactional
    public BlogResponse actualizar(Long id, BlogRequest request, Authentication autenticacion) {
        BlogAutorizacion.verificarPersonal(autenticacion);
        Blog blog = buscarBlog(id);

        blog.setTitulo(request.titulo());
        blog.setContenido(request.contenido());
        if (request.visible() != null) {
            blog.setVisible(request.visible());
        }
        if (request.idImagenPrincipal() != null) {
            asignarPortada(blog, buscarImagen(request.idImagenPrincipal()));
        }

        Blog guardado = blogRepository.save(blog);
        log.info("Blog {} actualizado", id);
        return blogMapper.toResponse(guardado);
    }

    @Transactional
    public BlogResponse desactivar(Long id, Authentication autenticacion) {
        return cambiarVisibilidad(id, false, autenticacion);
    }

    @Transactional
    public BlogResponse activar(Long id, Authentication autenticacion) {
        return cambiarVisibilidad(id, true, autenticacion);
    }

    /**
     * Borra el blog. Sus relaciones con imágenes (galería y portada) desaparecen con él;
     * las imágenes quedan en el catálogo, sin ese dueño.
     */
    @Transactional
    public void eliminar(Long id, Authentication autenticacion) {
        BlogAutorizacion.verificarPersonal(autenticacion);
        Blog blog = buscarBlog(id);

        blog.getImagenes().forEach(imagen -> imagen.getBlogs().remove(blog));
        blog.getImagenes().clear();
        blogRepository.delete(blog);
        log.info("Blog {} eliminado", id);
    }

    @Transactional
    public BlogResponse asignarImagenPrincipal(Long idBlog, Long idImagen, Authentication autenticacion) {
        BlogAutorizacion.verificarPersonal(autenticacion);
        Blog blog = buscarBlog(idBlog);

        asignarPortada(blog, buscarImagen(idImagen));

        Blog guardado = blogRepository.save(blog);
        log.info("Blog {} cambió su portada a la imagen {}", idBlog, idImagen);
        return blogMapper.toResponse(guardado);
    }

    private BlogResponse cambiarVisibilidad(Long id, boolean visible, Authentication autenticacion) {
        BlogAutorizacion.verificarPersonal(autenticacion);
        Blog blog = buscarBlog(id);

        blog.setVisible(visible);
        Blog guardado = blogRepository.save(blog);
        log.info("Blog {} {}", id, visible ? "activado" : "desactivado");
        return blogMapper.toResponse(guardado);
    }

    /** La portada siempre forma parte de la galería del blog. */
    private void asignarPortada(Blog blog, ImagenUrl imagen) {
        blog.setImagenPrincipal(imagen);
        blog.getImagenes().add(imagen);
        imagen.getBlogs().add(blog);
    }

    private Blog buscarBlog(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Blog", id));
    }

    private ImagenUrl buscarImagen(Long id) {
        return imagenUrlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", id));
    }
}
