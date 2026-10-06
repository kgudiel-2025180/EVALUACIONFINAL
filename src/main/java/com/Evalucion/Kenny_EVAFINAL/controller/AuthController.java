package com.Evalucion.Kenny_EVAFINAL.controller;

import com.Evalucion.Kenny_EVAFINAL.dto.auth.LoginRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.auth.LoginResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.auth.RegisterRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.common.ApiResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;
import com.Evalucion.Kenny_EVAFINAL.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos de autenticacion. Ninguno de los dos requiere token.
 * Toda la logica de negocio vive en {@link AuthService}; el controller solo
 * valida y traduce a HTTP.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 201 CREATED. El rol siempre sera LECTOR, sin importar lo que envie el cliente. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UsuarioResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UsuarioResponse usuario = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario registrado correctamente", usuario));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse respuesta = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Autenticacion exitosa", respuesta));
    }
}
