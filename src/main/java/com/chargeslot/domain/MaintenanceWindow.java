package com.chargeslot.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class MaintenanceWindow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private ChargingStation station;
    @ManyToOne(fetch = FetchType.LAZY) private Connector connector;
    @Column(nullable = false) private Instant startAt;
    @Column(nullable = false) private Instant endAt;
    @Column(nullable = false, length = 240) private String reason;
    protected MaintenanceWindow() { }
    public MaintenanceWindow(ChargingStation station, Connector connector, Instant start, Instant end, String reason) {
        this.station = station; this.connector = connector; this.startAt = start; this.endAt = end; this.reason = reason;
    }
    public Long getId() { return id; }
    public ChargingStation getStation() { return station; }
    public Connector getConnector() { return connector; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getReason() { return reason; }
}
