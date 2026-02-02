package com.example.fleet.service;

import com.example.fleet.model.AlertEvent;
import com.example.fleet.model.Geofence;
import com.example.fleet.model.TruckTelemetry;
import com.example.fleet.util.GeoUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive geofencing service. All work is CPU-only (non-blocking).
 *
 * For each truck, we track which geofences it is currently inside.
 * When inside->outside or outside->inside transitions occur, we emit events.
 */
@Service
public class GeofenceService {

    private final Map<String, Geofence> geofences = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> insideByTruck = new ConcurrentHashMap<>();

    public Flux<Geofence> list() {
        return Flux.fromIterable(geofences.values());
    }

    public Flux<Geofence> add(Geofence geofence) {
        geofences.put(geofence.geofenceId(), geofence);
        return Flux.just(geofence);
    }

    public Flux<Void> delete(String geofenceId) {
        geofences.remove(geofenceId);
        // Cleanup inside tracking
        insideByTruck.values().forEach(set -> set.remove(geofenceId));
        return Flux.empty();
    }

    public Flux<AlertEvent> evaluate(TruckTelemetry t) {
        if (geofences.isEmpty()) return Flux.empty();

        Set<String> inside = insideByTruck.computeIfAbsent(t.truckId(), k -> ConcurrentHashMap.newKeySet());
        List<AlertEvent> alerts = new ArrayList<>();

        for (Geofence g : geofences.values()) {
            boolean in = GeoUtils.pointInPolygon(t.lat(), t.lon(), g.polygon());
            boolean wasIn = inside.contains(g.geofenceId());
            if (in && !wasIn) {
                inside.add(g.geofenceId());
                alerts.add(new AlertEvent(
                        "alert-" + UUID.randomUUID(),
                        Instant.now(),
                        "ENTER_GEOFENCE",
                        t.truckId(),
                        g.geofenceId(),
                        t
                ));
            } else if (!in && wasIn) {
                inside.remove(g.geofenceId());
                // You can also emit EXIT events; kept minimal for the project brief.
            }
        }
        return Flux.fromIterable(alerts);
    }
}
