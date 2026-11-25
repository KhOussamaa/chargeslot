package com.chargeslot.controller;
import com.chargeslot.domain.ConnectorType;
import com.chargeslot.dto.ApiModels.AvailabilityView;
import com.chargeslot.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
@RestController @RequestMapping("/api/availability")
public class AvailabilityController {
    private final CatalogService catalog;
    public AvailabilityController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping public List<AvailabilityView> availability(@RequestParam(required = false) Long stationId,
        @RequestParam(required = false) ConnectorType connectorType,@RequestParam Long vehicleId,
        @RequestParam Instant startAt,@RequestParam Instant endAt,@RequestParam BigDecimal requestedPowerKw,Authentication auth) {
        return catalog.availability(stationId,connectorType,vehicleId,startAt,endAt,requestedPowerKw,Actor.from(auth));
    }
}
