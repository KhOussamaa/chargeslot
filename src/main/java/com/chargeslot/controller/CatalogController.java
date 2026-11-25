package com.chargeslot.controller;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping("/stations") public List<StationView> stations() { return catalog.stations(); }
    @GetMapping("/stations/{id}") public StationView station(@PathVariable Long id) { return catalog.station(id); }
    @GetMapping("/connectors/{id}") public ConnectorView connector(@PathVariable Long id) { return catalog.connector(id); }
    @GetMapping("/vehicles") public List<VehicleView> vehicles(Authentication auth) { return catalog.vehicles(Actor.from(auth)); }
}
