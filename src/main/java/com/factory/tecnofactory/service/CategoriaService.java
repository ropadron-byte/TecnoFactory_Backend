package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.CategoriaRequest;
import com.factory.tecnofactory.dto.CategoriaResponse;
import com.factory.tecnofactory.entity.Categoria;
import com.factory.tecnofactory.entity.ImagenUrl;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.mapper.CategoriaMapper;
import com.factory.tecnofactory.repository.CategoriaRepository;
import com.factory.tecnofactory.repository.ProductoCategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoCategoriaRepository productoCategoriaRepository;
    private final CategoriaMapper categoriaMapper;
    private final ResolutorImagenes resolutorImagenes;

    /** Son pocas categorías, así que se devuelven todas sin paginar. */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll(Sort.by("nombre")).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return aRespuesta(buscarEntidad(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe una categoría llamada '" + nombre + "'");
        }

        Categoria categoria = categoriaMapper.toEntity(request);
        categoria.setNombre(nombre);
        asignarImagenes(categoria, request.imagenes());
        Categoria guardada = categoriaRepository.save(categoria);

        log.info("Categoría creada con id {}", guardada.getId());
        return aRespuesta(guardada);
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidad(id);
        String nombre = request.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe otra categoría llamada '" + nombre + "'");
        }

        categoriaMapper.actualizar(request, categoria);
        categoria.setNombre(nombre);
        asignarImagenes(categoria, request.imagenes());

        log.info("Categoría {} actualizada", id);
        return aRespuesta(categoria);
    }

    /** Solo se borra si ningún producto (ni siquiera uno desactivado) la usa. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarEntidad(id);
        if (productoCategoriaRepository.existsByCategoria_Id(id)) {
            throw new ReglaNegocioException("No se puede eliminar la categoría '" + categoria.getNombre()
                    + "' porque tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
        log.info("Categoría {} eliminada", id);
    }

    private Categoria buscarEntidad(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));
    }

    private CategoriaResponse aRespuesta(Categoria categoria) {
        long cantidad = productoCategoriaRepository.countByCategoria_IdAndProducto_ActivoTrue(categoria.getId());
        return categoriaMapper.toResponse(categoria, cantidad);
    }

    /** null = no tocar las imágenes; lista vacía = quitarlas todas. */
    private void asignarImagenes(Categoria categoria, List<String> urls) {
        if (urls == null) {
            return;
        }
        Set<ImagenUrl> nuevas = resolutorImagenes.resolver(urls);
        categoria.getImagenesCategoria().retainAll(nuevas);
        categoria.getImagenesCategoria().addAll(nuevas);
    }
}
