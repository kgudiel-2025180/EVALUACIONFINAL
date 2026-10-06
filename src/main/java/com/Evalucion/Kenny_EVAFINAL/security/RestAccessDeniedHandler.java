package com.Evalucion.Kenny_EVAFINAL.security;

import com.Evalucion.Kenny_EVAFINAL.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 con formato estandar cuando un usuario autenticado no tiene
 * el rol requerido por el endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Acceso denegado (rol insuficiente) a {} -> {}", request.getRequestURI(),
                accessDeniedException.getMessage());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
                "No tiene permisos para realizar esta operacion", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
