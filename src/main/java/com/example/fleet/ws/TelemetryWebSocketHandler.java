package com.example.fleet.ws;

import com.example.fleet.service.TelemetryStreamService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
public class TelemetryWebSocketHandler implements WebSocketHandler {

    private final TelemetryStreamService telemetry;

    public TelemetryWebSocketHandler(TelemetryStreamService telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Send a continuous stream of telemetry to the client.
        // Avoid buffering: if the client is slow, keep only the latest telemetry events.
        var outbound = telemetry.stream()
                .map(t -> session.textMessage(session.bufferFactory().wrap(toJsonBytes(t))))
                .onBackpressureLatest();

        return session.send(outbound);
    }

    private byte[] toJsonBytes(Object o) {
        // Jackson's ObjectMapper used by Spring is typically shared; for a demo, we use a lightweight approach.
        // In production, inject and reuse ObjectMapper.
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(o);
        } catch (Exception e) {
            return ("{}").getBytes();
        }
    }
}
