package com.chargeslot.service.conflict;

import com.chargeslot.service.Intervals;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.List;

@Component @Order(3)
public class MaintenancePolicy implements ConflictPolicy {
    public List<Conflict> check(ConflictContext c) {
        return c.maintenances().stream().filter(m -> (m.getConnector() == null || m.getConnector().getId().equals(c.connector().getId())) &&
                Intervals.overlap(c.startAt(), c.endAt(), m.getStartAt(), m.getEndAt()))
            .map(m -> new Conflict("MAINTENANCE", "Indisponible du " + m.getStartAt() + " au " + m.getEndAt() + " : " + m.getReason())).toList();
    }
}
