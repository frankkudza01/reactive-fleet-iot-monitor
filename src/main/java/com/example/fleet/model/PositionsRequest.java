package com.example.fleet.model;

/**
 * Optional request used by RSocket clients to control sampling.
 */
public record PositionsRequest(
        long sampleMs // e.g. 250ms for smoother map animations
) { }
