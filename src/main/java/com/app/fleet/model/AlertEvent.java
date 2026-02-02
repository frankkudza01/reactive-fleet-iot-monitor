package com.example.fleet.model;

import java.time.Instant;

/**
 * Alert emitted when a truck enters a geofence.
 */
public record AlertEvent(
        String alertId,
        Instant ts,
        String type,      // ENTER_GEOFENCE
        String truckId,
        String geofenceId,
        TruckTelemetry telemetry
) { }
