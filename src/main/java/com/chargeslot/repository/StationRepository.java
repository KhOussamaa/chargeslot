package com.chargeslot.repository;

import com.chargeslot.domain.ChargingStation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface StationRepository extends JpaRepository<ChargingStation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChargingStation s where s.id = :id")
    Optional<ChargingStation> lockById(@Param("id") Long id);
}
