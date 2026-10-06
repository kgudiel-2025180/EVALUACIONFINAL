package com.Evalucion.Kenny_EVAFINAL.dto.libro;

import java.time.LocalDateTime;

public record LibroResponse(Long id,
                            String isbn,
                            String titulo,
                            String autor,
                            String categoria,
                            Integer stockTotal,
                            Integer stockDisponible,
                            LocalDateTime fechaCreacion) {
}
