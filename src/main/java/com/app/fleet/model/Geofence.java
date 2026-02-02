package com.example.fleet.model;

import java.util.List;

/**
 * Geofence polygon zone.
 * Polygon points should be ordered (clockwise/counter-clockwise) and closed implicitly.
 */
public record Geofence(
        String geofenceId,
        String name,
        List<GeoPoint> polygon
) { }
