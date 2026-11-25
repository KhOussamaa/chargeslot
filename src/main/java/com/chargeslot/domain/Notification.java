package com.chargeslot.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long reservationId;
    @Column(nullable = false) private Long driverId;
    @Column(nullable = false) private String eventType;
    @Column(nullable = false) private String status;
    @Column(nullable = false, length = 500) private String message;
    @Column(nullable = false) private Instant createdAt;
    protected Notification() { }
    public Notification(Long reservationId, Long driverId, String type, String status, String message, Instant at) {
        this.reservationId = reservationId; this.driverId = driverId; this.eventType = type;
        this.status = status; this.message = message; this.createdAt = at;
    }
    public Long getId() { return id; }
    public Long getReservationId() { return reservationId; }
    public Long getDriverId() { return driverId; }
    public String getEventType() { return eventType; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}
