package com.example.fleet.model;

/**
 * Simple latitude/longitude point.
 * In production, consider using a geospatial library and proper CRS handling.
 */
public record GeoPoint(double lat, double lon) { }
