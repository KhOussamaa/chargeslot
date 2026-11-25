package com.chargeslot.service;

import com.chargeslot.domain.*;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.exception.ApiException;
import com.chargeslot.repository.*;
import com.chargeslot.service.conflict.PowerCapacityPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;

@Service @Transactional
public class InfrastructureService {
    private final StationRepository stations;
    private final ConnectorRepository connectors;
    private final ReservationRepository reservations;
    private final MaintenanceRepository maintenances;
    private final Clock clock;
    public InfrastructureService(StationRepository stations, ConnectorRepository connectors, ReservationRepository reservations, MaintenanceRepository maintenances, Clock clock) {
        this.stations = stations; this.connectors = connectors; this.reservations = reservations; this.maintenances = maintenances; this.clock = clock;
    }
    public StationView createStation(StationRequest request, Actor actor) {
        actor.requireOperator();
        ChargingStation station = new ChargingStation(request.name().strip(), request.maximumPowerKw());
        station.update(request.name().strip(), request.maximumPowerKw(), request.active());
        return stationView(stations.saveAndFlush(station));
    }
    public StationView updateStation(Long id, StationRequest request, Actor actor) {
        actor.requireOperator();
        ChargingStation station = lock(id);
        List<Reservation> upcoming = reservations.findUpcoming(id, clock.instant());
        if (PowerCapacityPolicy.peak(upcoming, clock.instant(), Instant.MAX).compareTo(request.maximumPowerKw()) > 0)
            throw conflict("CAPACITY_IN_USE", "La nouvelle puissance ne couvre pas les reservations confirmees.");
        station.update(request.name().strip(), request.maximumPowerKw(), request.active());
        return stationView(station);
    }
    public ConnectorView createConnector(Long stationId, ConnectorRequest request, Actor actor) {
        actor.requireOperator();
        Connector connector = new Connector(lock(stationId), request.name().strip(), request.type(), request.maximumPowerKw());
        connector.update(request.name().strip(), request.type(), request.maximumPowerKw(), request.active());
        return ConnectorView.of(connectors.saveAndFlush(connector));
    }
    public ConnectorView updateConnector(Long id, ConnectorRequest request, Actor actor) {
        actor.requireOperator();
        Long stationId = connectors.stationIdForConnector(id).orElseThrow(() -> ApiException.notFound("Connecteur"));
        lock(stationId);
        Connector connector = connectors.findById(id).orElseThrow(() -> ApiException.notFound("Connecteur"));
        boolean invalidates = reservations.findUpcoming(stationId, clock.instant()).stream().filter(r -> r.getConnector().getId().equals(id))
            .anyMatch(r -> connector.getType() != request.type() || r.getRequestedPowerKw().compareTo(request.maximumPowerKw()) > 0);
        if (invalidates) throw conflict("CONNECTOR_IN_USE", "Cette modification invaliderait une reservation confirmee.");
        connector.update(request.name().strip(), request.type(), request.maximumPowerKw(), request.active());
        return ConnectorView.of(connector);
    }
    public MaintenanceView createMaintenance(MaintenanceRequest request, Actor actor) {
        actor.requireOperator(); Intervals.validate(request.startAt(), request.endAt(), clock);
        ChargingStation station = lock(request.stationId());
        Connector connector = request.connectorId() == null ? null : connectors.findById(request.connectorId()).orElseThrow(() -> ApiException.notFound("Connecteur"));
        if (connector != null && !connector.getStation().getId().equals(station.getId())) throw ApiException.badRequest("Le connecteur n'appartient pas a cette station.");
        boolean blocksReservation = reservations.findOverlapping(station.getId(), request.startAt(), request.endAt()).stream()
            .anyMatch(r -> connector == null || r.getConnector().getId().equals(connector.getId()));
        if (blocksReservation) throw conflict("RESERVATION_EXISTS", "Une reservation confirmee existe : annulez-la avant de planifier la maintenance.");
        return MaintenanceView.of(maintenances.saveAndFlush(new MaintenanceWindow(station, connector, request.startAt(), request.endAt(), request.reason().strip())));
    }
    public void deleteMaintenance(Long id, Actor actor) {
        actor.requireOperator();
        MaintenanceWindow window = maintenances.findById(id).orElseThrow(() -> ApiException.notFound("Maintenance"));
        lock(window.getStation().getId()); maintenances.delete(window);
    }
    private ChargingStation lock(Long id) { return stations.lockById(id).orElseThrow(() -> ApiException.notFound("Station")); }
    private StationView stationView(ChargingStation s) { return new StationView(s.getId(), s.getName(), s.getMaximumPowerKw(), s.isActive(), connectors.findByStationIdOrderById(s.getId()).stream().map(ConnectorView::of).toList()); }
    private ApiException conflict(String code, String message) { return new ApiException(HttpStatus.CONFLICT, code, message); }
}
