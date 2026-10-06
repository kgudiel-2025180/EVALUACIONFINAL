package com.Evalucion.Kenny_EVAFINAL.dto.prestamo;

import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;

import java.time.LocalDateTime;

/**
 * Confirmacion de una devolucion registrada, incluyendo el stock resultante del libro.
 */
public record DevolucionResponse(Long prestamoId,
                                 Long libroId,
                                 String libroTitulo,
                                 LocalDateTime fechaDevolucionReal,
                                 PrestamoEstado estado,
                                 Integer stockDisponibleActualizado) {
}
