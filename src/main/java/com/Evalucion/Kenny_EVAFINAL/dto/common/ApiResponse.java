package com.Evalucion.Kenny_EVAFINAL.dto.common;

/**
 * Envoltorio estandar de respuestas exitosas de la API.
 *
 * <pre>{@code
 * { "success": true, "message": "Préstamo registrado correctamente", "data": { ... } }
 * }</pre>
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
