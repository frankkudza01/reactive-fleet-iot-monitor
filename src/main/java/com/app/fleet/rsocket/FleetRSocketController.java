package com.example.fleet.rsocket;

import com.example.fleet.model.AlertEvent;
import com.example.fleet.model.PositionsRequest;
import com.example.fleet.model.TruckTelemetry;
import com.example.fleet.service.AlertStreamService;
import com.example.fleet.service.TelemetryStreamService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * RSocket routes for bidirectional streaming.
 *
 * Example client route:
 * - route: fleet.positions
 * - data: { "sampleMs": 250 }
 */
@Controller
public class FleetRSocketController {

    private final TelemetryStreamService telemetry;
    private final AlertStreamService alerts;

    public FleetRSocketController(TelemetryStreamService telemetry, AlertStreamService alerts) {
        this.telemetry = telemetry;
        this.alerts = alerts;
    }

    @MessageMapping("fleet.positions")
    public Flux<TruckTelemetry> positions(PositionsRequest req) {
        long sampleMs = req == null ? 0 : req.sampleMs();
        Flux<TruckTelemetry> f = telemetry.stream();

        // Sampling can reduce client CPU and network usage while keeping smooth animations.
        if (sampleMs > 0) {
            f = f.sample(Duration.ofMillis(Math.max(50, sampleMs)));
        }

        // Backpressure safety: latest events only if consumer is slow.
        return f.onBackpressureLatest();
    }

    @MessageMapping("fleet.alerts")
    public Flux<AlertEvent> alerts() {
        return alerts.stream().onBackpressureLatest();
    }
}
