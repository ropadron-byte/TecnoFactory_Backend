package com.factory.tecnofactory.service;

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

    @Transactional(readOnly = true)
    public BlogResponse buscarPorId(Long id, Authentication autenticacion) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Blog", id));

        // Un blog invisible se comporta como inexistente, salvo para el administrador y su autor.
        if (!Boolean.TRUE.equals(blog.getVisible()) && !BlogAutorizacion.puedeVerBlogOculto(blog, autenticacion)) {
            throw new RecursoNoEncontradoException("Blog", id);
        }
        return blogMapper.toResponse(blog);
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogResumenResponse> listar(Pageable pageable) {
        Page<Blog> pagina = blogRepository.findByVisibleTrue(pageable);
        return PageResponse.de(pagina.map(blogMapper::toResumen));
    }

    @Transactional
    public BlogResponse asignarImagenPrincipal(Long idBlog, Long idImagen, Authentication autenticacion) {
        Blog blog = blogRepository.findById(idBlog)
                .orElseThrow(() -> new RecursoNoEncontradoException("Blog", idBlog));
        BlogAutorizacion.verificarPropietarioOAdmin(blog, autenticacion);

        ImagenUrl imagen = imagenUrlRepository.findById(idImagen)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", idImagen));

        blog.setImagenPrincipal(imagen);
        // La portada siempre forma parte de la galería del blog.
        blog.getImagenes().add(imagen);
        imagen.getBlogs().add(blog);

        Blog guardado = blogRepository.save(blog);
        log.info("Blog {} cambió su portada a la imagen {}", idBlog, idImagen);
        return blogMapper.toResponse(guardado);
    }
}
