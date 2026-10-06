package com.Evalucion.Kenny_EVAFINAL.enums;

/**
 * Estados de un prestamo.
 *
 * <ul>
 *   <li>{@code ACTIVO}: prestamo vigente y no vencido.</li>
 *   <li>{@code ATRASADO}: prestamo no devuelto cuya fecha de devolucion esperada ya paso.</li>
 *   <li>{@code DEVUELTO}: prestamo cerrado; un prestamo solo puede pasar a este estado una vez.</li>
 * </ul>
 */
public enum PrestamoEstado {

    ACTIVO,
    DEVUELTO,
    ATRASADO
}
