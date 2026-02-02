package com.example.fleet.controller;

import com.example.fleet.model.AlertEvent;
import com.example.fleet.service.AlertStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts")
public class AlertsController {

    private final AlertStreamService alerts;

    public AlertsController(AlertStreamService alerts) {
        this.alerts = alerts;
    }

    @Operation(summary = "Server-Sent Events stream of geofence alerts (browser friendly)")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AlertEvent> stream() {
        return alerts.stream();
    }
}
