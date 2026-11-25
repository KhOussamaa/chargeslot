package com.chargeslot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Connector {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private ChargingStation station;
    @Column(nullable = false, length = 80) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConnectorType type;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal maximumPowerKw;
    @Column(nullable = false) private boolean active = true;
    protected Connector() { }
    public Connector(ChargingStation station, String name, ConnectorType type, BigDecimal power) {
        this.station = station; this.name = name; this.type = type; this.maximumPowerKw = power;
    }
    public Long getId() { return id; }
    public ChargingStation getStation() { return station; }
    public String getName() { return name; }
    public ConnectorType getType() { return type; }
    public BigDecimal getMaximumPowerKw() { return maximumPowerKw; }
    public boolean isActive() { return active; }
    public void update(String name, ConnectorType type, BigDecimal power, boolean active) {
        this.name = name; this.type = type; this.maximumPowerKw = power; this.active = active;
    }
}
