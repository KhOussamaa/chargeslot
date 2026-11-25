package com.chargeslot.service;
public record ReservationEvent(Long reservationId, Long driverId, String type) { }
