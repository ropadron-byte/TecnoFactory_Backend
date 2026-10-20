package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.UsuarioRequest;                       // NUEVO
import com.factory.tecnofactory.dto.UsuarioResponse;                      // NUEVO
import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;          // NUEVO
import com.factory.tecnofactory.mapper.UsuarioMapper;                     // NUEVO
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

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final ComunaRepository comunaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

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

    @Transactional                                                        // NUEVO
    public UsuarioResponse registrar(UsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByRun(request.run())) {
            throw new ReglaNegocioException("El RUN ya está registrado");
        }

        Comuna comuna = comunaRepository.findById(request.idComuna())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comuna", request.idComuna()));

        TipoUsuario rol = tipoUsuarioRepository.findByNombre(ROL_CLIENTE)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", ROL_CLIENTE));

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setRunDv(request.runDv().toUpperCase());
        usuario.setContrasenia(passwordEncoder.encode(request.clave()));
        usuario.setTipoUsuario(rol);
        usuario.setComuna(comuna);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario registrado con el correo {}", guardado.getCorreo());
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


}