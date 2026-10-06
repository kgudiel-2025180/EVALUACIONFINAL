package com.Evalucion.Kenny_EVAFINAL.util;

import com.Evalucion.Kenny_EVAFINAL.entity.Libro;
import com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException;

/**
 * REGLA 8: invariante de stock.
 *
 * <p>Se aplica en el alta, la edicion, el prestamo y la devolucion, de modo que
 * jamas persiste un libro con stock fuera de rango. Es la contraparte en Java de
 * los CHECK constraints definidos en {@code db/schema.sql}: la base de datos es
 * la ultima linea de defensa, no la unica.</p>
 */
public final class StockValidator {

    private StockValidator() {
    }

    public static void validar(int stockTotal, int stockDisponible) {
        if (stockTotal < 0) {
            throw new BusinessRuleException("El stock total no puede ser negativo");
        }
        if (stockDisponible < 0) {
            throw new BusinessRuleException("El stock disponible no puede ser negativo");
        }
        if (stockDisponible > stockTotal) {
            throw new BusinessRuleException("El stock disponible no puede superar el stock total");
        }
    }

    public static void validar(Libro libro) {
        validar(libro.getStockTotal(), libro.getStockDisponible());
    }
}
