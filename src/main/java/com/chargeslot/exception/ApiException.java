package com.chargeslot.exception;

import com.chargeslot.service.conflict.Conflict;
import org.springframework.http.HttpStatus;
import java.util.List;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<Conflict> conflicts;
    public ApiException(HttpStatus status, String code, String message) { this(status, code, message, List.of()); }
    public ApiException(HttpStatus status, String code, String message, List<Conflict> conflicts) {
        super(message); this.status = status; this.code = code; this.conflicts = conflicts;
    }
    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public List<Conflict> getConflicts() { return conflicts; }
    public static ApiException notFound(String type) { return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", type + " introuvable."); }
    public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message); }
}
