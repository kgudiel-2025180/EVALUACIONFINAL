package com.Evalucion.Kenny_EVAFINAL.service.impl;

import com.Evalucion.Kenny_EVAFINAL.dto.auth.LoginRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.auth.LoginResponse;
import com.Evalucion.Kenny_EVAFINAL.dto.auth.RegisterRequest;
import com.Evalucion.Kenny_EVAFINAL.dto.usuario.UsuarioResponse;
import com.Evalucion.Kenny_EVAFINAL.entity.Usuario;
import com.Evalucion.Kenny_EVAFINAL.enums.Rol;
import com.Evalucion.Kenny_EVAFINAL.enums.UsuarioEstado;
import com.Evalucion.Kenny_EVAFINAL.exception.ConflictException;
import com.Evalucion.Kenny_EVAFINAL.mapper.UsuarioMapper;
import com.Evalucion.Kenny_EVAFINAL.repository.UsuarioRepository;
import com.Evalucion.Kenny_EVAFINAL.security.JwtService;
import com.Evalucion.Kenny_EVAFINAL.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * El rol <b>no</b> proviene del request: se fuerza siempre a LECTOR y el estado
     * a ACTIVO. De este modo es imposible registrarse como ADMIN o BIBLIOTECARIO
     * desde el endpoint publico.
     */
    @Override
    @Transactional
    public UsuarioResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("El email ya esta registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .rol(Rol.LECTOR)
                .estado(UsuarioEstado.ACTIVO)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario registrado: #{} ({}) rol={}", guardado.getId(), guardado.getEmail(), guardado.getRol());
        return UsuarioMapper.aResponse(guardado);
    }

    /**
     * La verificacion de credenciales la realiza {@link AuthenticationManager}:
     * busca el usuario y compara con BCrypt. Si falla, lanza
     * {@code AuthenticationException}, que el {@code GlobalExceptionHandler}
     * traduce a 401.
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        String email = normalizarEmail(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ConflictException("Usuario no encontrado"));

        String token = jwtService.generarToken(usuario);
        log.info("Login exitoso: {} ({})", usuario.getEmail(), usuario.getRol());

        return new LoginResponse(token, "Bearer", jwtService.getExpiracionEnSegundos(),
                UsuarioMapper.aResponse(usuario));
    }

    /**
     * Normalizacion unica del email (minusculas y sin espacios) para que el campo
     * unico de base de datos no acepte variantes distintas de la misma direccion.
     */
    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
