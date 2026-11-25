package com.chargeslot.controller;
import com.chargeslot.dto.ApiModels.NotificationView;
import com.chargeslot.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final CatalogService catalog;
    public NotificationController(CatalogService catalog) { this.catalog = catalog; }
    @GetMapping public List<NotificationView> notifications(Authentication auth) { return catalog.notifications(Actor.from(auth)); }
}
