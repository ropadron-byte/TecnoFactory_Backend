package com.factory.tecnofactory.service;

import com.factory.tecnofactory.dto.AuthResponse;
import com.factory.tecnofactory.dto.GoogleCompletarRequest;
import com.factory.tecnofactory.dto.GoogleLoginRequest;
import com.factory.tecnofactory.dto.GooglePerfilPendienteResponse;
import com.factory.tecnofactory.entity.Comuna;
import com.factory.tecnofactory.entity.TipoUsuario;
import com.factory.tecnofactory.entity.Usuario;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.exception.TokenGoogleInvalidoException;
import com.factory.tecnofactory.repository.ComunaRepository;
import com.factory.tecnofactory.repository.TipoUsuarioRepository;
import com.factory.tecnofactory.repository.UsuarioRepository;
import com.factory.tecnofactory.security.GoogleTokenVerifier;
import com.factory.tecnofactory.security.GoogleTokenVerifier.GoogleUser;
import com.factory.tecnofactory.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String TIPO_TOKEN = "Bearer";

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final ComunaRepository comunaRepository;
    private final JwtService jwtService;

    /**
     * Primer paso: verifica el token de Google.
     * - Cuenta ya vinculada: devuelve JWT.
     * - Cuenta con contraseña: pide iniciar sesión y vincular (evita el pre-registro).
     * - Cuenta nueva: devuelve un token temporal para completar el perfil.
     */
    @Transactional
    public Resultado iniciarSesion(GoogleLoginRequest request) {
        GoogleUser google = googleTokenVerifier.verificar(request.credential());

        Optional<Usuario> porGoogle = usuarioRepository.findByGoogleId(google.sub());
        if (porGoogle.isPresent()) {
            Usuario usuario = porGoogle.get();
            exigirActivo(usuario);
            return Resultado.completado(respuesta(usuario));
        }

        Optional<Usuario> porCorreo = usuarioRepository.findByCorreo(google.email());
        if (porCorreo.isPresent()) {
            Usuario usuario = porCorreo.get();
            exigirActivo(usuario);

            if (usuario.getGoogleId() != null) {
                throw new ReglaNegocioException(
                        "Este correo ya está vinculado a otra cuenta de Google");
            }

            if (usuario.getContrasenia() != null) {
                throw new ReglaNegocioException(
                        "Ya existe una cuenta con este correo. Inicia sesión con tu contraseña y vincula Google desde tu perfil");
            }

            // Cuenta creada con Google pero que perdió el google_id (por ejemplo, una migración):
            // el correo ya está verificado por Google, así que se vuelve a vincular.
            usuario.setGoogleId(google.sub());
            usuarioRepository.save(usuario);
            return Resultado.completado(respuesta(usuario));
        }

        String registroToken = jwtService.generarTokenRegistroGoogle(
                google.sub(), google.email(), google.nombre(), google.apellidos());

        return Resultado.pendiente(new GooglePerfilPendienteResponse(
                true,
                registroToken,
                google.email(),
                google.nombre(),
                google.apellidos()));
    }

    /** Segundo paso: crea la cuenta CLIENTE con los datos que Google no entrega. */
    @Transactional
    public AuthResponse completarPerfil(GoogleCompletarRequest request) {
        JwtService.RegistroGoogle registro;
        try {
            registro = jwtService.leerRegistroGoogle(request.registroToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenGoogleInvalidoException();
        }

        if (registro.googleId() == null || registro.correo() == null) {
            throw new TokenGoogleInvalidoException();
        }
        if (usuarioRepository.existsByCorreo(registro.correo())) {
            throw new ReglaNegocioException("El correo ya está registrado");
        }
        if (usuarioRepository.existsByGoogleId(registro.googleId())) {
            throw new ReglaNegocioException("Esta cuenta de Google ya está registrada");
        }
        if (usuarioRepository.existsByRun(request.run())) {
            throw new ReglaNegocioException("El RUN ya está registrado");
        }

        Comuna comuna = comunaRepository.findById(request.idComuna())
                .orElseThrow(() -> new RecursoNoEncontradoException("Comuna", request.idComuna()));
        TipoUsuario rol = tipoUsuarioRepository.findByNombre(ROL_CLIENTE)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol", ROL_CLIENTE));

        Usuario usuario = new Usuario();
        usuario.setRun(request.run());
        usuario.setRunDv(request.runDv().toUpperCase());
        usuario.setNombre(request.nombre());
        usuario.setApellidos(request.apellidos());
        usuario.setCorreo(registro.correo());
        usuario.setContrasenia(null);
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setDireccion(request.direccion());
        usuario.setTelefono(request.telefono());
        usuario.setGoogleId(registro.googleId());
        usuario.setTipoUsuario(rol);
        usuario.setComuna(comuna);

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario CLIENTE registrado con Google: {}", guardado.getCorreo());
        return respuesta(guardado);
    }

    /**
     * Vincula Google a una cuenta que ya inició sesión con contraseña.
     * Solo se permite si el correo de Google coincide con el de la cuenta.
     */
    @Transactional
    public void vincular(String correoAutenticado, GoogleLoginRequest request) {
        GoogleUser google = googleTokenVerifier.verificar(request.credential());

        if (!correoAutenticado.equalsIgnoreCase(google.email())) {
            throw new ReglaNegocioException("El correo de Google no coincide con tu cuenta");
        }

        Usuario usuario = usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correoAutenticado));
        exigirActivo(usuario);

        if (usuario.getGoogleId() != null && !usuario.getGoogleId().equals(google.sub())) {
            throw new ReglaNegocioException("Tu cuenta ya está vinculada a otra cuenta de Google");
        }

        Optional<Usuario> dueno = usuarioRepository.findByGoogleId(google.sub());
        if (dueno.isPresent() && !dueno.get().getId().equals(usuario.getId())) {
            throw new ReglaNegocioException("Esta cuenta de Google ya está vinculada a otro usuario");
        }

        if (usuario.getGoogleId() == null) {
            usuario.setGoogleId(google.sub());
            usuarioRepository.save(usuario);
            log.info("Cuenta {} vinculada a Google", usuario.getCorreo());
        }
    }

    private AuthResponse respuesta(Usuario usuario) {
        String rol = usuario.getTipoUsuario().getNombre();
        String token = jwtService.generarToken(usuario.getCorreo(), rol);
        return new AuthResponse(token, TIPO_TOKEN, usuario.getCorreo(), rol);
    }

    private void exigirActivo(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            // Mismo 401 genérico que el login con contraseña: no revela si la cuenta existe
            throw new BadCredentialsException("Credenciales inválidas");
        }
    }

    /** Resultado interno: o hay JWT, o hay que completar el perfil. */
    public record Resultado(AuthResponse auth, GooglePerfilPendienteResponse perfilPendiente) {

        public static Resultado completado(AuthResponse auth) {
            return new Resultado(auth, null);
        }

        public static Resultado pendiente(GooglePerfilPendienteResponse perfilPendiente) {
            return new Resultado(null, perfilPendiente);
        }

        public boolean requierePerfil() {
            return perfilPendiente != null;
        }
    }
}
