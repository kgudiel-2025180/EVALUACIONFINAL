package com.Evalucion.Kenny_EVAFINAL.security;

import com.Evalucion.Kenny_EVAFINAL.config.JwtProperties;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Componente unico encargado de generar y validar tokens JWT.
 *
 * <p>Ni los controllers ni los filtros construyen tokens a mano: toda la
 * logica JWT vive aqui para poder cambiar de estrategia sin tocar el resto.</p>
 */
@Service
@Slf4j
public class JwtService {

    public static final String CLAIM_ROL = "rol";
    public static final String CLAIM_USUARIO_ID = "uid";

    private final JwtProperties properties;
    private final SecretKey clave;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos 32 bytes (256 bits) para usar HMAC-SHA");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        log.info("Servicio JWT inicializado (expiracion={} ms)", properties.getExpiration());
    }

    /**
     * Genera un token firmado para el usuario autenticado.
     * El rol viaja como claim para que el filtro pueda reconstruir las autoridades.
     */
    public String generarToken(Usuario usuario) {
        Date emision = new Date();
        Date expiracion = new Date(emision.getTime() + properties.getExpiration());
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_USUARIO_ID, usuario.getId())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .issuer(properties.getIssuer())
                .issuedAt(emision)
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    /** Valida firma y expiracion sin lanzar excepciones. */
    public boolean esValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT invalido: {}", e.getMessage());
            return false;
        }
    }

    /** Extrae el email (subject) del token. */
    public String extraerEmail(String token) {
        return extraerClaims(token).getSubject();
    }

    /** Extrae el rol del token. */
    public String extraerRol(String token) {
        return extraerClaims(token).get(CLAIM_ROL, String.class);
    }

    /** Duracion del token en segundos (para informar al cliente). */
    public long getExpiracionEnSegundos() {
        return properties.getExpiration() / 1000;
    }

    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
