package com.factory.tecnofactory.security;

import com.factory.tecnofactory.exception.TokenGoogleInvalidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Verifica el ID token que entrega Google Identity Services en el navegador.
 *
 * No se usa el login OAuth2 por redirección de Spring: el frontend obtiene el token y este
 * servicio comprueba firma, emisor, audiencia, expiración y que el correo esté verificado.
 */
@Service
public class GoogleTokenVerifier {

    private static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String EMISOR_GOOGLE = "https://accounts.google.com";
    private static final String EMISOR_GOOGLE_SIN_HTTPS = "accounts.google.com";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    private static final String CLAIM_GIVEN_NAME = "given_name";
    private static final String CLAIM_FAMILY_NAME = "family_name";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_PICTURE = "picture";

    private final JwtDecoder decoder;

    @Autowired
    public GoogleTokenVerifier(@Value("${google.client-id}") String clientId) {
        this(crearDecoder(clientId));
    }

    private GoogleTokenVerifier(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    /** Constructor para pruebas: permite sustituir el verificador real de Google. */
    static GoogleTokenVerifier conDecoder(JwtDecoder decoder) {
        return new GoogleTokenVerifier(decoder);
    }

    private static JwtDecoder crearDecoder(String clientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();

        OAuth2TokenValidator<Jwt> emisor = new JwtClaimValidator<String>(
                JwtClaimNames.ISS,
                iss -> EMISOR_GOOGLE.equals(iss) || EMISOR_GOOGLE_SIN_HTTPS.equals(iss));

        OAuth2TokenValidator<Jwt> audiencia = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                aud -> aud != null && aud.contains(clientId));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), emisor, audiencia));
        return decoder;
    }

    public GoogleUser verificar(String credential) {
        try {
            Jwt jwt = decoder.decode(credential);

            String correo = jwt.getClaimAsString(CLAIM_EMAIL);
            if (jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || correo == null || correo.isBlank()
                    || !estaVerificado(jwt)) {
                throw new TokenGoogleInvalidoException();
            }

            String nombre = jwt.getClaimAsString(CLAIM_GIVEN_NAME);
            if (nombre == null || nombre.isBlank()) {
                nombre = jwt.getClaimAsString(CLAIM_NAME);
            }

            return new GoogleUser(
                    jwt.getSubject(),
                    correo,
                    nombre,
                    jwt.getClaimAsString(CLAIM_FAMILY_NAME),
                    jwt.getClaimAsString(CLAIM_PICTURE));
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenGoogleInvalidoException();
        }
    }

    private static boolean estaVerificado(Jwt jwt) {
        Object valor = jwt.getClaims().get(CLAIM_EMAIL_VERIFIED);
        return Boolean.TRUE.equals(valor) || "true".equalsIgnoreCase(String.valueOf(valor));
    }

    public record GoogleUser(
            String sub,
            String email,
            String nombre,
            String apellidos,
            String foto
    ) {
    }
}
