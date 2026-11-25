package com.chargeslot.service.conflict;

import com.chargeslot.domain.ReservationStatus;
import com.chargeslot.service.Intervals;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.List;

@Component @Order(1)
public class OverlapPolicy implements ConflictPolicy {
    public List<Conflict> check(ConflictContext c) {
        boolean found = c.reservations().stream().anyMatch(r -> r.getStatus() == ReservationStatus.CONFIRMED &&
            r.getConnector().getId().equals(c.connector().getId()) &&
            Intervals.overlap(c.startAt().minus(c.margin()), c.endAt().plus(c.margin()), r.getStartAt(), r.getEndAt()));
        return found ? List.of(new Conflict("OVERLAP", "Le connecteur est deja reserve sur ce creneau (marge comprise).")) : List.of();
    }
}
