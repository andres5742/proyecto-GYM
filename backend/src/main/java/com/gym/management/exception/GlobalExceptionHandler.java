package com.gym.management.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String raw = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (raw != null && raw.contains("billing_payments_payment_type_check")) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de pago no permitido en la base de datos. Reinicie el backend para aplicar migraciones.");
        }
        if (raw != null && raw.contains("employee_cash_shortfalls") && raw.contains("shift_handover")) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "No se puede eliminar la entrega: tiene un descuadre de caja vinculado. "
                            + "Reinicie el backend con la versión actual o elimine primero el descuadre.");
        }
        if (raw != null && raw.contains("employee_cash_shortfalls_kind_check")) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de descuadre no permitido en la base de datos. Reinicie el backend para aplicar migraciones.");
        }
        if (raw != null
                && (raw.contains("access_logs_credential_type_check")
                        || raw.contains("member_biometric_credentials_credential_type_check")
                        || raw.contains("employee_biometric_credentials_credential_type_check"))) {
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de acceso (tarjeta/huella) no permitido en la base de datos. Reinicie el backend para aplicar migraciones.");
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "No se pudo guardar el registro por restricción de datos.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Cuenta no habilitada";
        return buildResponse(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Error de autenticación";
        return buildResponse(HttpStatus.UNAUTHORIZED, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Error de validación");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
