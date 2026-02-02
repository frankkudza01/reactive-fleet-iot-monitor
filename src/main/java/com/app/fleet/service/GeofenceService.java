package com.example.fleet.service;

import com.example.fleet.model.AlertEvent;
import com.example.fleet.model.Geofence;
import com.example.fleet.model.GeoPoint;
import com.example.fleet.model.TruckTelemetry;
import com.example.fleet.util.GeoUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reactive geofencing service (CPU-only; non-blocking).
 *
 * Optimizations:
 * 1) Precompute a bounding box per geofence to skip expensive point-in-polygon tests
 * 2) Track inside sets per truck to detect enter/exit transitions
 */
@Service
public class GeofenceService {

    private record IndexedGeofence(Geofence geofence, double minLat, double maxLat, double minLon, double maxLon) {}

    private final Map<String, IndexedGeofence> geofences = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> insideByTruck = new ConcurrentHashMap<>();

    public Flux<Geofence> list() {
        return Flux.fromIterable(geofences.values()).map(IndexedGeofence::geofence);
    }

    public Flux<Geofence> add(Geofence geofence) {
        geofences.put(geofence.geofenceId(), index(geofence));
        return Flux.just(geofence);
    }

    public Flux<Void> delete(String geofenceId) {
        geofences.remove(geofenceId);
        insideByTruck.values().forEach(set -> set.remove(geofenceId));
        return Flux.empty();
    }

    public Flux<AlertEvent> evaluate(TruckTelemetry t) {
        if (geofences.isEmpty()) return Flux.empty();

        Set<String> inside = insideByTruck.computeIfAbsent(t.truckId(), k -> ConcurrentHashMap.newKeySet());
        List<AlertEvent> alerts = new ArrayList<>();

        for (IndexedGeofence ig : geofences.values()) {
            String gid = ig.geofence().geofenceId();

            if (!inBoundingBox(t.lat(), t.lon(), ig)) {
                inside.remove(gid);
                continue;
            }

            boolean in = GeoUtils.pointInPolygon(t.lat(), t.lon(), ig.geofence().polygon());
            boolean wasIn = inside.contains(gid);

            if (in && !wasIn) {
                inside.add(gid);
                alerts.add(new AlertEvent(
                        "alert-" + UUID.randomUUID(),
                        Instant.now(),
                        "ENTER_GEOFENCE",
                        t.truckId(),
                        gid,
                        t
                ));
            } else if (!in && wasIn) {
                inside.remove(gid);
            }
        }

        return Flux.fromIterable(alerts);
    }

    private IndexedGeofence index(Geofence g) {
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;

        for (GeoPoint p : g.polygon()) {
            minLat = Math.min(minLat, p.lat());
            maxLat = Math.max(maxLat, p.lat());
            minLon = Math.min(minLon, p.lon());
            maxLon = Math.max(maxLon, p.lon());
        }
        return new IndexedGeofence(g, minLat, maxLat, minLon, maxLon);
    }

    private boolean inBoundingBox(double lat, double lon, IndexedGeofence ig) {
        return lat >= ig.minLat && lat <= ig.maxLat && lon >= ig.minLon && lon <= ig.maxLon;
    }
}
