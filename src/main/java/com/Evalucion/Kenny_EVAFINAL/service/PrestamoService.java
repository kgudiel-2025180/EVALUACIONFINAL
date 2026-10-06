package com.Evalucion.Kenny_EVAFINAL.service;

import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.DevolucionResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.prestamo.PrestamoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Reglas de prestamo, devolucion y atrasos (REGLAS 1-13).
 * Todas las operaciones que modifican stock o prestamos son transaccionales.
 */
public interface PrestamoService {

    /**
     * Registra un prestamo. Operacion atomica: prestamo + descuento de stock.
     *
     * @throws com.Evalucion.Kenny_EVAFINAL.exception.ResourceNotFoundException
     *         si el usuario o el libro no existen (REGLAS 9 y 10)
     * @throws com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException
     *         si viola stock, limite de prestamos o sancion (REGLAS 1, 2, 4, 5)
     */
    PrestamoResponse registrar(PrestamoRequest request);

    /**
     * Registra la devolucion. Operacion atomica: cierre del prestamo + incremento
     * de stock.
     *
     * @throws com.Evalucion.Kenny_EVAFINAL.exception.ResourceNotFoundException
     *         si el prestamo no existe (REGLA 11)
     * @throws com.Evalucion.Kenny_EVAFINAL.exception.ConflictException
     *         si ya fue devuelto (REGLA 12)
     */
    DevolucionResponse devolver(Long prestamoId);

    /** Historial completo del propio usuario. */
    Page<PrestamoResponse> obtenerMisPrestamos(Long usuarioId, Pageable pageable);

    /** Prestamos no devueltos con la fecha esperada vencida. */
    Page<PrestamoResponse> obtenerAtrasados(Pageable pageable);
}
