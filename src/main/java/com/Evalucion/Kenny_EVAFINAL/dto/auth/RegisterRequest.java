package com.Evalucion.Kenny_EVAFINAL.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de registro publico.
 *
 * <p>El rol NO se acepta en el payload: el endpoint siempre asigna
 * {@code LECTOR} y estado {@code ACTIVO}. BCrypt limita la contrasena a 72 bytes.</p>
 */
public record RegisterRequest(
        @NotBlank(message = "el nombre es obligatorio")
        @Size(min = 2, max = 100, message = "el nombre debe tener entre 2 y 100 caracteres")
        String nombre,

        @NotBlank(message = "el email es obligatorio")
        @Email(message = "el email no tiene un formato valido")
        @Size(max = 150, message = "el email no puede superar 150 caracteres")
        String email,

        @NotBlank(message = "la contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "la contrasena debe tener entre 8 y 72 caracteres")
        String password) {
}
