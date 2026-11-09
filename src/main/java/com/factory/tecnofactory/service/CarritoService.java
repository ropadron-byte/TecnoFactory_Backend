package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.CarritoItemRequest;
import com.factory.tecnofactory.dto.CarritoResponse;
import com.factory.tecnofactory.entity.Carrito;
import com.factory.tecnofactory.entity.DetalleCarrito;
import com.factory.tecnofactory.entity.Producto;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.mapper.CarritoMapper;
import com.factory.tecnofactory.repository.CarritoRepository;
import com.factory.tecnofactory.repository.ProductoRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** El carrito siempre es el de quien tiene la sesión iniciada (se identifica por su correo). */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final CarritoMapper carritoMapper;

    @Transactional
    public CarritoResponse obtener(String correo) {
        return carritoMapper.toResponse(carritoDe(correo));
    }

    @Transactional
    public CarritoResponse agregar(String correo, CarritoItemRequest request) {
        Carrito carrito = carritoDe(correo);
        Producto producto = productoDisponible(request.idProducto());
        DetalleCarrito existente = buscarDetalle(carrito, producto.getId());

        int cantidadFinal = request.cantidad() + (existente == null ? 0 : existente.getCantidad());
        validarStock(producto, cantidadFinal);

        if (existente == null) {
            carrito.getDetalles().add(new DetalleCarrito(carrito, producto, cantidadFinal));
        } else {
            existente.setCantidad(cantidadFinal);
        }
        return carritoMapper.toResponse(carrito);
    }

    @Transactional
    public CarritoResponse cambiarCantidad(String correo, Long idProducto, int cantidad) {
        Carrito carrito = carritoDe(correo);
        DetalleCarrito detalle = buscarDetalle(carrito, idProducto);
        if (detalle == null) {
            throw new RecursoNoEncontradoException("Producto en el carrito", idProducto);
        }
        validarStock(detalle.getProducto(), cantidad);
        detalle.setCantidad(cantidad);
        return carritoMapper.toResponse(carrito);
    }

    @Transactional
    public CarritoResponse quitar(String correo, Long idProducto) {
        Carrito carrito = carritoDe(correo);
        boolean quitado = carrito.getDetalles()
                .removeIf(d -> d.getProducto().getId().equals(idProducto));
        if (!quitado) {
            throw new RecursoNoEncontradoException("Producto en el carrito", idProducto);
        }
        return carritoMapper.toResponse(carrito);
    }

    @Transactional
    public void vaciar(String correo) {
        carritoDe(correo).getDetalles().clear();
        log.info("Carrito vaciado ({})", correo);
    }

    /** Devuelve el carrito del usuario; si todavía no tiene, lo crea. */
    private Carrito carritoDe(String correo) {
        return carritoRepository.findByUsuarioCorreo(correo).orElseGet(() -> {
            Carrito nuevo = new Carrito();
            nuevo.setUsuario(usuarioRepository.findByCorreo(correo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correo)));
            return carritoRepository.save(nuevo);
        });
    }

    private Producto productoDisponible(Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto", idProducto));
        if (!producto.getActivo()) {
            throw new ReglaNegocioException("El producto '" + producto.getNombre() + "' no está disponible");
        }
        return producto;
    }

    private DetalleCarrito buscarDetalle(Carrito carrito, Long idProducto) {
        return carrito.getDetalles().stream()
                .filter(d -> d.getProducto().getId().equals(idProducto))
                .findFirst()
                .orElse(null);
    }

    private void validarStock(Producto producto, int cantidad) {
        if (cantidad > producto.getStock()) {
            throw new ReglaNegocioException("Stock insuficiente para '" + producto.getNombre()
                    + "': quedan " + producto.getStock() + " unidades");
        }
    }
}
