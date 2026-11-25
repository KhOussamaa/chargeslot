package com.chargeslot.controller;
import com.chargeslot.dto.ApiModels.ReservationView;
import com.chargeslot.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reservations")
public class CancellationController {
    private final ReservationService reservations;
    public CancellationController(ReservationService reservations) { this.reservations = reservations; }
    @PostMapping("/{id}/cancel") public ReservationView cancel(@PathVariable Long id,Authentication auth) { return reservations.cancel(id,Actor.from(auth)); }
}
