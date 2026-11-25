package com.chargeslot.exception;

import com.chargeslot.service.conflict.Conflict;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.math.BigDecimal;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(String code, String message, List<Conflict> conflicts, BigDecimal availablePowerKw, String proposal) { }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException ex) {
        BigDecimal available = ex.getConflicts().stream().filter(c -> c.availablePowerKw() != null).map(Conflict::availablePowerKw).findFirst().orElse(null);
        String proposal = available == null ? null : (available.signum() > 0 ? "Redemander une recharge limitee a " + available + " kW, ou choisir un autre creneau." : "Choisir un autre creneau.");
        return ResponseEntity.status(ex.getStatus()).body(new ApiError(ex.getCode(), ex.getMessage(), ex.getConflicts(), available, proposal));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).distinct().reduce((a,b) -> a + "; " + b).orElse("Requete invalide.");
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", message, List.of(), null, null));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> malformed(Exception ex) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "JSON, date ISO 8601, identifiant ou type de connecteur invalide.", List.of(), null, null));
    }
    @ExceptionHandler(CannotAcquireLockException.class)
    ResponseEntity<ApiError> busy(Exception ex) {
        return ResponseEntity.status(409).body(new ApiError("STATION_BUSY", "Station occupee par une autre operation. Reessayer.", List.of(), null, null));
    }
}
