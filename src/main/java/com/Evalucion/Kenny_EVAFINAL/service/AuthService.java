package com.Evalucion.Kenny_EVAFINAL.service;

import com.Evalucion.Kenny_EVAFINAL.dto.auth.LoginRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.auth.LoginResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.auth.RegisterRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;

public interface AuthService {

    /**
     * Registra un usuario publico. El rol se fuerza a LECTOR y el estado a
     * ACTIVO: jamas se acepta un rol enviado por el cliente (REGLA de acceso
     * al endpoint publico).
     */
    UsuarioResponse registrar(RegisterRequest request);

    /** Autentica con BCrypt y emite un JWT. */
    LoginResponse login(LoginRequest request);
}
