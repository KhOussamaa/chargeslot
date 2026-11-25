package com.chargeslot.repository;
import com.chargeslot.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByDriverIdOrderById(Long driverId);
}
