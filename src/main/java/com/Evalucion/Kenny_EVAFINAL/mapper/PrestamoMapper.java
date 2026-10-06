package com.Evalucion.Kenny_EVAFINAL.mapper;

import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Prestamo;

/**
 * Mapea prestamos a DTOs incluyendo solo la informacion necesaria de usuario y
 * libro. Debe invocarse dentro de una transaccion porque las relaciones son
 * {@code FetchType.LAZY}.
 */
public final class PrestamoMapper {

    private PrestamoMapper() {
    }

    public static PrestamoResponse aResponse(Prestamo prestamo) {
        if (prestamo == null) {
            return null;
        }
        return new PrestamoResponse(
                prestamo.getId(),
                prestamo.getUsuario().getId(),
                prestamo.getUsuario().getNombre(),
                prestamo.getUsuario().getEmail(),
                prestamo.getLibro().getId(),
                prestamo.getLibro().getTitulo(),
                prestamo.getLibro().getIsbn(),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaDevolucionEsperada(),
                prestamo.getFechaDevolucionReal(),
                prestamo.getEstado());
    }
}
