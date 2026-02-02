package com.example.fleet.service;

import com.example.fleet.config.FleetProperties;
import com.example.fleet.model.TruckTelemetry;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Central hot stream of telemetry for WebSocket/RSocket consumers.
 *
 * Backpressure strategy is configurable. For "latest", slow consumers will not
 * accumulate memory; they'll see the newest update when they can.
 */
@Service
public class TelemetryStreamService {

    private final Sinks.Many<TruckTelemetry> sink;
    private final AtomicLong dropped = new AtomicLong();

    public TelemetryStreamService(FleetProperties props) {
        String mode = props.backpressure().mode() == null ? "latest" : props.backpressure().mode().toLowerCase();
        int bufferSize = Math.max(100, props.backpressure().bufferSize());

        this.sink = switch (mode) {
            case "buffer" -> Sinks.many().multicast().onBackpressureBuffer(bufferSize, false);
            case "drop" -> Sinks.many().multicast().directBestEffort();
            default -> Sinks.many().multicast().directBestEffort(); // "latest" behavior implemented at subscriber level
        };
    }

    public void emit(TruckTelemetry telemetry) {
        var result = sink.tryEmitNext(telemetry);
        if (result.isFailure()) {
            dropped.incrementAndGet();
        }
    }

    public Flux<TruckTelemetry> stream() {
        // Provide a baseline safety net: if a subscriber can't keep up, keep only latest.
        return sink.asFlux().onBackpressureLatest();
    }

    public long droppedCount() {
        return dropped.get();
    }
}
