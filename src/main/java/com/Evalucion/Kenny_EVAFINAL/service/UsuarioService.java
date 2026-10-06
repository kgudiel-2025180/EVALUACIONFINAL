package com.Evalucion.Kenny_EVAFINAL.service;

import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;

/**
 * Operaciones sobre usuarios. Centraliza tambien la <b>politica de sanciones</b>
 * (REGLA 4 y 5) para que pueda modificarse en un unico lugar sin tocar el
 * resto del sistema.
 */
public interface UsuarioService {

    /** Devuelve la entidad o lanza {@code ResourceNotFoundException}. */
    Usuario obtenerEntidadPorId(Long id);

    Usuario obtenerEntidadPorEmail(String email);

    UsuarioResponse obtenerPorId(Long id);

    boolean existeEmail(String email);

    /**
     * Cambia el estado del usuario a {@code SANCIONADO}.
     *
     * <p>Se ejecuta en una <b>transaccion propia (REQUIRES_NEW)</b>: la sancion debe
     * persistirse aunque la operacion de prestamo que la disparo termine en rollback.
     * De este modo, si se rechaza el prestamo por un vencimiento, el usuario queda
     * sancionado igualmente.</p>
     */
    void sancionar(Long usuarioId);
}
