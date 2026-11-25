package com.chargeslot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_reservation_times", columnList = "startAt,endAt"))
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Connector connector;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Vehicle vehicle;
    @Column(nullable = false) private Instant startAt;
    @Column(nullable = false) private Instant endAt;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal requestedPowerKw;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReservationStatus status = ReservationStatus.CONFIRMED;
    protected Reservation() { }
    public Reservation(Connector connector, Vehicle vehicle, Instant start, Instant end, BigDecimal power) {
        this.connector = connector; this.vehicle = vehicle; this.startAt = start; this.endAt = end; this.requestedPowerKw = power;
    }
    public Long getId() { return id; }
    public Connector getConnector() { return connector; }
    public Vehicle getVehicle() { return vehicle; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public BigDecimal getRequestedPowerKw() { return requestedPowerKw; }
    public ReservationStatus getStatus() { return status; }
    public void cancel() { status = ReservationStatus.CANCELLED; }
}
