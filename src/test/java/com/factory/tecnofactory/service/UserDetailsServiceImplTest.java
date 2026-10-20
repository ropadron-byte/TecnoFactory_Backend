package com.factory.tecnofactory.service;

import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.repository.UsuarioRepository;
import com.factory.tecnofactory.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    private static Usuario usuario(String rol, boolean activo) {
        TipoUsuario tipo = new TipoUsuario();
        tipo.setNombre(rol);

        Usuario u = new Usuario();
        u.setCorreo("ana@correo.cl");
        u.setContrasenia("$2a$10$hashDePrueba");
        u.setActivo(activo);
        u.setTipoUsuario(tipo);
        return u;
    }

    @Test
    void cargaElUsuarioConSuHashYSuRolConPrefijo() {
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(usuario("VENDEDOR", true)));

        UserDetails ud = service.loadUserByUsername("ana@correo.cl");

        assertEquals("ana@correo.cl", ud.getUsername());
        assertEquals("$2a$10$hashDePrueba", ud.getPassword());
        assertEquals(List.of("ROLE_VENDEDOR"),
                ud.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertTrue(ud.isEnabled());
    }

    @Test
    void usuarioInactivoQuedaDeshabilitado() {
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(usuario("CLIENTE", false)));

        assertFalse(service.loadUserByUsername("ana@correo.cl").isEnabled());
    }

    @Test
    void correoInexistenteLanzaUsernameNotFound() {
        when(usuarioRepository.findByCorreo("nadie@correo.cl")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nadie@correo.cl"));
    }
}
