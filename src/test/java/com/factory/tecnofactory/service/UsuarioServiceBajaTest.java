package com.factory.tecnofactory.service;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceBajaTest {

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

    private static Usuario usuario(Long id, String correo, String rol) {
        Region region = new Region();
        region.setId(13);
        region.setNombre("Metropolitana");
        Comuna comuna = new Comuna();
        comuna.setId(13101);
        comuna.setNombre("Santiago");
        comuna.setRegion(region);
        TipoUsuario tipo = new TipoUsuario();
        tipo.setNombre(rol);

        Usuario u = new Usuario();
        u.setId(id);
        u.setRun(12345678);
        u.setRunDv("5");
        u.setNombre("Ana");
        u.setApellidos("Pérez");
        u.setCorreo(correo);
        u.setDireccion("Calle 1");
        u.setTelefono("+56912345678");
        u.setFechaNacimiento(LocalDate.of(1990, 5, 20));
        u.setContrasenia("hash");
        u.setTipoUsuario(tipo);
        u.setComuna(comuna);
        return u;
    }

    @Test
    void clienteDesactivaSuCuentaYNoSeBorra() {
        Usuario cliente = usuario(1L, "ana@correo.cl", "CLIENTE");
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(cliente));

        service.desactivarMiCuenta("ana@correo.cl");

        assertFalse(cliente.getActivo());
        verify(usuarioRepository).save(cliente);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void unVendedorNoPuedeUsarLaDesactivacionDeCliente() {
        when(usuarioRepository.findByCorreo("luis@correo.cl"))
                .thenReturn(Optional.of(usuario(2L, "luis@correo.cl", "VENDEDOR")));

        assertThrows(ReglaNegocioException.class, () -> service.desactivarMiCuenta("luis@correo.cl"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void administradorDesactivaUnVendedor() {
        Usuario vendedor = usuario(2L, "luis@correo.cl", "VENDEDOR");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedor));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);

        UsuarioResponse r = service.desactivarVendedor(2L);

        assertFalse(r.activo());
        assertEquals("VENDEDOR", r.tipoUsuario());
    }

    @Test
    void noSePuedeDesactivarAUnClienteNiAUnAdministradorPorLaRutaDeVendedores() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ana@correo.cl", "CLIENTE")));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, "adm@correo.cl", "ADMINISTRADOR")));

        assertThrows(ReglaNegocioException.class, () -> service.desactivarVendedor(1L));
        assertThrows(ReglaNegocioException.class, () -> service.eliminarVendedor(3L));
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void administradorBorraUnVendedorSinBlogs() {
        Usuario vendedor = usuario(2L, "luis@correo.cl", "VENDEDOR");
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(vendedor));
        when(blogRepository.existsByUsuarioId(2L)).thenReturn(false);

        service.eliminarVendedor(2L);

        verify(usuarioRepository).delete(vendedor);
        verify(usuarioRepository).flush();
    }

    @Test
    void noSeBorraUnVendedorConBlogs() {
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "luis@correo.cl", "VENDEDOR")));
        when(blogRepository.existsByUsuarioId(2L)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.eliminarVendedor(2L));
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void vendedorInexistenteDa404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminarVendedor(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.desactivarVendedor(99L));
    }
}
