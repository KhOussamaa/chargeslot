package com.chargeslot.controller;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;
@RestController @RequestMapping("/api")
public class InfrastructureController {
    private final InfrastructureService infrastructure;
    private final CatalogService catalog;
    public InfrastructureController(InfrastructureService infrastructure,CatalogService catalog) { this.infrastructure = infrastructure; this.catalog = catalog; }
    @PostMapping("/stations") public ResponseEntity<StationView> createStation(@Valid @RequestBody StationRequest request,Authentication auth) {
        StationView result = infrastructure.createStation(request,Actor.from(auth)); return ResponseEntity.created(URI.create("/api/stations/"+result.id())).body(result);
    }
    @PatchMapping("/stations/{id}") public StationView updateStation(@PathVariable Long id,@Valid @RequestBody StationRequest request,Authentication auth) { return infrastructure.updateStation(id,request,Actor.from(auth)); }
    @PostMapping("/stations/{stationId}/connectors") public ResponseEntity<ConnectorView> createConnector(@PathVariable Long stationId,@Valid @RequestBody ConnectorRequest request,Authentication auth) {
        ConnectorView result = infrastructure.createConnector(stationId,request,Actor.from(auth)); return ResponseEntity.created(URI.create("/api/connectors/"+result.id())).body(result);
    }
    @PatchMapping("/connectors/{id}") public ConnectorView updateConnector(@PathVariable Long id,@Valid @RequestBody ConnectorRequest request,Authentication auth) { return infrastructure.updateConnector(id,request,Actor.from(auth)); }
    @GetMapping("/maintenances") public List<MaintenanceView> maintenances(Authentication auth) { return catalog.maintenances(Actor.from(auth)); }
    @GetMapping("/maintenances/{id}") public MaintenanceView maintenance(@PathVariable Long id,Authentication auth) { return catalog.maintenance(id,Actor.from(auth)); }
    @PostMapping("/maintenances") public ResponseEntity<MaintenanceView> createMaintenance(@Valid @RequestBody MaintenanceRequest request,Authentication auth) {
        MaintenanceView result = infrastructure.createMaintenance(request,Actor.from(auth)); return ResponseEntity.created(URI.create("/api/maintenances/"+result.id())).body(result);
    }
    @DeleteMapping("/maintenances/{id}") public ResponseEntity<Void> deleteMaintenance(@PathVariable Long id,Authentication auth) { infrastructure.deleteMaintenance(id,Actor.from(auth)); return ResponseEntity.noContent().build(); }
}
