package com.chargeslot.service;

import com.chargeslot.domain.*;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.exception.ApiException;
import com.chargeslot.repository.*;
import com.chargeslot.service.conflict.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Service
public class ReservationService {
    private final StationRepository stations;
    private final ConnectorRepository connectors;
    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;
    private final ConflictContextFactory contexts;
    private final ConflictEngine conflicts;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    public ReservationService(StationRepository stations, ConnectorRepository connectors, VehicleRepository vehicles,
        ReservationRepository reservations, ConflictContextFactory contexts, ConflictEngine conflicts,
        ApplicationEventPublisher events, Clock clock) {
        this.stations = stations; this.connectors = connectors; this.vehicles = vehicles; this.reservations = reservations;
        this.contexts = contexts; this.conflicts = conflicts; this.events = events; this.clock = clock;
    }
    @Transactional
    public ReservationView create(ReservationRequest request, Actor actor) {
        Intervals.validate(request.startAt(), request.endAt(), clock);
        actor.requireOwner(request.driverId());
        Vehicle vehicle = vehicles.findById(request.vehicleId()).orElseThrow(() -> ApiException.notFound("Vehicule"));
        if (!vehicle.getDriverId().equals(request.driverId())) throw ApiException.badRequest("Le vehicule ne correspond pas au conducteur indique.");
        Long stationId = connectors.stationIdForConnector(request.connectorId()).orElseThrow(() -> ApiException.notFound("Connecteur"));
        // Toute ecriture concernant une station prend le meme verrou avant de lire les reservations.
        stations.lockById(stationId).orElseThrow(() -> ApiException.notFound("Station"));
        Connector connector = connectors.findById(request.connectorId()).orElseThrow(() -> ApiException.notFound("Connecteur"));
        conflicts.requireNoConflicts(contexts.create(connector, vehicle, request.startAt(), request.endAt(), request.requestedPowerKw()));
        Reservation reservation = reservations.saveAndFlush(new Reservation(connector, vehicle, request.startAt(), request.endAt(), request.requestedPowerKw()));
        events.publishEvent(new ReservationEvent(reservation.getId(), request.driverId(), "CONFIRMED"));
        return ReservationView.of(reservation);
    }
    @Transactional
    public ReservationView cancel(Long id, Actor actor) {
        Long stationId = reservations.stationIdForReservation(id).orElseThrow(() -> ApiException.notFound("Reservation"));
        stations.lockById(stationId).orElseThrow(() -> ApiException.notFound("Station"));
        Reservation reservation = reservations.findById(id).orElseThrow(() -> ApiException.notFound("Reservation"));
        actor.requireOwner(reservation.getVehicle().getDriverId());
        if (reservation.getStatus() == ReservationStatus.CANCELLED) return ReservationView.of(reservation);
        if (!reservation.getStartAt().isAfter(clock.instant())) throw new ApiException(HttpStatus.CONFLICT, "ALREADY_STARTED", "Impossible d'annuler une reservation deja commencee.");
        reservation.cancel();
        events.publishEvent(new ReservationEvent(id, reservation.getVehicle().getDriverId(), "CANCELLED"));
        return ReservationView.of(reservation);
    }
}
