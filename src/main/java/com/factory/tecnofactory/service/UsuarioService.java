package com.factory.tecnofactory.service;

import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
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

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    
    private final ComunaRepository comunaRepository;
    private final PasswordEncoder passwordEncoder;

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
}