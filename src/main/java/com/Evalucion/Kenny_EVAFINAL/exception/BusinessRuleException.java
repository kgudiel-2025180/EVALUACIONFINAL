package com.Evalucion.Kenny_EVAFINAL.exception;

/**
 * Violacion de una regla de negocio (stock agotado, limite de prestamos,
 * usuario sancionado, etc.). Se traduce a HTTP 400.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String mensaje) {
        super(mensaje);
    }
}
