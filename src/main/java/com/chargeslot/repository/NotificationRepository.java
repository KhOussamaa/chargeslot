package com.chargeslot.repository;
import com.chargeslot.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByDriverIdOrderByIdDesc(Long driverId);
    List<Notification> findAllByOrderByIdDesc();
}
