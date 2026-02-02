package com.example.fleet.controller;

import com.example.fleet.model.TruckTelemetry;
import com.example.fleet.repo.TruckStateRepository;
import com.example.fleet.service.TelemetryStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/fleet")
@Tag(name = "Fleet")
public class FleetController {

    private final TruckStateRepository repo;
    private final TelemetryStreamService stream;

    public FleetController(TruckStateRepository repo, TelemetryStreamService stream) {
        this.repo = repo;
        this.stream = stream;
    }

    @Operation(summary = "List live truck states from Redis (unordered set slice)")
    @GetMapping("/trucks")
    public Flux<TruckTelemetry> list(@RequestParam(defaultValue = "0") int offset,
                                    @RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.max(1, Math.min(2000, limit));
        return repo.list(Math.max(0, offset), safeLimit);
    }

    @Operation(summary = "Get live state for one truck")
    @GetMapping("/trucks/{truckId}")
    public Mono<TruckTelemetry> get(@PathVariable String truckId) {
        return repo.get(truckId);
    }

    @Operation(summary = "Count trucks currently cached")
    @GetMapping("/count")
    public Mono<Long> count() {
        return repo.count();
    }

    @Operation(summary = "Telemetry stream stats (dropped events under backpressure)")
    @GetMapping("/stream-stats")
    public Mono<StreamStats> stats() {
        return Mono.just(new StreamStats(stream.droppedCount()));
    }

    public record StreamStats(long droppedTelemetry) { }
}
