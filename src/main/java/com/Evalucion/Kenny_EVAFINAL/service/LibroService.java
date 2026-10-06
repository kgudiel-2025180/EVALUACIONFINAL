package com.Evalucion.Kenny_EVAFINAL.service;

import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.libro.LibroResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LibroService {

    /** Catalogo paginado con filtros opcionales por titulo y categoria. */
    Page<LibroResponse> listar(String titulo, String categoria, Pageable pageable);

    LibroResponse obtenerPorId(Long id);

    LibroResponse crear(LibroRequest request);

    LibroResponse actualizar(Long id, LibroRequest request);

    /** Eliminacion logica: conserva el historial de prestamos. */
    void eliminar(Long id);
}
