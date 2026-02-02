package com.example.fleet.model;

import java.time.Instant;

/**
 * Telemetry payload streamed to clients and cached in Redis.
 */
public record TruckTelemetry(
        String truckId,
        Instant ts,
        double lat,
        double lon,
        double speedKph,
        double fuelPct,
        double headingDeg
) { }
