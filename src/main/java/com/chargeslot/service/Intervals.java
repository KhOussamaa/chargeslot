package com.chargeslot.service;

import com.chargeslot.exception.ApiException;
import java.time.*;

public final class Intervals {
    private Intervals() { }
    public static boolean overlap(Instant a, Instant b, Instant c, Instant d) { return a.isBefore(d) && b.isAfter(c); }
    public static void validate(Instant start, Instant end, Clock clock) {
        if (start == null || end == null || !start.isBefore(end)) throw ApiException.badRequest("Le debut doit preceder la fin.");
        if (start.isBefore(clock.instant())) throw ApiException.badRequest("Le creneau doit commencer dans le futur.");
        if (start.isAfter(clock.instant().plus(Duration.ofDays(366)))) throw ApiException.badRequest("Reservation possible au maximum un an a l'avance.");
        if (Duration.between(start, end).compareTo(Duration.ofDays(1)) > 0) throw ApiException.badRequest("Un creneau dure au maximum 24 heures.");
    }
}
