package com.example.fleet.util;

import com.example.fleet.model.GeoPoint;

import java.util.List;

/**
 * Pure CPU geospatial helper (non-blocking).
 * Implements ray-casting point-in-polygon test.
 */
public final class GeoUtils {

    private GeoUtils() {}

    public static boolean pointInPolygon(double lat, double lon, List<GeoPoint> polygon) {
        if (polygon == null || polygon.size() < 3) return false;

        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double yi = polygon.get(i).lat();
            double xi = polygon.get(i).lon();
            double yj = polygon.get(j).lat();
            double xj = polygon.get(j).lon();

            boolean intersect =
                    ((yi > lat) != (yj > lat)) &&
                            (lon < (xj - xi) * (lat - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }
}
