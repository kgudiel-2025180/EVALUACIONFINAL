package com.Evalucion.Kenny_EVAFINAL.mapper;

import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;

/**
 * Convierte entidades en DTOs de salida. Los controladores jamas devuelven
 * entidades JPA, de ahi la necesidad de mappers separados.
 */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse aResponse(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.getFechaCreacion());
    }
}
