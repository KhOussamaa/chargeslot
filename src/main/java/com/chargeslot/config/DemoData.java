package com.chargeslot.config;

import com.chargeslot.domain.*;
import com.chargeslot.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component @ConditionalOnProperty(name = "chargeslot.demo-data", havingValue = "true")
public class DemoData implements CommandLineRunner {
    private final StationRepository stations;
    private final ConnectorRepository connectors;
    private final VehicleRepository vehicles;
    public DemoData(StationRepository stations, ConnectorRepository connectors, VehicleRepository vehicles) { this.stations = stations; this.connectors = connectors; this.vehicles = vehicles; }
    @Override @Transactional
    public void run(String... args) {
        if (stations.count() != 0 || vehicles.count() != 0) return;
        ChargingStation campus = stations.save(new ChargingStation("Campus", new BigDecimal("80")));
        ChargingStation centre = stations.save(new ChargingStation("Centre-ville", new BigDecimal("120")));
        connectors.save(new Connector(campus, "Campus CCS A", ConnectorType.CCS, new BigDecimal("60")));
        connectors.save(new Connector(campus, "Campus CCS B", ConnectorType.CCS, new BigDecimal("60")));
        connectors.save(new Connector(campus, "Campus Type 2", ConnectorType.TYPE2, new BigDecimal("22")));
        connectors.save(new Connector(centre, "Centre CCS", ConnectorType.CCS, new BigDecimal("120")));
        connectors.save(new Connector(centre, "Centre CHAdeMO", ConnectorType.CHADEMO, new BigDecimal("50")));
        vehicles.save(new Vehicle(17L, "Alice - voiture CCS", ConnectorType.CCS, new BigDecimal("50")));
        vehicles.save(new Vehicle(18L, "Bob - voiture CCS", ConnectorType.CCS, new BigDecimal("60")));
        vehicles.save(new Vehicle(17L, "Alice - voiture Type 2", ConnectorType.TYPE2, new BigDecimal("22")));
    }
}
