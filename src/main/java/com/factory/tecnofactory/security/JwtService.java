package com.factory.tecnofactory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_PROPOSITO = "proposito";
    private static final String CLAIM_CORREO = "correo";
    private static final String CLAIM_NOMBRE = "nombre";
    private static final String CLAIM_APELLIDOS = "apellidos";
    private static final String PROPOSITO_REGISTRO_GOOGLE = "registro_google";
    private static final long EXPIRACION_REGISTRO_GOOGLE_MS = 600_000L; // 10 minutos

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${jwt.secret}") String secreto,
                      @Value("${jwt.expiration-ms}") long expiracionMs) {
        // Lanza una excepción si el secreto mide menos de 32 bytes: la app no arranca con una clave débil
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(String correo, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(correo)
                .claim(CLAIM_ROL, rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    /**
     * Token temporal que se entrega cuando alguien entra por primera vez con Google y todavía
     * no completó su perfil. No sirve como token de acceso: lleva el claim "proposito".
     */
    public String generarTokenRegistroGoogle(String googleId, String correo, String nombre, String apellidos) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(googleId)
                .claim(CLAIM_CORREO, correo)
                .claim(CLAIM_NOMBRE, nombre)
                .claim(CLAIM_APELLIDOS, apellidos)
                .claim(CLAIM_PROPOSITO, PROPOSITO_REGISTRO_GOOGLE)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + EXPIRACION_REGISTRO_GOOGLE_MS))
                .signWith(clave)
                .compact();
    }

    /** Lee un token de registro de Google y comprueba que realmente sea de ese tipo. */
    public RegistroGoogle leerRegistroGoogle(String token) {
        Claims claims = leerClaims(token);
        if (!PROPOSITO_REGISTRO_GOOGLE.equals(claims.get(CLAIM_PROPOSITO, String.class))) {
            throw new JwtException("El token no es un token de registro de Google");
        }
        return new RegistroGoogle(
                claims.getSubject(),
                claims.get(CLAIM_CORREO, String.class),
                claims.get(CLAIM_NOMBRE, String.class),
                claims.get(CLAIM_APELLIDOS, String.class));
    }

    /** Verifica la firma y la fecha de expiración; lanza JwtException si algo falla. */
    private Claims leerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Un token es válido para acceder a la API solo si está firmado, no expiró y no es un
     * token temporal de registro. Así un registroToken nunca se acepta como Bearer.
     */
    public boolean esValido(String token) {
        try {
            return leerClaims(token).get(CLAIM_PROPOSITO) == null;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String obtenerCorreo(String token) {
        return leerClaims(token).getSubject();
    }

    public String obtenerRol(String token) {
        return leerClaims(token).get(CLAIM_ROL, String.class);
    }

    public record RegistroGoogle(String googleId, String correo, String nombre, String apellidos) {
    }
}