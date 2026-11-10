package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.BoletaResponse;
import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.entity.Boleta;
import com.factory.tecnofactory.entity.Carrito;
import com.factory.tecnofactory.entity.DetalleBoleta;
import com.factory.tecnofactory.entity.DetalleCarrito;
import com.factory.tecnofactory.entity.Producto;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.mapper.BoletaMapper;
import com.factory.tecnofactory.repository.BoletaRepository;
import com.factory.tecnofactory.repository.CarritoRepository;
import com.factory.tecnofactory.util.Precios;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoletaService {

    private static final String CARRITO_VACIO = "El carrito está vacío";

    private final BoletaRepository boletaRepository;
    private final CarritoRepository carritoRepository;
    private final BoletaMapper boletaMapper;

    /**
     * Convierte el carrito en una boleta: revisa el stock, lo descuenta, copia nombre, precio y
     * descuento de cada producto (para que la boleta no cambie si luego se edita el producto)
     * y deja el carrito vacío. Todo o nada: si algo falla, no se descuenta nada.
     */
    @Transactional
    public BoletaResponse generarDesdeCarrito(String correo) {
        Carrito carrito = carritoRepository.findByUsuarioCorreo(correo)
                .orElseThrow(() -> new ReglaNegocioException(CARRITO_VACIO));
        if (carrito.getDetalles().isEmpty()) {
            throw new ReglaNegocioException(CARRITO_VACIO);
        }

        Boleta boleta = new Boleta();
        boleta.setUsuario(carrito.getUsuario());
        boleta.setFecha(LocalDateTime.now());
        boleta.setEstado(true);

        List<DetalleCarrito> items = carrito.getDetalles().stream()
                .sorted(Comparator.comparing(d -> d.getProducto().getNombre()))
                .toList();

        long total = 0;
        for (DetalleCarrito item : items) {
            Producto producto = item.getProducto();
            if (!producto.getActivo()) {
                throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' ya no está disponible");
            }
            if (item.getCantidad() > producto.getStock()) {
                throw new ReglaNegocioException("Stock insuficiente para '" + producto.getNombre()
                        + "': quedan " + producto.getStock() + " unidades");
            }
            producto.setStock(producto.getStock() - item.getCantidad());

            DetalleBoleta detalle = new DetalleBoleta();
            detalle.setBoleta(boleta);
            detalle.setProducto(producto);
            detalle.setNombre(producto.getNombre());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setDescuento(producto.getDescuento());
            detalle.setCantidad(item.getCantidad());
            boleta.getDetalles().add(detalle);

            total += Precios.conDescuento(producto.getPrecio(), producto.getDescuento()) * item.getCantidad();
        }
        boleta.setTotalBoleta(total);

        Boleta guardada = boletaRepository.save(boleta);
        carrito.getDetalles().clear();

        log.info("Boleta {} generada para {} por un total de {}", guardada.getId(), correo, total);
        return boletaMapper.toResponse(guardada);
    }

    @Transactional(readOnly = true)
    public PageResponse<BoletaResponse> listarMias(String correo, Pageable pageable) {
        return PageResponse.de(boletaRepository
                .findByUsuarioCorreoOrderByFechaDesc(correo, pageable)
                .map(boletaMapper::toResponse));
    }

    /** Todas las boletas (administrador y vendedor). */
    @Transactional(readOnly = true)
    public PageResponse<BoletaResponse> listarTodas(Pageable pageable) {
        return PageResponse.de(boletaRepository
                .findAllByOrderByFechaDesc(pageable)
                .map(boletaMapper::toResponse));
    }

    /**
     * El personal ve cualquier boleta; un cliente solo las suyas. Si no es suya responde 404
     * (no se revela que existe).
     */
    @Transactional(readOnly = true)
    public BoletaResponse buscarPorId(Long id, String correo, boolean esPersonal) {
        Boleta boleta = boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta", id));
        if (!esPersonal && !boleta.getUsuario().getCorreo().equals(correo)) {
            throw new RecursoNoEncontradoException("Boleta", id);
        }
        return boletaMapper.toResponse(boleta);
    }

    /** Marca la boleta como anulada y devuelve el stock a los productos. */
    @Transactional
    public BoletaResponse anular(Long id) {
        Boleta boleta = boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta", id));
        if (!boleta.getEstado()) {
            throw new ReglaNegocioException("La boleta ya está anulada");
        }
        for (DetalleBoleta detalle : boleta.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
        }
        boleta.setEstado(false);

        log.info("Boleta {} anulada", id);
        return boletaMapper.toResponse(boleta);
    }
}
