package com.Evalucion.Kenny_EVAFINAL.dto.prestamo;

import com.Evalucion.Kenny_EVAFINAL.enums.PrestamoEstado;

import java.time.LocalDateTime;

/**
 * Vista de un prestamo con la informacion minima necesaria del libro y del usuario.
 * No expone entidades JPA ni relaciones bidireccionales.
 */
public record PrestamoResponse(Long id,
                               Long usuarioId,
                               String usuarioNombre,
                               String usuarioEmail,
                               Long libroId,
                               String libroTitulo,
                               String libroIsbn,
                               LocalDateTime fechaPrestamo,
                               LocalDateTime fechaDevolucionEsperada,
                               LocalDateTime fechaDevolucionReal,
                               PrestamoEstado estado) {
}
