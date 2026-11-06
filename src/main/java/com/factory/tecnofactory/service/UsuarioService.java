package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.UsuarioRequest;                       // NUEVO
import com.factory.tecnofactory.dto.UsuarioResponse;                      // NUEVO
import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;          // NUEVO
import com.factory.tecnofactory.mapper.UsuarioMapper;                     // NUEVO
import com.factory.tecnofactory.repository.BlogRepository;
import com.factory.tecnofactory.repository.ComunaRepository;
import com.factory.tecnofactory.repository.TipoUsuarioRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_CLIENTE = "CLIENTE";                  // NUEVO
    private static final String ROL_VENDEDOR = "VENDEDOR";

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final ComunaRepository comunaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;
    private final BlogRepository blogRepository;

    @Transactional
    public boolean crearAdministradorSiNoExiste(DatosAdministrador datos) {
        if (usuarioRepository.existsByTipoUsuarioNombre(ROL_ADMINISTRADOR)) {
            log.info("Ya existe un administrador: no se crea otro");
            return false;
        }

        TipoUsuario rol = tipoUsuarioRepository.findByNombre(ROL_ADMINISTRADOR)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", ROL_ADMINISTRADOR));

        Comuna comuna = comunaRepository.findById(datos.idComuna())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comuna", datos.idComuna()));

        Usuario admin = new Usuario();
        admin.setRun(datos.run());
        admin.setRunDv(datos.runDv());
        admin.setNombre(datos.nombre());
        admin.setApellidos(datos.apellidos());
        admin.setCorreo(datos.correo());
        admin.setContrasenia(passwordEncoder.encode(datos.clave()));
        admin.setFechaNacimiento(datos.fechaNacimiento());
        admin.setDireccion(datos.direccion());
        admin.setTelefono(datos.telefono());
        admin.setTipoUsuario(rol);
        admin.setComuna(comuna);

        usuarioRepository.save(admin);
        log.info("Administrador inicial creado con el correo {}", admin.getCorreo());
        return true;
    }

    /** Registro público: siempre crea un CLIENTE. */
    @Transactional
    public UsuarioResponse registrar(UsuarioRequest request) {
        return crearConRol(request, ROL_CLIENTE);
    }

    /** Solo lo invoca el administrador (lo garantiza SecurityConfig sobre /api/usuarios/**). */
    @Transactional
    public UsuarioResponse crearVendedor(UsuarioRequest request) {
        return crearConRol(request, ROL_VENDEDOR);
    }

    private UsuarioResponse crearConRol(UsuarioRequest request, String nombreRol) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByRun(request.run())) {
            throw new ReglaNegocioException("El RUN ya está registrado");
        }

        Comuna comuna = comunaRepository.findById(request.idComuna())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comuna", request.idComuna()));

        TipoUsuario rol = tipoUsuarioRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", nombreRol));

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setRunDv(request.runDv().toUpperCase());
        usuario.setContrasenia(passwordEncoder.encode(request.clave()));
        usuario.setTipoUsuario(rol);
        usuario.setComuna(comuna);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario {} registrado con el correo {}", nombreRol, guardado.getCorreo());
        return usuarioMapper.toResponse(guardado);
    }

    @Transactional(readOnly = true)                                       // NUEVO
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        return usuarioMapper.toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorCorreo(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correo));
        return usuarioMapper.toResponse(usuario);
    }

    // ---- Baja de cuentas -------------------------------------------------------------------

    /** Un CLIENTE desactiva su propia cuenta: pierde el acceso al instante y sus datos y compras se conservan (no puede borrarla). */
    @Transactional
    public void desactivarMiCuenta(String correo) {
        Usuario usuario = buscarClienteAutenticado(correo);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        log.info("Cliente {} desactivó su cuenta", correo);
    }

    /** El administrador desactiva la cuenta de un VENDEDOR (no la de clientes ni la de otros administradores). */
    @Transactional
    public UsuarioResponse desactivarVendedor(Long id) {
        Usuario vendedor = buscarVendedor(id);
        vendedor.setActivo(false);
        Usuario guardado = usuarioRepository.save(vendedor);
        log.info("Vendedor {} desactivado por un administrador", guardado.getCorreo());
        return usuarioMapper.toResponse(guardado);
    }

    /** El administrador borra la cuenta de un VENDEDOR. Si ya escribió blogs, se pide desactivarla. */
    @Transactional
    public void eliminarVendedor(Long id) {
        Usuario vendedor = buscarVendedor(id);
        if (blogRepository.existsByUsuarioId(id)) {
            throw new ReglaNegocioException("El vendedor tiene blogs asociados; desactiva su cuenta en lugar de borrarla");
        }
        borrar(vendedor);
        log.info("Vendedor {} borrado por un administrador", vendedor.getCorreo());
    }

    private Usuario buscarClienteAutenticado(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correo));
        if (!ROL_CLIENTE.equals(usuario.getTipoUsuario().getNombre())) {
            throw new ReglaNegocioException("Esta acción es solo para cuentas de cliente");
        }
        return usuario;
    }

    private Usuario buscarVendedor(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
        if (!ROL_VENDEDOR.equals(usuario.getTipoUsuario().getNombre())) {
            throw new ReglaNegocioException("Solo se pueden gestionar cuentas de vendedores por esta vía");
        }
        return usuario;
    }

    /** flush() hace que una FK falle aquí y salga como 409, no al cerrar la transacción. */
    private void borrar(Usuario usuario) {
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
    }
}
