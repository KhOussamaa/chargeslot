package com.chargeslot.service.conflict;

import com.chargeslot.domain.*;
import com.chargeslot.service.Intervals;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component @Order(4)
public class PowerCapacityPolicy implements ConflictPolicy {
    // Intervalles [debut, fin[ : les variations simultanees sont agregees avant de calculer le pic.
    public static BigDecimal peak(List<Reservation> reservations, Instant start, Instant end) {
        TreeMap<Instant, BigDecimal> events = new TreeMap<>();
        for (Reservation r : reservations) {
            if (r.getStatus() != ReservationStatus.CONFIRMED || !Intervals.overlap(start, end, r.getStartAt(), r.getEndAt())) continue;
            Instant a = r.getStartAt().isBefore(start) ? start : r.getStartAt();
            Instant b = r.getEndAt().isAfter(end) ? end : r.getEndAt();
            events.merge(a, r.getRequestedPowerKw(), BigDecimal::add);
            events.merge(b, r.getRequestedPowerKw().negate(), BigDecimal::add);
        }
        BigDecimal current = BigDecimal.ZERO, peak = BigDecimal.ZERO;
        for (BigDecimal delta : events.values()) { current = current.add(delta); peak = peak.max(current); }
        return peak;
    }
    public BigDecimal available(ConflictContext c) {
        return c.station().getMaximumPowerKw().subtract(peak(c.reservations(), c.startAt(), c.endAt())).max(BigDecimal.ZERO);
    }
    public List<Conflict> check(ConflictContext c) {
        BigDecimal available = available(c);
        return c.requestedPowerKw().compareTo(available) > 0 ?
            List.of(new Conflict("POWER_ADJUSTMENT_REQUIRED", "La puissance partagee de la station est insuffisante.", available)) : List.of();
    }
}
