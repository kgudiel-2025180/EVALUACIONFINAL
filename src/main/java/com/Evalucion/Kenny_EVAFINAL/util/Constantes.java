package com.Evalucion.Kenny_EVAFINAL.util;

/**
 * Constantes de negocio del sistema de biblioteca.
 * Centralizadas para que un cambio de politica (por ejemplo el plazo de
 * devolucion) no obligue a buscar numeros sueltos por todo el codigo.
 */
public final class Constantes {

    /** REGLA 3: plazo fijo de devolucion en dias. */
    public static final int PRESTAMO_DIAS_PLAZO = 14;

    /** REGLA 2: prestamos simultaneos maximos para un LECTOR. */
    public static final int MAX_PRESTAMOS_ACTIVOS_LECTOR = 3;

    /** Limite superior de elementos por pagina de la API. */
    public static final int TAMANO_MAXIMO_PAGINA = 100;

    /** Tamano por defecto cuando el cliente no envia {@code size}. */
    public static final int TAMANO_PAGINA_POR_DEFECTO = 10;

    private Constantes() {
    }
}
