package com.example.fleet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleet")
public record FleetProperties(
        Simulator simulator,
        Backpressure backpressure
) {
    public record Simulator(
            boolean autoStart,
            int initialTrucks,
            long tickMs,
            int maxEmitPerTick
    ) { }

    public record Backpressure(
            String mode,
            int bufferSize
    ) { }
}
