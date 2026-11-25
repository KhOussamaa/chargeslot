package com.chargeslot.repository;
import com.chargeslot.domain.Connector;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ConnectorRepository extends JpaRepository<Connector, Long> {
    List<Connector> findByStationIdOrderById(Long stationId);
    @Query("select c.station.id from Connector c where c.id = :id")
    Optional<Long> stationIdForConnector(@Param("id") Long id);
}
