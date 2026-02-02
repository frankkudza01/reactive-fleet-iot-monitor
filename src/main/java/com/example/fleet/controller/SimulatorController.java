package com.example.fleet.controller;

import com.example.fleet.service.SimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/simulator")
@Tag(name = "Simulator")
public class SimulatorController {

    private final SimulatorService simulator;

    public SimulatorController(SimulatorService simulator) {
        this.simulator = simulator;
    }

    @Operation(summary = "Start the simulator with N trucks (supports burst like 5000)")
    @PostMapping("/start")
    public Mono<Status> start(@RequestParam(defaultValue = "1000") int count) {
        simulator.start(count);
        return Mono.just(new Status(true, simulator.truckCount()));
    }

    @Operation(summary = "Stop the simulator")
    @PostMapping("/stop")
    public Mono<Status> stop() {
        simulator.stop();
        return Mono.just(new Status(false, simulator.truckCount()));
    }

    @Operation(summary = "Simulator status")
    @GetMapping("/status")
    public Mono<Status> status() {
        return Mono.just(new Status(simulator.isRunning(), simulator.truckCount()));
    }

    public record Status(boolean running, int trucks) { }
}
