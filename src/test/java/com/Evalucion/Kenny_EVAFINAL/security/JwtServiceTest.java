package com.Evalucion.Kenny_EVAFINAL.security;

import com.Evalucion.Kenny_EVAFINAL.config.JwtProperties;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.enums.Rol;
import com.Evalucion.Kenny_EVAFINAL.enums.UsuarioEstado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Generacion y validacion de tokens JWT sin contexto Spring.
 */
class JwtServiceTest {

    private static final String CLAVE = "0123456789abcdef0123456789abcdef";

    private JwtService servicio;

    @BeforeEach
    void configurar() {
        JwtProperties propiedades = new JwtProperties();
        propiedades.setSecret(CLAVE);
        propiedades.setExpiration(60_000L);
        propiedades.setIssuer("biblioteca-universitaria");
        servicio = new JwtService(propiedades);
    }

    private Usuario usuario(Rol rol) {
        return Usuario.builder()
                .id(7L)
                .nombre("Usuario de prueba")
                .email("prueba@kinal.edu.gt")
                .password("$2a$10$Z5YVTBQeg1sdlJ96Thjd8e15Owr131ylaErOg.RKF4bLQT0laify6")
                .rol(rol)
                .estado(UsuarioEstado.ACTIVO)
                .build();
    }

    @Test
    @DisplayName("El token generado es valido y conserva el email y el rol")
    void tokenValido_conservaClaims() {
        String token = servicio.generarToken(usuario(Rol.LECTOR));

        assertNotNull(token);
        assertTrue(servicio.esValido(token));
        assertEquals("prueba@kinal.edu.gt", servicio.extraerEmail(token));
        assertEquals("LECTOR", servicio.extraerRol(token));
    }

    @Test
    @DisplayName("El rol del token es el que decide las autoridades")
    void tokenDeAdmin_conRolAdmin() {
        String token = servicio.generarToken(usuario(Rol.ADMIN));
        assertEquals("ADMIN", servicio.extraerRol(token));
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza")
    void tokenAjeno_esRechazado() {
        JwtProperties ajenas = new JwtProperties();
        ajenas.setSecret("otra-clave-completamente-distinta-32b");
        ajenas.setExpiration(60_000L);
        ajenas.setIssuer("biblioteca-universitaria");
        String tokenAjeno = new JwtService(ajenas).generarToken(usuario(Rol.LECTOR));

        assertFalse(servicio.esValido(tokenAjeno));
    }

    @Test
    @DisplayName("Un token emitido por otro issuer se rechaza")
    void tokenDeOtroIssuer_esRechazado() {
        JwtProperties otroIssuer = new JwtProperties();
        otroIssuer.setSecret(CLAVE);
        otroIssuer.setExpiration(60_000L);
        otroIssuer.setIssuer("otro-issuer");
        String token = new JwtService(otroIssuer).generarToken(usuario(Rol.LECTOR));

        assertFalse(servicio.esValido(token));
    }

    @Test
    @DisplayName("Un token expirado se rechaza")
    void tokenExpirado_esRechazado() throws InterruptedException {
        JwtProperties caduco = new JwtProperties();
        caduco.setSecret(CLAVE);
        caduco.setExpiration(1L);
        caduco.setIssuer("biblioteca-universitaria");
        String token = new JwtService(caduco).generarToken(usuario(Rol.LECTOR));

        Thread.sleep(50L);
        assertFalse(servicio.esValido(token));
    }

    @Test
    @DisplayName("Una cadena que no es JWT se rechaza sin excepciones")
    void cadenaBasura_esRechazada() {
        assertFalse(servicio.esValido("esto-no-es-un-token"));
        assertFalse(servicio.esValido(""));
    }

    @Test
    @DisplayName("Un secreto corto para HMAC-SHA es rechazado al arrancar")
    void secretoCorto_fallaAlArrancar() {
        JwtProperties corta = new JwtProperties();
        corta.setSecret("corta");
        assertThrows(IllegalStateException.class, () -> new JwtService(corta));
    }

    @Test
    @DisplayName("La expiracion se informa en segundos")
    void expiracion_enSegundos() {
        assertEquals(60L, servicio.getExpiracionEnSegundos());
    }
}
