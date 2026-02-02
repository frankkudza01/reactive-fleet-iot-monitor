package com.example.fleet.ws;

import com.example.fleet.service.TelemetryStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * WebSocket stream for browsers.
 *
 * Optimizations:
 * - Reuse Spring-managed ObjectMapper (no per-message allocation)
 * - Optional sampling via query param: /ws/telemetry?sampleMs=250
 * - Backpressure safety: keep only latest when client is slow
 */
@Component
public class TelemetryWebSocketHandler implements WebSocketHandler {

    private final TelemetryStreamService telemetry;
    private final ObjectMapper mapper;

    public TelemetryWebSocketHandler(TelemetryStreamService telemetry, ObjectMapper mapper) {
        this.telemetry = telemetry;
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        long sampleMs = parseSampleMs(session.getHandshakeInfo().getUri()).orElse(0L);

        var stream = telemetry.stream();
        if (sampleMs > 0) {
            stream = stream.sample(Duration.ofMillis(Math.max(50, sampleMs)));
        }

        var outbound = stream
                .map(t -> session.textMessage(toJson(t)))
                .onBackpressureLatest();

        return session.send(outbound);
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Optional<Long> parseSampleMs(URI uri) {
        if (uri == null || uri.getQuery() == null) return Optional.empty();
        for (String part : uri.getQuery().split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equalsIgnoreCase("sampleMs")) {
                try {
                    return Optional.of(Long.parseLong(kv[1]));
                } catch (Exception ignored) {}
            }
        }
        return Optional.empty();
    }
}
