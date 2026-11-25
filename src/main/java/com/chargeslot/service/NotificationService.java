package com.chargeslot.service;

import com.chargeslot.domain.Notification;
import com.chargeslot.repository.NotificationRepository;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.event.*;
import java.time.Clock;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository repository;
    private final NotificationGateway gateway;
    private final Clock clock;
    public NotificationService(NotificationRepository repository, NotificationGateway gateway, Clock clock) {
        this.repository = repository; this.gateway = gateway; this.clock = clock;
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReservationEvent(ReservationEvent event) {
        String message = "Reservation " + event.reservationId() + " : " + event.type();
        String status = "SENT";
        try { gateway.send(event.driverId(), message); }
        catch (RuntimeException ex) {
            status = "FAILED";
            log.warn("Notification non livree pour la reservation {}. La reservation reste enregistree.", event.reservationId());
        }
        repository.save(new Notification(event.reservationId(), event.driverId(), event.type(), status, message, clock.instant()));
    }
}
