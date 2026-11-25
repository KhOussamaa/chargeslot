package com.chargeslot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long driverId;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConnectorType connectorType;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal maximumAcceptedPowerKw;
    protected Vehicle() { }
    public Vehicle(Long driverId, String name, ConnectorType type, BigDecimal power) {
        this.driverId = driverId; this.name = name; this.connectorType = type; this.maximumAcceptedPowerKw = power;
    }
    public Long getId() { return id; }
    public Long getDriverId() { return driverId; }
    public String getName() { return name; }
    public ConnectorType getConnectorType() { return connectorType; }
    public BigDecimal getMaximumAcceptedPowerKw() { return maximumAcceptedPowerKw; }
}
