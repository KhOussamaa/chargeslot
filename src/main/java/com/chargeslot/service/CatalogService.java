package com.chargeslot.service;

import com.chargeslot.domain.*;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.exception.ApiException;
import com.chargeslot.repository.*;
import com.chargeslot.service.conflict.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly = true)
public class CatalogService {
    private final StationRepository stations;
    private final ConnectorRepository connectors;
    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final MaintenanceRepository maintenances;
    private final NotificationRepository notifications;
    private final ConflictContextFactory contexts;
    private final ConflictEngine conflicts;
    private final PowerCapacityPolicy power;
    private final Clock clock;
    public CatalogService(StationRepository stations, ConnectorRepository connectors, VehicleRepository vehicles,
        ReservationRepository reservations, MaintenanceRepository maintenances, NotificationRepository notifications,
        ConflictContextFactory contexts, ConflictEngine conflicts, PowerCapacityPolicy power, Clock clock) {
        this.stations = stations; this.connectors = connectors; this.vehicles = vehicles; this.reservations = reservations;
        this.maintenances = maintenances; this.notifications = notifications; this.contexts = contexts;
        this.conflicts = conflicts; this.power = power; this.clock = clock;
    }
    public List<StationView> stations() {
        return stations.findAll().stream().sorted(Comparator.comparing(ChargingStation::getId)).map(s ->
            new StationView(s.getId(), s.getName(), s.getMaximumPowerKw(), s.isActive(), connectors.findByStationIdOrderById(s.getId()).stream().map(ConnectorView::of).toList())).toList();
    }
    public List<VehicleView> vehicles(Actor actor) {
        return (actor.operator() ? vehicles.findAll() : vehicles.findByDriverIdOrderById(actor.driverId())).stream().map(VehicleView::of).toList();
    }
    public StationView station(Long id) {
        ChargingStation s = stations.findById(id).orElseThrow(() -> ApiException.notFound("Station"));
        return new StationView(s.getId(),s.getName(),s.getMaximumPowerKw(),s.isActive(),connectors.findByStationIdOrderById(id).stream().map(ConnectorView::of).toList());
    }
    public ConnectorView connector(Long id) { return ConnectorView.of(connectors.findById(id).orElseThrow(() -> ApiException.notFound("Connecteur"))); }
    public ReservationView reservation(Long id, Actor actor) {
        Reservation r = reservations.findById(id).orElseThrow(() -> ApiException.notFound("Reservation"));
        actor.requireOwner(r.getVehicle().getDriverId()); return ReservationView.of(r);
    }
    public MaintenanceView maintenance(Long id, Actor actor) {
        actor.requireOperator(); return MaintenanceView.of(maintenances.findById(id).orElseThrow(() -> ApiException.notFound("Maintenance")));
    }
    public List<ReservationView> reservations(Actor actor) {
        return (actor.operator() ? reservations.findAllByOrderByStartAtDesc() : reservations.findByVehicleDriverIdOrderByStartAtDesc(actor.driverId())).stream().map(ReservationView::of).toList();
    }
    public List<NotificationView> notifications(Actor actor) {
        return (actor.operator() ? notifications.findAllByOrderByIdDesc() : notifications.findByDriverIdOrderByIdDesc(actor.driverId())).stream().map(NotificationView::of).toList();
    }
    public List<MaintenanceView> maintenances(Actor actor) {
        actor.requireOperator(); return maintenances.findAllByOrderByStartAt().stream().map(MaintenanceView::of).toList();
    }
    public List<AvailabilityView> availability(Long stationId, ConnectorType type, Long vehicleId, Instant start, Instant end, BigDecimal requested, Actor actor) {
        Intervals.validate(start, end, clock);
        if (requested == null || requested.signum() <= 0 || requested.scale() > 2 || requested.precision() - requested.scale() > 7) throw ApiException.badRequest("Puissance positive, sept chiffres et deux decimales au maximum.");
        Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> ApiException.notFound("Vehicule"));
        actor.requireOwner(vehicle.getDriverId());
        if (stationId != null && !stations.existsById(stationId)) throw ApiException.notFound("Station");
        List<Connector> candidates = stationId == null ? connectors.findAll() : connectors.findByStationIdOrderById(stationId);
        List<AvailabilityView> result = new ArrayList<>();
        for (Connector connector : candidates) {
            if (type != null && connector.getType() != type) continue;
            ConflictContext context = contexts.create(connector, vehicle, start, end, requested);
            if (!conflicts.detect(context).isEmpty()) continue;
            BigDecimal available = power.available(context).min(connector.getMaximumPowerKw()).min(vehicle.getMaximumAcceptedPowerKw());
            result.add(new AvailabilityView(connector.getId(), connector.getStation().getId(), connector.getStation().getName(), connector.getName(), connector.getType(), available));
        }
        return result.stream().sorted(Comparator.comparing(AvailabilityView::connectorId)).toList();
    }
}
