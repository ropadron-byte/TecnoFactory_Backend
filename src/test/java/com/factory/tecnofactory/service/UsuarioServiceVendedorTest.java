package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.UsuarioRequest;
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
class UsuarioServiceVendedorTest {

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

    private static UsuarioRequest request() {
        return new UsuarioRequest(6000000, "k", "Luis", "Soto", "luis@correo.cl",
                "Clave12345", LocalDate.of(1990, 5, 20), "Calle 1", "+56912345678", 13101);
    }

    @Test
    void creaUnUsuarioConRolVendedorYClaveEncriptada() {
        Region region = new Region();
        region.setId(13);
        region.setNombre("Metropolitana");
        Comuna comuna = new Comuna();
        comuna.setId(13101);
        comuna.setNombre("Santiago");
        comuna.setRegion(region);
        TipoUsuario vendedor = new TipoUsuario();
        vendedor.setId(2L);
        vendedor.setNombre("VENDEDOR");

        when(comunaRepository.findById(13101)).thenReturn(Optional.of(comuna));
        when(tipoUsuarioRepository.findByNombre("VENDEDOR")).thenReturn(Optional.of(vendedor));
        when(passwordEncoder.encode("Clave12345")).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse respuesta = service.crearVendedor(request());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals("HASH", captor.getValue().getContrasenia());
        assertEquals("K", captor.getValue().getRunDv());
        assertEquals("VENDEDOR", respuesta.tipoUsuario());
    }

    @Test
    void rechazaUnCorreoYaRegistrado() {
        when(usuarioRepository.existsByCorreo("luis@correo.cl")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.crearVendedor(request()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rechazaUnRunYaRegistrado() {
        when(usuarioRepository.existsByRun(6000000)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.crearVendedor(request()));
    }
}
