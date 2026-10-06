package com.Evalucion.Kenny_EVAFINAL.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Manejador global de excepciones: toda la API responde con el mismo formato
 * y con codigos HTTP coherentes. No se filtran stack traces al cliente.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return responder(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Regla de negocio violada en {}: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflicto en {}: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Acceso no autorizado en {}: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Fallo de autenticacion en {}: {}", request.getRequestURI(), ex.getMessage());
        String mensaje = (ex instanceof BadCredentialsException)
                ? "Credenciales invalidas"
                : "No se pudo autenticar la solicitud";
        return responder(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", mensaje, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acceso denegado en {}: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tiene permisos para realizar esta operacion", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<CampoError> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(this::aCampoError)
                .toList();
        String mensaje = "La solicitud contiene datos invalidos";
        log.warn("Validacion fallida en {}: {}", request.getRequestURI(), errores);
        return responder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", mensaje, request, errores);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        log.warn("Violacion de restricciones en {}: {}", request.getRequestURI(), ex.getMessage());
        return responder(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos invalidos", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        String mensaje = "El parametro '" + ex.getName() + "' tiene un valor invalido";
        return responder(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", mensaje, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la solicitud no es valido", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                             HttpServletRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "El recurso solicitado no existe", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Error inesperado en {}", request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Ocurrio un error inesperado. Contacte al administrador del sistema.", request);
    }

    private CampoError aCampoError(FieldError fieldError) {
        return new CampoError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiError> responder(HttpStatus status, String codigo, String mensaje,
                                               HttpServletRequest request) {
        return responder(status, codigo, mensaje, request, null);
    }

    private ResponseEntity<ApiError> responder(HttpStatus status, String codigo, String mensaje,
                                               HttpServletRequest request, Object data) {
        ApiError error = ApiError.of(status.value(), codigo, mensaje, request.getRequestURI(), data);
        return ResponseEntity.status(status).body(error);
    }

    public record CampoError(String campo, String error) {
    }
}
