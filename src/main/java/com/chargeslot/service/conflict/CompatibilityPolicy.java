package com.chargeslot.service.conflict;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.*;

@Component @Order(2)
public class CompatibilityPolicy implements ConflictPolicy {
    public List<Conflict> check(ConflictContext c) {
        List<Conflict> result = new ArrayList<>();
        if (!c.station().isActive() || !c.connector().isActive()) result.add(new Conflict("INACTIVE", "Station ou connecteur desactive."));
        if (c.vehicle().getConnectorType() != c.connector().getType()) result.add(new Conflict("INCOMPATIBLE", "Le connecteur est incompatible avec le vehicule."));
        if (c.requestedPowerKw().compareTo(c.vehicle().getMaximumAcceptedPowerKw()) > 0) result.add(new Conflict("VEHICLE_POWER_LIMIT", "La puissance demandee depasse la limite du vehicule."));
        if (c.requestedPowerKw().compareTo(c.connector().getMaximumPowerKw()) > 0) result.add(new Conflict("CONNECTOR_POWER_LIMIT", "La puissance demandee depasse la limite du connecteur."));
        return result;
    }
}
