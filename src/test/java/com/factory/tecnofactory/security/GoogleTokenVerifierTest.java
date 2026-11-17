package com.factory.tecnofactory.security;

import com.factory.tecnofactory.exception.TokenGoogleInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenVerifierTest {

    private static final String TOKEN = "id-token-de-google";

    private static Jwt.Builder base() {
        return Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("google-123")
                .claim("email", "ana@correo.cl")
                .claim("given_name", "Ana")
                .claim("family_name", "Pérez")
                .claim("picture", "https://foto")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }

    private static GoogleTokenVerifier con(Jwt jwt) {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode(TOKEN)).thenReturn(jwt);
        return GoogleTokenVerifier.conDecoder(decoder);
    }

    @Test
    void tokenValidoDevuelveLosDatosDeGoogle() {
        Jwt jwt = base().claim("email_verified", true).build();

        GoogleTokenVerifier.GoogleUser usuario = con(jwt).verificar(TOKEN);

        assertEquals("google-123", usuario.sub());
        assertEquals("ana@correo.cl", usuario.email());
        assertEquals("Ana", usuario.nombre());
        assertEquals("Pérez", usuario.apellidos());
        assertEquals("https://foto", usuario.foto());
    }

    @Test
    void emailVerificadoComoTextoSeAcepta() {
        Jwt jwt = base().claim("email_verified", "true").build();

        assertEquals("ana@correo.cl", con(jwt).verificar(TOKEN).email());
    }

    @Test
    void emailNoVerificadoSeRechaza() {
        Jwt jwt = base().claim("email_verified", false).build();

        assertThrows(TokenGoogleInvalidoException.class, () -> con(jwt).verificar(TOKEN));
    }

    @Test
    void tokenSinCorreoSeRechaza() {
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "RS256")
                .subject("google-123")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThrows(TokenGoogleInvalidoException.class, () -> con(jwt).verificar(TOKEN));
    }

    @Test
    void unTokenQueElDecoderRechazaSeTraduceA401() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode(TOKEN)).thenThrow(new JwtException("firma inválida"));

        assertThrows(TokenGoogleInvalidoException.class,
                () -> GoogleTokenVerifier.conDecoder(decoder).verificar(TOKEN));
    }
}
