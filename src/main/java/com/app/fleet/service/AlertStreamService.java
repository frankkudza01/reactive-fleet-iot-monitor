package com.example.fleet.service;

import com.example.fleet.model.AlertEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Hot stream of alert events (geofence enters).
 */
@Service
public class AlertStreamService {

    private final Sinks.Many<AlertEvent> sink = Sinks.many().multicast().directBestEffort();
    private final AtomicLong dropped = new AtomicLong();

    public void emit(AlertEvent evt) {
        var r = sink.tryEmitNext(evt);
        if (r.isFailure()) dropped.incrementAndGet();
    }

    public Flux<AlertEvent> stream() {
        return sink.asFlux().onBackpressureLatest();
    }

    public long droppedCount() {
        return dropped.get();
    }
}
