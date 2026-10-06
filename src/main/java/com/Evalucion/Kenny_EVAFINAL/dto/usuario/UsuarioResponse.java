package com.Evalucion.Kenny_EVAFINAL.dto.usuario;

import com.Evalucion.Kenny_EVAFINAL.enums.Rol;
import com.Evalucion.Kenny_EVAFINAL.enums.UsuarioEstado;

import java.time.LocalDateTime;

/**
 * Vision publica de un usuario: nunca expone la contrasena.
 */
public record UsuarioResponse(Long id,
                              String nombre,
                              String email,
                              Rol rol,
                              UsuarioEstado estado,
                              LocalDateTime fechaCreacion) {
}
