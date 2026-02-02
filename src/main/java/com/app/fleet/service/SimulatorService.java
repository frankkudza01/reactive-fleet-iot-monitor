package com.example.fleet.service;

import com.example.fleet.config.FleetProperties;
import com.example.fleet.model.TruckTelemetry;
import com.example.fleet.repo.TruckStateRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-blocking simulator:
 * - Maintains in-memory truck state for fast updates
 * - Writes latest state to Redis (reactive)
 * - Publishes to a hot telemetry stream service (aggregated downstream)
 * - Evaluates geofences and emits alerts (reactively composed; no nested subscribe)
 */
@Service
public class SimulatorService {

    private final FleetProperties props;
    private final TruckStateRepository repo;
    private final TelemetryStreamService telemetryStream;
    private final GeofenceService geofenceService;
    private final AlertStreamService alertStream;

    private final Map<String, TruckTelemetry> state = new ConcurrentHashMap<>();
    private final Random rng = new Random(7);

    private volatile Disposable loop;

    public SimulatorService(FleetProperties props,
                            TruckStateRepository repo,
                            TelemetryStreamService telemetryStream,
                            GeofenceService geofenceService,
                            AlertStreamService alertStream) {
        this.props = props;
        this.repo = repo;
        this.telemetryStream = telemetryStream;
        this.geofenceService = geofenceService;
        this.alertStream = alertStream;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (props.simulator().autoStart()) {
            start(props.simulator().initialTrucks());
        }
    }

    public synchronized void start(int count) {
        stop();
        seedTrucks(count);

        long tickMs = Math.max(50, props.simulator().tickMs());

        this.loop = Flux.interval(Duration.ofMillis(tickMs))
                .flatMap(tick -> tickOnce(), 1) // one tick at a time (keeps load predictable)
                .onErrorContinue((err, o) -> {}) // keep simulator alive
                .subscribe();
    }

    public synchronized void stop() {
        if (loop != null && !loop.isDisposed()) {
            loop.dispose();
        }
        loop = null;
    }

    public boolean isRunning() {
        return loop != null && !loop.isDisposed();
    }

    public int truckCount() {
        return state.size();
    }

    private void seedTrucks(int count) {
        state.clear();
        // Bounding box around a city for demo. Lat: 51.3..51.7, Lon: -0.5..0.2
        for (int i = 1; i <= count; i++) {
            String id = String.format("TRK-%05d", i);
            double lat = 51.3 + rng.nextDouble() * 0.4;
            double lon = -0.5 + rng.nextDouble() * 0.7;
            double speed = 10 + rng.nextDouble() * 60;
            double fuel = 20 + rng.nextDouble() * 80;
            double heading = rng.nextDouble() * 360;
            state.put(id, new TruckTelemetry(id, Instant.now(), lat, lon, speed, fuel, heading));
        }
    }

    private Mono<Void> tickOnce() {
        // Compute updated state in-memory (CPU only)
        List<TruckTelemetry> updated = new ArrayList<>(state.size());
        for (TruckTelemetry t : state.values()) {
            updated.add(step(t));
        }

        // Persist + stream + geofence checks, bounded concurrency so Redis doesn't get flooded.
        return Flux.fromIterable(updated)
                .flatMap(t ->
                        repo.upsert(t)
                                .then(Mono.fromRunnable(() -> telemetryStream.accept(t)))
                                .then(geofenceService.evaluate(t)
                                        .doOnNext(alertStream::emit)
                                        .then())
                , 256)
                .then();
    }

    private TruckTelemetry step(TruckTelemetry t) {
        double heading = wrap360(t.headingDeg() + (rng.nextDouble() - 0.5) * 12);
        double speed = clamp(t.speedKph() + (rng.nextDouble() - 0.5) * 8, 0, 120);
        double fuel = clamp(t.fuelPct() - (0.005 + speed / 12000.0), 0, 100);

        double distance = speed / 3600.0; // km per second at 1Hz
        double dLat = (distance / 110.574) * Math.cos(Math.toRadians(heading));
        double dLon = (distance / (111.320 * Math.cos(Math.toRadians(t.lat())) + 1e-9)) * Math.sin(Math.toRadians(heading));

        double lat = t.lat() + dLat;
        double lon = t.lon() + dLon;

        if (lat < 51.3 || lat > 51.7) lat = 51.3 + rng.nextDouble() * 0.4;
        if (lon < -0.5 || lon > 0.2) lon = -0.5 + rng.nextDouble() * 0.7;

        TruckTelemetry next = new TruckTelemetry(t.truckId(), Instant.now(), lat, lon, speed, fuel, heading);
        state.put(t.truckId(), next);
        return next;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double wrap360(double v) {
        double x = v % 360.0;
        return x < 0 ? x + 360.0 : x;
    }
}
