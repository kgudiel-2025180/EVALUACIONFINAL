package com.Evalucion.Kenny_EVAFINAL.dto.prestamo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud de prestamo. Solo identifica al usuario y al libro:
 * fechas, plazo y estado los calcula el backend.
 */
public record PrestamoRequest(
        @NotNull(message = "el id del usuario es obligatorio")
        @Positive(message = "el id del usuario debe ser positivo")
        Long usuarioId,

        @NotNull(message = "el id del libro es obligatorio")
        @Positive(message = "el id del libro debe ser positivo")
        Long libroId) {
}
