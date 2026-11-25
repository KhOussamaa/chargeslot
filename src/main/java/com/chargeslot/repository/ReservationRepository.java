package com.chargeslot.repository;

import com.chargeslot.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("select r.connector.station.id from Reservation r where r.id = :id")
    Optional<Long> stationIdForReservation(@Param("id") Long id);
    @Query("select r from Reservation r where r.connector.station.id = :stationId and r.status = com.chargeslot.domain.ReservationStatus.CONFIRMED and r.startAt < :endAt and r.endAt > :startAt")
    List<Reservation> findOverlapping(@Param("stationId") Long stationId, @Param("startAt") Instant startAt, @Param("endAt") Instant endAt);
    List<Reservation> findByVehicleDriverIdOrderByStartAtDesc(Long driverId);
    List<Reservation> findAllByOrderByStartAtDesc();
    @Query("select r from Reservation r where r.connector.station.id = :stationId and r.status = com.chargeslot.domain.ReservationStatus.CONFIRMED and r.endAt > :now")
    List<Reservation> findUpcoming(@Param("stationId") Long stationId, @Param("now") Instant now);
}
