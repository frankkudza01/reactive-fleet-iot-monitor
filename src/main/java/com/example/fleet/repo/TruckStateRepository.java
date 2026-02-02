package com.example.fleet.repo;

import com.example.fleet.model.TruckTelemetry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for live truck state.
 * Backed by Redis for fast reads and to decouple stream consumers.
 */
public interface TruckStateRepository {
    Mono<Void> upsert(TruckTelemetry telemetry);
    Mono<TruckTelemetry> get(String truckId);
    Flux<TruckTelemetry> list(int offset, int limit);
    Mono<Long> count();
    Mono<Void> clearAll();
}
