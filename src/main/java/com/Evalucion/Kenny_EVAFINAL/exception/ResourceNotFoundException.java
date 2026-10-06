package com.Evalucion.Kenny_EVAFINAL.exception;

/**
 * Recurso inexistente o no disponible (por ejemplo, un libro dado de baja
 * logicamente). Se traduce a HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }

    public ResourceNotFoundException(String recurso, Object valor) {
        super(recurso + " no encontrado: " + valor);
    }
}
