package com.chargeslot.service.conflict;

import com.chargeslot.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record ConflictContext(ChargingStation station, Connector connector, Vehicle vehicle,
    Instant startAt, Instant endAt, BigDecimal requestedPowerKw, List<Reservation> reservations,
    List<MaintenanceWindow> maintenances, Duration margin) { }
