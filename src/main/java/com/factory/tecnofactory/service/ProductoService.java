package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.dto.ProductoRequest;
import com.factory.tecnofactory.dto.ProductoResponse;
import com.factory.tecnofactory.entity.Categoria;
import com.factory.tecnofactory.entity.Producto;
import com.factory.tecnofactory.entity.ProductoCategoria;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.mapper.ProductoMapper;
import com.factory.tecnofactory.repository.CategoriaRepository;
import com.factory.tecnofactory.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;

    /** Catálogo público: solo productos activos, con filtro opcional por categoría. */
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarActivos(Long idCategoria, Pageable pageable) {
        Page<Producto> pagina = (idCategoria == null)
                ? productoRepository.findByActivoTrue(pageable)
                : productoRepository.findByActivoTrueAndCategorias_Categoria_Id(idCategoria, pageable);
        return PageResponse.de(pagina.map(productoMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarOfertas(Pageable pageable) {
        Page<Producto> pagina = productoRepository.findByActivoTrueAndDescuentoGreaterThan(0, pageable);
        return PageResponse.de(pagina.map(productoMapper::toResponse));
    }

    /** Para el panel: incluye los productos desactivados. */
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listarTodos(Pageable pageable) {
        return PageResponse.de(productoRepository.findAll(pageable).map(productoMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarCriticos() {
        return productoRepository.findCriticos().stream().map(productoMapper::toResponse).toList();
    }

    /** Un producto desactivado solo lo ve el personal; para el público es como si no existiera. */
    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id, boolean incluirInactivos) {
        Producto producto = productoRepository.findById(id)
                .filter(p -> incluirInactivos || p.getActivo())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
        return productoMapper.toResponse(producto);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        List<Categoria> categorias = obtenerCategorias(request.categoriaIds());

        Producto guardado = productoRepository.save(productoMapper.toEntity(request));
        asignarCategorias(guardado, categorias);

        log.info("Producto creado con id {}", guardado.getId());
        return productoMapper.toResponse(guardado);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidad(id);
        List<Categoria> categorias = obtenerCategorias(request.categoriaIds());

        productoMapper.actualizar(request, producto);
        asignarCategorias(producto, categorias);

        log.info("Producto {} actualizado", id);
        return productoMapper.toResponse(producto);
    }

    /** No se borra: las boletas antiguas apuntan al producto (la base lo impide). */
    @Transactional
    public void desactivar(Long id) {
        buscarEntidad(id).setActivo(false);
        log.info("Producto {} desactivado", id);
    }

    @Transactional
    public ProductoResponse activar(Long id) {
        Producto producto = buscarEntidad(id);
        producto.setActivo(true);
        log.info("Producto {} activado", id);
        return productoMapper.toResponse(producto);
    }

    private Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", id));
    }

    /** Falla con 404 si alguna categoría no existe. */
    private List<Categoria> obtenerCategorias(Set<Long> ids) {
        List<Categoria> encontradas = categoriaRepository.findAllById(ids);
        if (encontradas.size() != ids.size()) {
            Set<Long> hallados = encontradas.stream().map(Categoria::getId).collect(Collectors.toSet());
            Long faltante = ids.stream().filter(id -> !hallados.contains(id)).findFirst().orElseThrow();
            throw new RecursoNoEncontradoException("Categoría", faltante);
        }
        return encontradas;
    }

    /**
     * Deja el producto con exactamente esas categorías. Quita las que sobran y agrega las que
     * faltan (no borra y vuelve a insertar la misma fila: Hibernate hace los INSERT antes que
     * los DELETE y chocaría con la clave primaria).
     */
    private void asignarCategorias(Producto producto, List<Categoria> nuevas) {
        Set<Long> idsNuevos = nuevas.stream().map(Categoria::getId).collect(Collectors.toSet());
        producto.getCategorias().removeIf(pc -> !idsNuevos.contains(pc.getCategoria().getId()));

        Set<Long> idsActuales = producto.getCategorias().stream()
                .map(pc -> pc.getCategoria().getId())
                .collect(Collectors.toSet());
        for (Categoria categoria : nuevas) {
            if (!idsActuales.contains(categoria.getId())) {
                producto.getCategorias().add(new ProductoCategoria(producto, categoria));
            }
        }
    }
}
