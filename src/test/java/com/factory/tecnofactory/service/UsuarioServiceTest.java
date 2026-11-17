package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.PerfilRequest;
import com.factory.tecnofactory.dto.UsuarioRequest;
import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.Region;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

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

    private static UsuarioRequest requestValido() {
        return new UsuarioRequest(6000000, "k", "Ana", "Pérez", "ana@correo.cl",
                "Clave12345", LocalDate.of(1990, 5, 20), "Calle Falsa 123",
                "+56912345678", 13101);
    }

    @Test
    void registraUnClienteConLaClaveEncriptada() {
        Region region = new Region();
        region.setId(13);
        region.setNombre("Metropolitana");
        Comuna comuna = new Comuna();
        comuna.setId(13101);
        comuna.setNombre("Santiago");
        comuna.setRegion(region);
        TipoUsuario cliente = new TipoUsuario();
        cliente.setId(3L);
        cliente.setNombre("CLIENTE");

        when(comunaRepository.findById(13101)).thenReturn(Optional.of(comuna));
        when(tipoUsuarioRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.encode("Clave12345")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = service.registrar(requestValido());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertEquals("HASH", guardado.getContrasenia());
        assertEquals("K", guardado.getRunDv());
        assertEquals("CLIENTE", guardado.getTipoUsuario().getNombre());
        assertEquals("CLIENTE", respuesta.tipoUsuario());
        assertEquals("Santiago", respuesta.comuna());
        assertEquals("Metropolitana", respuesta.region());
    }

    @Test
    void correoRepetidoLanzaReglaDeNegocio() {
        when(usuarioRepository.existsByCorreo("ana@correo.cl")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.registrar(requestValido()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void runRepetidoLanzaReglaDeNegocio() {
        when(usuarioRepository.existsByRun(6000000)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.registrar(requestValido()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void comunaInexistenteLanzaRecursoNoEncontrado() {
        when(comunaRepository.findById(13101)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.registrar(requestValido()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void buscarUnUsuarioInexistenteLanzaRecursoNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }
    @Test
    void buscarPorCorreoInexistenteLanzaRecursoNoEncontrado() {
        when(usuarioRepository.findByCorreo("nadie@correo.cl")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorCorreo("nadie@correo.cl"));
    }

    // ---- Perfil editable -------------------------------------------------------------------

    private static PerfilRequest perfilValido() {
        return new PerfilRequest(12345678, "5", "Ana María", "Pérez Soto",
                LocalDate.of(1991, 6, 15), "Nueva Calle 456", "+56987654321", 13102);
    }

    private static Usuario usuarioExistente() {
        Region region = new Region();
        region.setId(13);
        region.setNombre("Metropolitana");

        Comuna comuna = new Comuna();
        comuna.setId(13101);
        comuna.setNombre("Santiago");
        comuna.setRegion(region);

        TipoUsuario cliente = new TipoUsuario();
        cliente.setId(3L);
        cliente.setNombre("CLIENTE");

        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setRun(11111111);
        usuario.setRunDv("1");
        usuario.setNombre("Ana");
        usuario.setApellidos("Pérez");
        usuario.setCorreo("ana@correo.cl");
        usuario.setContrasenia("HASH");
        usuario.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        usuario.setDireccion("Calle 1");
        usuario.setTelefono("+56912345678");
        usuario.setTipoUsuario(cliente);
        usuario.setComuna(comuna);
        usuario.setActivo(true);
        return usuario;
    }

    @Test
    void actualizarPerfilGuardaLosDatosSinTocarCorreoNiContrasenia() {
        Usuario usuario = usuarioExistente();

        Region region = new Region();
        region.setId(13);
        region.setNombre("Metropolitana");
        Comuna nuevaComuna = new Comuna();
        nuevaComuna.setId(13102);
        nuevaComuna.setNombre("Cerrillos");
        nuevaComuna.setRegion(region);

        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByRunAndIdNot(12345678, 7L)).thenReturn(false);
        when(comunaRepository.findById(13102)).thenReturn(Optional.of(nuevaComuna));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioResponse respuesta = service.actualizarPerfil("ana@correo.cl", perfilValido());

        assertEquals(12345678, usuario.getRun());
        assertEquals("5", usuario.getRunDv());
        assertEquals("Ana María", usuario.getNombre());
        assertEquals("Pérez Soto", usuario.getApellidos());
        assertEquals(LocalDate.of(1991, 6, 15), usuario.getFechaNacimiento());
        assertEquals("Nueva Calle 456", usuario.getDireccion());
        assertEquals("+56987654321", usuario.getTelefono());
        assertEquals(13102, usuario.getComuna().getId());

        // Datos que el perfil no puede cambiar
        assertEquals("ana@correo.cl", usuario.getCorreo());
        assertEquals("HASH", usuario.getContrasenia());
        assertEquals("CLIENTE", usuario.getTipoUsuario().getNombre());

        assertEquals("Cerrillos", respuesta.comuna());
        assertEquals("ana@correo.cl", respuesta.correo());
    }

    @Test
    void actualizarPerfilConRunDeOtroUsuarioLanzaReglaDeNegocio() {
        Usuario usuario = usuarioExistente();
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByRunAndIdNot(12345678, 7L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.actualizarPerfil("ana@correo.cl", perfilValido()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarPerfilConComunaInexistenteLanzaRecursoNoEncontrado() {
        Usuario usuario = usuarioExistente();
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByRunAndIdNot(12345678, 7L)).thenReturn(false);
        when(comunaRepository.findById(13102)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizarPerfil("ana@correo.cl", perfilValido()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarPerfilDeUnUsuarioInexistenteLanzaRecursoNoEncontrado() {
        when(usuarioRepository.findByCorreo("nadie@correo.cl")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizarPerfil("nadie@correo.cl", perfilValido()));
    }

}