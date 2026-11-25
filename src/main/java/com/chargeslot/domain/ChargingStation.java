package com.chargeslot.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class ChargingStation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal maximumPowerKw;
    @Column(nullable = false) private boolean active = true;
    protected ChargingStation() { }
    public ChargingStation(String name, BigDecimal maximumPowerKw) { this.name = name; this.maximumPowerKw = maximumPowerKw; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getMaximumPowerKw() { return maximumPowerKw; }
    public boolean isActive() { return active; }
    public void update(String name, BigDecimal power, boolean active) { this.name = name; this.maximumPowerKw = power; this.active = active; }
}
