package com.Evalucion.Kenny_EVAFINAL.enums;

/**
 * Estado de cuenta de un usuario.
 *
 * <p>La política de retiro de la sancion NO esta definida en la especificacion,
 * por lo que la logica de sancion/levantamiento se centraliza en
 * {@code SancionService} para poder modificarla sin tocar el resto del sistema.</p>
 */
public enum UsuarioEstado {

    ACTIVO,
    SANCIONADO
}
