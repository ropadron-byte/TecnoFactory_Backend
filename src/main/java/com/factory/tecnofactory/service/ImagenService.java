package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.DuenoResponse;
import com.factory.tecnofactory.dto.ImagenResponse;
import com.factory.tecnofactory.dto.ImagenResumen;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.dto.TipoDueno;
import com.factory.tecnofactory.entity.Blog;
import com.factory.tecnofactory.entity.Categoria;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.entity.Producto;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        BlogAutorizacion.verificarPersonal(autenticacion);

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
        BlogAutorizacion.verificarPersonal(autenticacion);

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

    /**
     * Reemplaza el archivo de una imagen conservando su id y todas sus relaciones.
     * El archivo anterior se borra del disco solo cuando la transacción termina bien.
     */
    @Transactional
    public ImagenResponse reemplazar(Long id, MultipartFile archivo, Authentication autenticacion) {
        ImagenUrl imagen = buscarImagen(id);
        BlogAutorizacion.verificarPersonal(autenticacion);

        String urlAnterior = imagen.getUrl();
        String urlNueva = almacenamiento.guardar(archivo, subcarpetaDe(urlAnterior));
        try {
            imagen.setUrl(urlNueva);
            imagenUrlRepository.saveAndFlush(imagen);
        } catch (RuntimeException e) {
            almacenamiento.eliminar(urlNueva);
            throw e;
        }
        eliminarArchivoTrasCommit(urlAnterior);

        log.info("Imagen {} reemplazada", id);
        return toResponse(imagen);
    }

    /** Borra una imagen SIN relaciones (ni blogs, ni portadas, ni productos, ni categorías). */
    @Transactional
    public void eliminar(Long id) {
        ImagenUrl imagen = buscarImagen(id);

        int relaciones = blogsDe(imagen).size() + imagen.getProductos().size() + imagen.getCategorias().size();
        if (relaciones > 0) {
            throw new ReglaNegocioException("La imagen está en uso en " + relaciones
                    + " lugar(es); desacóplala antes de borrarla");
        }

        String url = imagen.getUrl();
        imagenUrlRepository.delete(imagen);
        eliminarArchivoTrasCommit(url);
        log.info("Imagen {} eliminada", id);
    }

    /** Desacopla la imagen de TODOS sus dueños. La imagen sigue existiendo, libre. */
    @Transactional
    public ImagenResponse desacoplarDeTodos(Long id, Authentication autenticacion) {
        ImagenUrl imagen = buscarImagen(id);
        BlogAutorizacion.verificarPersonal(autenticacion);

        for (Blog blog : blogsDe(imagen)) {
            quitarDeBlog(blog, imagen);
        }
        for (Producto producto : new ArrayList<>(imagen.getProductos())) {
            producto.getImagenesProducto().remove(imagen);
        }
        imagen.getProductos().clear();
        for (Categoria categoria : new ArrayList<>(imagen.getCategorias())) {
            categoria.getImagenesCategoria().remove(imagen);
        }
        imagen.getCategorias().clear();

        log.info("Imagen {} desacoplada de todos sus dueños", id);
        return toResponse(imagen);
    }

    /** Desacopla la imagen de UN dueño (blog, producto o categoría). */
    @Transactional
    public ImagenResponse desacoplarDe(Long idImagen, TipoDueno tipo, Long idDueno, Authentication autenticacion) {
        return switch (tipo) {
            case BLOG -> desacoplarDeBlog(idDueno, idImagen, autenticacion);
            case PRODUCTO -> {
                BlogAutorizacion.verificarPersonal(autenticacion);
                ImagenUrl imagen = buscarImagen(idImagen);
                Producto producto = imagen.getProductos().stream()
                        .filter(p -> p.getId().equals(idDueno))
                        .findFirst()
                        .orElseThrow(() -> new RecursoNoEncontradoException("Relación imagen-producto", idDueno));
                producto.getImagenesProducto().remove(imagen);
                imagen.getProductos().remove(producto);
                log.info("Imagen {} desacoplada del producto {}", idImagen, idDueno);
                yield toResponse(imagen);
            }
            case CATEGORIA -> {
                BlogAutorizacion.verificarPersonal(autenticacion);
                ImagenUrl imagen = buscarImagen(idImagen);
                Categoria categoria = imagen.getCategorias().stream()
                        .filter(c -> c.getId().equals(idDueno))
                        .findFirst()
                        .orElseThrow(() -> new RecursoNoEncontradoException("Relación imagen-categoría", idDueno));
                categoria.getImagenesCategoria().remove(imagen);
                imagen.getCategorias().remove(categoria);
                log.info("Imagen {} desacoplada de la categoría {}", idImagen, idDueno);
                yield toResponse(imagen);
            }
        };
    }

    /** Quita una imagen de la galería de un blog; si era su portada, el blog queda sin portada. */
    @Transactional
    public ImagenResponse desacoplarDeBlog(Long idBlog, Long idImagen, Authentication autenticacion) {
        Blog blog = buscarBlog(idBlog);
        BlogAutorizacion.verificarPersonal(autenticacion);
        ImagenUrl imagen = buscarImagen(idImagen);

        boolean enGaleria = blog.getImagenes().stream().anyMatch(i -> i.getId().equals(idImagen));
        boolean esPortada = blog.getImagenPrincipal() != null && blog.getImagenPrincipal().getId().equals(idImagen);
        if (!enGaleria && !esPortada) {
            throw new RecursoNoEncontradoException("Relación imagen-blog", idImagen + " en blog " + idBlog);
        }

        quitarDeBlog(blog, imagen);
        log.info("Imagen {} desacoplada del blog {}", idImagen, idBlog);
        return toResponse(imagen);
    }

    private void quitarDeBlog(Blog blog, ImagenUrl imagen) {
        blog.getImagenes().removeIf(i -> i.getId().equals(imagen.getId()));
        imagen.getBlogs().remove(blog);
        if (blog.getImagenPrincipal() != null && blog.getImagenPrincipal().getId().equals(imagen.getId())) {
            blog.setImagenPrincipal(null);
        }
    }

    /** Blogs que usan la imagen en su galería o como portada. */
    private Set<Blog> blogsDe(ImagenUrl imagen) {
        Set<Blog> blogs = new LinkedHashSet<>(imagen.getBlogs());
        blogs.addAll(blogRepository.findByImagenPrincipalId(imagen.getId()));
        return blogs;
    }

    /** /uploads/<subcarpeta>/<archivo> -> <subcarpeta>; cualquier otra forma cae en "general". */
    private static String subcarpetaDe(String url) {
        String[] partes = url.split("/");
        return partes.length >= 4 && "uploads".equals(partes[1]) ? partes[2] : "general";
    }

    /** El archivo se borra solo si la base confirmó el cambio; si no, se perdería una imagen que sigue en uso. */
    private void eliminarArchivoTrasCommit(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return; // URL externa: no hay archivo local que borrar
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    borrarArchivo(url);
                }
            });
        } else {
            borrarArchivo(url);
        }
    }

    private void borrarArchivo(String url) {
        try {
            almacenamiento.eliminar(url);
        } catch (RuntimeException e) {
            log.warn("La base ya no referencia {} pero no se pudo borrar el archivo", url, e);
        }
    }

    private ImagenUrl buscarImagen(Long id) {
        return imagenUrlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Imagen", id));
    }

    private Blog buscarBlog(Long idBlog) {
        return blogRepository.findById(idBlog)
                .orElseThrow(() -> new RecursoNoEncontradoException("Blog", idBlog));
    }

    private Blog buscarBlogVisible(Long idBlog, Authentication autenticacion) {
        Blog blog = buscarBlog(idBlog);
        if (!Boolean.TRUE.equals(blog.getVisible()) && !BlogAutorizacion.puedeVerBlogOculto(autenticacion)) {
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
