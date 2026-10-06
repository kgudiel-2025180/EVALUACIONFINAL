package com.Evalucion.Kenny_EVAFINAL.util;

import com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Normaliza el {@link Pageable} recibido por HTTP.
 *
 * <p>Protege la API contra valores invalidos (pagina negativa, tamano 0) y contra
 * paginas excesivamente grandes, sin depender de que el cliente envie parametros
 * bien formados.</p>
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static Pageable normalizar(Pageable pageable, Sort ordenPorDefecto) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, Constantes.TAMANO_PAGINA_POR_DEFECTO, ordenPorDefecto);
        }
        if (pageable.getPageNumber() < 0) {
            throw new BusinessRuleException("El numero de pagina no puede ser negativo");
        }
        if (pageable.getPageSize() < 1) {
            throw new BusinessRuleException("El tamano de pagina debe ser mayor que cero");
        }
        if (pageable.getPageSize() > Constantes.TAMANO_MAXIMO_PAGINA) {
            throw new BusinessRuleException(
                    "El tamano de pagina no puede superar " + Constantes.TAMANO_MAXIMO_PAGINA + " registros");
        }
        Sort orden = pageable.getSort().isSorted() ? pageable.getSort() : ordenPorDefecto;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), orden);
    }

    /** Evita que un listado vacio devuelva metadatos inconsistentes. */
    public static <T> Page<T> paginaVacia(Pageable pageable) {
        return Page.empty(pageable);
    }
}
