package com.Evalucion.Kenny_EVAFINAL.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades JWT. El secreto real se inyecta desde la variable de entorno
 * {@code JWT_SECRET}; el valor por defecto solo sirve para desarrollo local.
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    /** Secreto HMAC-SHA de al menos 256 bits (32 bytes). */
    private String secret;

    /** Duracion del token en milisegundos. */
    private long expiration = 86_400_000L;

    private String issuer = "biblioteca-universitaria";
}
