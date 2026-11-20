package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.PageResponse;
import com.factory.tecnofactory.dto.UsuarioActualizarRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.Region;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.mapper.UsuarioMapperImpl;
import com.factory.tecnofactory.repository.BlogRepository;
import com.factory.tecnofactory.repository.ComunaRepository;
import com.factory.tecnofactory.repository.TipoUsuarioRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceAdminTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TipoUsuarioRepository tipoUsuarioRepository;
    @Mock
    private ComunaRepository comunaRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BlogRepository blogRepository;

    private UsuarioService service;

    @BeforeEach
    void crearService() {
        service = new UsuarioService(usuarioRepository, tipoUsuarioRepository,
                comunaRepository, passwordEncoder, new UsuarioMapperImpl(), blogRepository);
    }

    private static Usuario vendedor() {
        Region region = new Region();
        region.setId(13);
        region.setNombre("Metropolitana");
        Comuna comuna = new Comuna();
        comuna.setId(13101);
        comuna.setNombre("Santiago");
        comuna.setRegion(region);
        TipoUsuario rol = new TipoUsuario();
        rol.setId(2L);
        rol.setNombre("VENDEDOR");

        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setRun(6000000);
        usuario.setRunDv("K");
        usuario.setNombre("Luis");
        usuario.setApellidos("Soto");
        usuario.setCorreo("luis@correo.cl");
        usuario.setContrasenia("HASH-VIEJO");
        usuario.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        usuario.setDireccion("Calle 1");
        usuario.setTelefono("+56912345678");
        usuario.setActivo(true);
        usuario.setTipoUsuario(rol);
        usuario.setComuna(comuna);
        return usuario;
    }

    private static UsuarioActualizarRequest request() {
        return new UsuarioActualizarRequest(6000000, "k", "Luis", "Soto", "luis@correo.cl",
                "", LocalDate.of(1990, 5, 20), "Calle 2", "+56912345678", 13101, true);
    }

    @Test
    void listaUsuariosPaginadosParaElPanel() {
        when(usuarioRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(vendedor()), PageRequest.of(0, 20), 1));

        PageResponse<UsuarioResponse> respuesta = service.listar(PageRequest.of(0, 20));

        assertEquals(1, respuesta.contenido().size());
        assertEquals("VENDEDOR", respuesta.contenido().get(0).tipoUsuario());
        assertEquals("Santiago", respuesta.contenido().get(0).comuna());
    }

    @Test
    void actualizaSinTocarLaClaveSiVieneVacia() {
        Usuario usuario = vendedor();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreoAndIdNot("luis@correo.cl", 2L)).thenReturn(false);
        when(usuarioRepository.existsByRunAndIdNot(6000000, 2L)).thenReturn(false);
        when(comunaRepository.findById(13101)).thenReturn(Optional.of(usuario.getComuna()));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = service.actualizarPorAdministrador(2L, request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("HASH-VIEJO", captor.getValue().getContrasenia());
        assertEquals("Calle 2", respuesta.direccion());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void actualizaLaClaveCuandoSeEnvia() {
        Usuario usuario = vendedor();
        UsuarioActualizarRequest conClave = new UsuarioActualizarRequest(6000000, "k", "Luis", "Soto",
                "luis@correo.cl", "Clave12345", LocalDate.of(1990, 5, 20), "Calle 2",
                "+56912345678", 13101, true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreoAndIdNot(any(), any())).thenReturn(false);
        when(usuarioRepository.existsByRunAndIdNot(any(), any())).thenReturn(false);
        when(comunaRepository.findById(13101)).thenReturn(Optional.of(usuario.getComuna()));
        when(passwordEncoder.encode("Clave12345")).thenReturn("HASH-NUEVO");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.actualizarPorAdministrador(2L, conClave);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("HASH-NUEVO", captor.getValue().getContrasenia());
    }

    @Test
    void rechazaCorreoDuplicadoDeOtroUsuario() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedor()));
        when(usuarioRepository.existsByCorreoAndIdNot("luis@correo.cl", 2L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizarPorAdministrador(2L, request()));
        verify(usuarioRepository, never()).save(any());
    }
}
