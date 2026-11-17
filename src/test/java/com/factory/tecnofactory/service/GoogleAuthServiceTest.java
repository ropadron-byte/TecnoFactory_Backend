package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.GoogleCompletarRequest;
import com.factory.tecnofactory.dto.GoogleLoginRequest;
import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.exception.TokenGoogleInvalidoException;
import com.factory.tecnofactory.repository.ComunaRepository;
import com.factory.tecnofactory.repository.TipoUsuarioRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import com.factory.tecnofactory.security.GoogleTokenVerifier;
import com.factory.tecnofactory.security.GoogleTokenVerifier.GoogleUser;
import com.factory.tecnofactory.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CREDENTIAL = "id-token-de-google";
    private static final GoogleUser GOOGLE = new GoogleUser(
            "google-123", "ana@correo.cl", "Ana", "Pérez", "https://foto");

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TipoUsuarioRepository tipoUsuarioRepository;
    @Mock
    private ComunaRepository comunaRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private GoogleAuthService service;

    private static Usuario usuario(Long id, String correo, String rol, boolean activo,
                                   String googleId, String contrasenia) {
        TipoUsuario tipo = new TipoUsuario();
        tipo.setNombre(rol);

        Usuario u = new Usuario();
        u.setId(id);
        u.setCorreo(correo);
        u.setActivo(activo);
        u.setGoogleId(googleId);
        u.setContrasenia(contrasenia);
        u.setTipoUsuario(tipo);
        return u;
    }

    @Test
    void cuentaYaVinculadaDevuelveJwt() {
        Usuario u = usuario(1L, "ana@correo.cl", "CLIENTE", true, "google-123", null);
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.of(u));
        when(jwtService.generarToken("ana@correo.cl", "CLIENTE")).thenReturn("jwt");

        GoogleAuthService.Resultado resultado = service.iniciarSesion(new GoogleLoginRequest(CREDENTIAL));

        assertTrue(!resultado.requierePerfil());
        assertEquals("jwt", resultado.auth().token());
        assertEquals("CLIENTE", resultado.auth().rol());
    }

    @Test
    void cuentaNuevaDevuelveTokenTemporalYPrefillDeGoogle() {
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.empty());
        when(jwtService.generarTokenRegistroGoogle("google-123", "ana@correo.cl", "Ana", "Pérez"))
                .thenReturn("registro-token");

        GoogleAuthService.Resultado resultado = service.iniciarSesion(new GoogleLoginRequest(CREDENTIAL));

        assertTrue(resultado.requierePerfil());
        assertEquals("registro-token", resultado.perfilPendiente().registroToken());
        assertEquals("ana@correo.cl", resultado.perfilPendiente().correo());
        assertEquals("Ana", resultado.perfilPendiente().nombre());
        assertEquals("Pérez", resultado.perfilPendiente().apellidos());

        // La cuenta no se crea hasta que el usuario envíe todos sus datos a /google/completar.
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cuentaConContrasenaNoSeVinculaAutomaticamente() {
        Usuario u = usuario(1L, "ana@correo.cl", "CLIENTE", true, null, "$2a$10$hash");
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(u));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> service.iniciarSesion(new GoogleLoginRequest(CREDENTIAL)));

        assertTrue(ex.getMessage().contains("vincula Google"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cuentaCreadaConGoogleSinIdSeVuelveAVincular() {
        Usuario u = usuario(1L, "ana@correo.cl", "CLIENTE", true, null, null);
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(u));
        when(usuarioRepository.save(u)).thenReturn(u);
        when(jwtService.generarToken("ana@correo.cl", "CLIENTE")).thenReturn("jwt");

        GoogleAuthService.Resultado resultado = service.iniciarSesion(new GoogleLoginRequest(CREDENTIAL));

        assertEquals("google-123", u.getGoogleId());
        assertEquals("jwt", resultado.auth().token());
    }

    @Test
    void cuentaDesactivadaRecibeElMismoErrorQueUnLoginFallido() {
        Usuario u = usuario(1L, "ana@correo.cl", "CLIENTE", false, "google-123", null);
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.of(u));

        assertThrows(BadCredentialsException.class,
                () -> service.iniciarSesion(new GoogleLoginRequest(CREDENTIAL)));
    }

    @Test
    void completarPerfilCreaClienteSinContrasenaYConGoogleId() {
        GoogleCompletarRequest request = new GoogleCompletarRequest(
                "registro-token", 12345678, "k", "Ana", "Pérez",
                LocalDate.of(1990, 5, 10), "Calle 1", "+56912345678", 13101);

        when(jwtService.leerRegistroGoogle("registro-token"))
                .thenReturn(new JwtService.RegistroGoogle("google-123", "ana@correo.cl", "Ana", "Pérez"));
        when(usuarioRepository.existsByCorreo("ana@correo.cl")).thenReturn(false);
        when(usuarioRepository.existsByGoogleId("google-123")).thenReturn(false);
        when(usuarioRepository.existsByRun(12345678)).thenReturn(false);

        Comuna comuna = new Comuna();
        comuna.setId(13101);
        when(comunaRepository.findById(13101)).thenReturn(Optional.of(comuna));

        TipoUsuario rol = new TipoUsuario();
        rol.setNombre("CLIENTE");
        when(tipoUsuarioRepository.findByNombre("CLIENTE")).thenReturn(Optional.of(rol));

        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generarToken("ana@correo.cl", "CLIENTE")).thenReturn("jwt");

        AuthResponse response = service.completarPerfil(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario guardado = captor.getValue();

        assertEquals("jwt", response.token());
        assertEquals("ana@correo.cl", guardado.getCorreo());
        assertEquals("K", guardado.getRunDv());
        assertNull(guardado.getContrasenia());
        assertEquals("google-123", guardado.getGoogleId());
        assertEquals("CLIENTE", guardado.getTipoUsuario().getNombre());
    }

    @Test
    void completarPerfilConRunDuplicadoNoCreaLaCuenta() {
        GoogleCompletarRequest request = new GoogleCompletarRequest(
                "registro-token", 12345678, "5", "Ana", "Pérez",
                LocalDate.of(1990, 5, 10), "Calle 1", "+56912345678", 13101);

        when(jwtService.leerRegistroGoogle("registro-token"))
                .thenReturn(new JwtService.RegistroGoogle("google-123", "ana@correo.cl", "Ana", "Pérez"));
        when(usuarioRepository.existsByCorreo("ana@correo.cl")).thenReturn(false);
        when(usuarioRepository.existsByGoogleId("google-123")).thenReturn(false);
        when(usuarioRepository.existsByRun(12345678)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> service.completarPerfil(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void completarPerfilConTokenInvalidoDevuelve401() {
        GoogleCompletarRequest request = new GoogleCompletarRequest(
                "token-malo", 12345678, "5", "Ana", "Pérez",
                LocalDate.of(1990, 5, 10), "Calle 1", "+56912345678", 13101);

        when(jwtService.leerRegistroGoogle("token-malo")).thenThrow(new JwtException("malo"));

        assertThrows(TokenGoogleInvalidoException.class, () -> service.completarPerfil(request));
    }

    @Test
    void vincularGoogleALaCuentaAutenticada() {
        Usuario u = usuario(1L, "ana@correo.cl", "CLIENTE", true, null, "$2a$10$hash");
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(u));
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());

        service.vincular("ana@correo.cl", new GoogleLoginRequest(CREDENTIAL));

        assertEquals("google-123", u.getGoogleId());
        verify(usuarioRepository).save(u);
    }

    @Test
    void noSePuedeVincularUnGoogleDeOtroCorreo() {
        GoogleUser otro = new GoogleUser("google-123", "otra@correo.cl", "Otra", "Pérez", null);
        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(otro);

        assertThrows(ReglaNegocioException.class,
                () -> service.vincular("ana@correo.cl", new GoogleLoginRequest(CREDENTIAL)));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noSePuedeVincularUnGoogleQueYaPerteneceAOtroUsuario() {
        Usuario u = usuario(1L, "ana@correo.cl", "CLIENTE", true, null, "$2a$10$hash");
        Usuario dueno = usuario(2L, "otra@correo.cl", "CLIENTE", true, "google-123", null);

        when(googleTokenVerifier.verificar(CREDENTIAL)).thenReturn(GOOGLE);
        when(usuarioRepository.findByCorreo("ana@correo.cl")).thenReturn(Optional.of(u));
        when(usuarioRepository.findByGoogleId("google-123")).thenReturn(Optional.of(dueno));

        assertThrows(ReglaNegocioException.class,
                () -> service.vincular("ana@correo.cl", new GoogleLoginRequest(CREDENTIAL)));
        verify(usuarioRepository, never()).save(any());
    }
}
