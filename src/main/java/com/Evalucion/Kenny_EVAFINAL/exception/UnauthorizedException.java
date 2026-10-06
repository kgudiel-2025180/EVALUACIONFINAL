package com.Evalucion.Kenny_EVAFINAL.exception;

/**
 * Credenciales o token invalidos. Se traduce a HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String mensaje) {
        super(mensaje);
    }
}
