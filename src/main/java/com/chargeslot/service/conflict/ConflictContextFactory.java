package com.chargeslot.service.conflict;

import com.chargeslot.domain.*;
import com.chargeslot.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.*;

@Component
public class ConflictContextFactory {
    private final ReservationRepository reservations;
    private final MaintenanceRepository maintenances;
    private final Duration margin;
    public ConflictContextFactory(ReservationRepository reservations, MaintenanceRepository maintenances,
        @Value("${chargeslot.operational-margin-minutes:0}") long minutes) {
        if (minutes < 0 || minutes > 60) throw new IllegalArgumentException("Marge attendue entre 0 et 60 minutes.");
        this.reservations = reservations; this.maintenances = maintenances; this.margin = Duration.ofMinutes(minutes);
    }
    public ConflictContext create(Connector connector, Vehicle vehicle, Instant start, Instant end, BigDecimal power) {
        Long stationId = connector.getStation().getId();
        return new ConflictContext(connector.getStation(), connector, vehicle, start, end, power,
            reservations.findOverlapping(stationId, start.minus(margin), end.plus(margin)),
            maintenances.findOverlapping(stationId, start, end), margin);
    }
}
