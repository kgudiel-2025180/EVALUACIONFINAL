package com.Evalucion.Kenny_EVAFINAL.exception;

import java.time.LocalDateTime;

/**
 * Cuerpo estandar de error. Envia siempre {@code success=false} y nunca
 * incluye stack traces ni detalles internos de la infraestructura.
 *
 * <pre>{@code
 * {
 *   "timestamp": "2026-10-06T10:15:30",
 *   "success": false,
 *   "status": 400,
 *   "error": "BUSINESS_RULE_VIOLATION",
 *   "message": "El libro no tiene ejemplares disponibles",
 *   "path": "/api/v1/prestamos",
 *   "data": null
 * }
 * }</pre>
 */
public record ApiError(LocalDateTime timestamp,
                       boolean success,
                       int status,
                       String error,
                       String message,
                       String path,
                       Object data) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), false, status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path, Object data) {
        return new ApiError(LocalDateTime.now(), false, status, error, message, path, data);
    }
}
