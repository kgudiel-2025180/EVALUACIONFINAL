package com.Evalucion.Kenny_EVAFINAL.dto.auth;

import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;

/**
 * Respuesta de autenticacion. Nunca incluye la contrasena.
 */
public record LoginResponse(String token,
                            String tipoToken,
                            long expiracionEnSegundos,
                            UsuarioResponse usuario) {
}
