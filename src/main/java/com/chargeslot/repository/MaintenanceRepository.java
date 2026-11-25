package com.chargeslot.repository;

import com.chargeslot.domain.MaintenanceWindow;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceWindow, Long> {
    @Query("select m from MaintenanceWindow m where m.station.id = :stationId and m.startAt < :endAt and m.endAt > :startAt")
    List<MaintenanceWindow> findOverlapping(@Param("stationId") Long stationId, @Param("startAt") Instant startAt, @Param("endAt") Instant endAt);
    List<MaintenanceWindow> findAllByOrderByStartAt();
}
