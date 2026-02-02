package com.example.fleet.service;

import com.example.fleet.config.FleetProperties;
import com.example.fleet.model.TruckTelemetry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central hot stream of telemetry for WebSocket/RSocket consumers.
 *
 * Optimization: "latest-per-truck aggregation"
 * - Simulator may update 1000–5000 trucks per tick.
 * - Instead of broadcasting EVERY update immediately (high fan-out pressure),
 *   we keep only the latest telemetry per truck in a map and periodically flush
 *   a snapshot to consumers.
 *
 * This keeps memory stable and gives smooth map updates.
 */
@Service
public class TelemetryStreamService {

    private final Sinks.Many<TruckTelemetry> sink;
    private final ConcurrentHashMap<String, TruckTelemetry> latestByTruck = new ConcurrentHashMap<>();
    private final AtomicLong dropped = new AtomicLong();
    private final long flushMs;
    private final int emitBatchSize;

    private volatile Disposable flusher;

    public TelemetryStreamService(FleetProperties props) {
        this.flushMs = Math.max(50, props.stream().flushMs());
        this.emitBatchSize = Math.max(100, props.stream().emitBatchSize());

        // Sink strategy based on configured backpressure mode.
        String mode = props.backpressure().mode() == null ? "latest" : props.backpressure().mode().toLowerCase();
        int bufferSize = Math.max(100, props.backpressure().bufferSize());

        this.sink = switch (mode) {
            case "buffer" -> Sinks.many().multicast().onBackpressureBuffer(bufferSize, false);
            case "drop" -> Sinks.many().multicast().directBestEffort();
            default -> Sinks.many().multicast().directBestEffort(); // "latest": subscribers use onBackpressureLatest()
        };
    }

    /**
     * Accept telemetry updates from the simulator.
     * This is O(1) and never blocks.
     */
    public void accept(TruckTelemetry telemetry) {
        latestByTruck.put(telemetry.truckId(), telemetry);
    }

    /**
     * Subscribe to a hot stream of telemetry.
     * If a consumer is slow, it will keep only the latest events.
     */
    public Flux<TruckTelemetry> stream() {
        return sink.asFlux().onBackpressureLatest();
    }

    @PostConstruct
    void startFlusher() {
        this.flusher = Flux.interval(Duration.ofMillis(flushMs))
                .doOnNext(tick -> flushOnce())
                .onErrorContinue((e, o) -> {})
                .subscribe();
    }

    @PreDestroy
    void stopFlusher() {
        if (flusher != null && !flusher.isDisposed()) {
            flusher.dispose();
        }
    }

    private void flushOnce() {
        if (latestByTruck.isEmpty()) return;

        // Snapshot keys (lock-free) and emit up to emitBatchSize events.
        String[] keys = latestByTruck.keySet().toArray(String[]::new);
        int cap = Math.min(keys.length, emitBatchSize);

        for (int i = 0; i < cap; i++) {
            TruckTelemetry t = latestByTruck.remove(keys[i]);
            if (t == null) continue;
            var result = sink.tryEmitNext(t);
            if (result.isFailure()) {
                dropped.incrementAndGet();
                // Reinsert so it can be retried on next flush.
                latestByTruck.put(t.truckId(), t);
            }
        }
        // If keys.length > cap, remaining entries stay in the map for future flushes.
    }

    public long droppedCount() {
        return dropped.get();
    }
}
