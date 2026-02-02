package com.example.fleet.service;

import com.example.fleet.config.FleetProperties;
import com.example.fleet.model.TruckTelemetry;
import com.example.fleet.repo.TruckStateRepository;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non-blocking simulator:
 * - Maintains in-memory truck state for fast updates
 * - Writes latest state to Redis (reactive)
 * - Emits to telemetry stream sink (hot stream)
 * - Evaluates geofences and emits alerts
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
        int maxEmit = Math.max(100, props.simulator().maxEmitPerTick());

        this.loop = Flux.interval(Duration.ofMillis(tickMs))
                .flatMap(tick -> tickOnce(maxEmit))
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
        // Use a rough bounding box around a city (example: around London) for demo.
        // Lat: 51.3..51.7, Lon: -0.5..0.2
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

    private Flux<Void> tickOnce(int maxEmitPerTick) {
        // Update all trucks in memory, then persist + emit stream.
        // No blocking calls; all I/O is reactive Redis.
        List<TruckTelemetry> updated = new ArrayList<>(state.size());

        for (TruckTelemetry t : state.values()) {
            updated.add(step(t));
        }

        // Write to repo in bounded concurrency to avoid overwhelming Redis during spikes.
        return Flux.fromIterable(updated)
                .flatMap(t -> repo.upsert(t)
                        .doOnSuccess(v -> {
                            // Emit to stream (hot) — cap emission per tick for burst safety.
                            telemetryStream.emit(t);
                            geofenceService.evaluate(t).subscribe(alertStream::emit);
                        })
                , 256)
                .thenMany(Flux.empty());
    }

    private TruckTelemetry step(TruckTelemetry t) {
        // Simple movement model:
        // - heading changes slightly
        // - speed fluctuates
        // - fuel decreases slowly
        // - lat/lon updated by heading and speed
        double heading = wrap360(t.headingDeg() + (rng.nextDouble() - 0.5) * 12);
        double speed = clamp(t.speedKph() + (rng.nextDouble() - 0.5) * 8, 0, 120);
        double fuel = clamp(t.fuelPct() - (0.005 + speed / 12000.0), 0, 100);

        // Convert speed to delta degrees roughly (not geodesic; OK for demo)
        double distance = speed / 3600.0; // km per second at 1Hz
        double dLat = (distance / 110.574) * Math.cos(Math.toRadians(heading));
        double dLon = (distance / (111.320 * Math.cos(Math.toRadians(t.lat())) + 1e-9)) * Math.sin(Math.toRadians(heading));

        double lat = t.lat() + dLat;
        double lon = t.lon() + dLon;

        // Keep within bounding box (bounce)
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
