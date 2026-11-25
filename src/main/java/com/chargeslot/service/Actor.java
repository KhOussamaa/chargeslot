package com.chargeslot.service;

import com.chargeslot.config.SecurityConfig.DemoUser;
import com.chargeslot.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;

public record Actor(String username, Long driverId, boolean operator) {
    public static Actor from(Authentication auth) {
        DemoUser user = (DemoUser) auth.getPrincipal();
        return new Actor(user.getUsername(), user.getDriverId(),
            user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")));
    }
    public void requireOperator() {
        if (!operator) throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Cette action est reservee a l'operateur.");
    }
    public void requireOwner(Long ownerId) {
        if (!operator && !driverId.equals(ownerId))
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Ce vehicule ou cette reservation appartient a un autre conducteur.");
    }
}
