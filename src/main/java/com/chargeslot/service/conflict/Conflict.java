package com.chargeslot.service.conflict;
import java.math.BigDecimal;
public record Conflict(String code, String message, BigDecimal availablePowerKw) {
    public Conflict(String code, String message) { this(code, message, null); }
}
