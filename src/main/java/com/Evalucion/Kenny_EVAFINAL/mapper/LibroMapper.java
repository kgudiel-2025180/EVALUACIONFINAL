package com.Evalucion.Kenny_EVAFINAL.mapper;

import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Libro;

public final class LibroMapper {

    private LibroMapper() {
    }

    public static LibroResponse aResponse(Libro libro) {
        if (libro == null) {
            return null;
        }
        return new LibroResponse(
                libro.getId(),
                libro.getIsbn(),
                libro.getTitulo(),
                libro.getAutor(),
                libro.getCategoria(),
                libro.getStockTotal(),
                libro.getStockDisponible(),
                libro.getFechaCreacion());
    }
}
