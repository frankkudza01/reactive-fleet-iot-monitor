package com.example.fleet.controller;

import com.example.fleet.model.Geofence;
import com.example.fleet.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/geofences")
@Tag(name = "Geofences")
public class GeofenceController {

    private final GeofenceService service;

    public GeofenceController(GeofenceService service) {
        this.service = service;
    }

    @Operation(summary = "Create a geofence polygon")
    @PostMapping
    public Flux<Geofence> create(@RequestBody @Valid CreateGeofenceRequest req) {
        var g = new Geofence(req.geofenceId(), req.name(), req.polygon());
        return service.add(g);
    }

    @Operation(summary = "List geofences")
    @GetMapping
    public Flux<Geofence> list() {
        return service.list();
    }

    @Operation(summary = "Delete a geofence")
    @DeleteMapping("/{geofenceId}")
    public Flux<Void> delete(@PathVariable String geofenceId) {
        return service.delete(geofenceId);
    }

    public record CreateGeofenceRequest(
            @NotBlank String geofenceId,
            @NotBlank String name,
            @NotNull List<com.example.fleet.model.GeoPoint> polygon
    ) { }
}
