package com.Evalucion.Kenny_EVAFINAL.exception;

/**
 * Conflicto con el estado actual del recurso (email o ISBN duplicado,
 * operacion ya realizada). Se traduce a HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}
