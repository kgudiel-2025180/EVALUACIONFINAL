package com.Evalucion.Kenny_EVAFINAL.service;

import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;

/**
 * Politica de sanciones.
 *
 * <p><b>Decision de diseno:</b> la especificacion no define una politica de
 * levantamiento automatico de sanciones, por lo que <b>no se ha inventado ninguna
 * regla de reactivacion</b>. Toda la logica relacionada (deteccion de vencidos,
 * aplicacion de la sancion y verificacion de si un usuario puede prestar) vive
 * concentrada en esta interfaz, de modo que una futura politica (por ejemplo
 * "levantar sancion a los N dias") se implemente aqui sin afectar a los demas
 * servicios.</p>
 */
public interface SancionService {

    /**
     * Aplica las REGLAS 4 y 5 antes de conceder un prestamo:
     *
     * <ol>
     *   <li><b>REGLA 5:</b> si el usuario ya esta SANCIONADO, se rechaza.</li>
     *   <li><b>REGLA 4:</b> si es LECTOR y tiene un prestamo vencido, pasa
     *       automaticamente a SANCIONADO (en transaccion propia) y se rechaza.</li>
     * </ol>
     *
     * @throws com.Evalucion.Kenny_EVAFINAL.exception.BusinessRuleException si no
     *         puede recibir el prestamo
     */
    void validarPoliticaPreviaAPrestamo(Usuario usuario);
}
