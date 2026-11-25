package com.chargeslot.dto;

import com.chargeslot.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiModels {
    private ApiModels() { }
    public record ReservationRequest(@NotNull @Positive Long driverId, @NotNull @Positive Long vehicleId,
        @NotNull @Positive Long connectorId, @NotNull Instant startAt, @NotNull Instant endAt,
        @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal requestedPowerKw) { }
    public record StationRequest(@NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal maximumPowerKw,
        @NotNull Boolean active) { }
    public record ConnectorRequest(@NotBlank @Size(max = 80) String name, @NotNull ConnectorType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 7, fraction = 2) BigDecimal maximumPowerKw,
        @NotNull Boolean active) { }
    public record MaintenanceRequest(@NotNull @Positive Long stationId, @Positive Long connectorId,
        @NotNull Instant startAt, @NotNull Instant endAt, @NotBlank @Size(max = 240) String reason) { }
    public record ConnectorView(Long id, Long stationId, String name, ConnectorType type, BigDecimal maximumPowerKw, boolean active) {
        public static ConnectorView of(Connector c) { return new ConnectorView(c.getId(), c.getStation().getId(), c.getName(), c.getType(), c.getMaximumPowerKw(), c.isActive()); }
    }
    public record StationView(Long id, String name, BigDecimal maximumPowerKw, boolean active, List<ConnectorView> connectors) { }
    public record VehicleView(Long id, Long driverId, String name, ConnectorType connectorType, BigDecimal maximumAcceptedPowerKw) {
        public static VehicleView of(Vehicle v) { return new VehicleView(v.getId(), v.getDriverId(), v.getName(), v.getConnectorType(), v.getMaximumAcceptedPowerKw()); }
    }
    public record ReservationView(Long id, Long driverId, Long vehicleId, Long stationId, Long connectorId,
        Instant startAt, Instant endAt, BigDecimal allocatedPowerKw, ReservationStatus status) {
        public static ReservationView of(Reservation r) { return new ReservationView(r.getId(), r.getVehicle().getDriverId(), r.getVehicle().getId(), r.getConnector().getStation().getId(), r.getConnector().getId(), r.getStartAt(), r.getEndAt(), r.getRequestedPowerKw(), r.getStatus()); }
    }
    public record MaintenanceView(Long id, Long stationId, Long connectorId, Instant startAt, Instant endAt, String reason) {
        public static MaintenanceView of(MaintenanceWindow m) { return new MaintenanceView(m.getId(), m.getStation().getId(), m.getConnector() == null ? null : m.getConnector().getId(), m.getStartAt(), m.getEndAt(), m.getReason()); }
    }
    public record NotificationView(Long id, Long reservationId, Long driverId, String eventType, String status, String message, Instant createdAt) {
        public static NotificationView of(Notification n) { return new NotificationView(n.getId(), n.getReservationId(), n.getDriverId(), n.getEventType(), n.getStatus(), n.getMessage(), n.getCreatedAt()); }
    }
    public record AvailabilityView(Long connectorId, Long stationId, String stationName, String connectorName,
        ConnectorType type, BigDecimal availablePowerKw) { }
}
