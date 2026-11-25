package com.chargeslot.controller;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
@RestController @RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservations;
    private final CatalogService catalog;
    public ReservationController(ReservationService reservations,CatalogService catalog) { this.reservations = reservations; this.catalog = catalog; }
    @PostMapping public ResponseEntity<ReservationView> create(@Valid @RequestBody ReservationRequest request,Authentication auth) {
        ReservationView result = reservations.create(request,Actor.from(auth));
        return ResponseEntity.created(URI.create("/api/reservations/"+result.id())).body(result);
    }
    @GetMapping public List<ReservationView> list(Authentication auth) { return catalog.reservations(Actor.from(auth)); }
    @GetMapping("/{id}") public ReservationView get(@PathVariable Long id,Authentication auth) { return catalog.reservation(id,Actor.from(auth)); }
}
