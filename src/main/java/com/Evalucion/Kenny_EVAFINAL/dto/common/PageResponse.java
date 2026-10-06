package com.Evalucion.Kenny_EVAFINAL.dto.common;

import org.springframework.data.domain.Page;

/**
 * Representacion paginada de listados. Se devuelve siempre con la misma forma,
 * independientemente de si el listado viene de {@code Page} o de una lista.
 */
public record PageResponse<T>(java.util.List<T> contenido,
                              int pagina,
                              int tamanoPagina,
                              long totalElementos,
                              int totalPaginas,
                              boolean primeraPagina,
                              boolean ultimaPagina) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
