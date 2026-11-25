package com.chargeslot.service;

import org.slf4j.*;
import org.springframework.stereotype.Component;

@Component
public class LocalNotificationGateway implements NotificationGateway {
    private static final Logger log = LoggerFactory.getLogger(LocalNotificationGateway.class);
    public void send(Long driverId, String message) { log.info("Notification locale - conducteur {} : {}", driverId, message); }
}
