package com.example.fleet.repo;

import com.example.fleet.model.TruckTelemetry;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Redis-backed live state store.
 *
 * Optimization: use a Sorted Set (ZSET) for stable paging:
 * - ZSET key stores truckIds with score = lastUpdateEpochMillis
 * - list() pages via reverseRange (most recently updated first)
 */
@Repository
public class RedisTruckStateRepository implements TruckStateRepository {

    private static final String KEY_PREFIX = "truck:";
    private static final String IDS_ZSET = "truck:ids:z";

    private final ReactiveRedisTemplate<String, TruckTelemetry> telemetryTemplate;
    private final ReactiveRedisTemplate<String, String> stringTemplate;

    public RedisTruckStateRepository(ReactiveRedisTemplate<String, TruckTelemetry> telemetryTemplate,
                                    ReactiveRedisTemplate<String, String> stringTemplate) {
        this.telemetryTemplate = telemetryTemplate;
        this.stringTemplate = stringTemplate;
    }

    @Override
    public Mono<Void> upsert(TruckTelemetry telemetry) {
        String key = KEY_PREFIX + telemetry.truckId();
        double score = (double) Instant.now().toEpochMilli();

        return telemetryTemplate.opsForValue().set(key, telemetry)
                .then(stringTemplate.opsForZSet().add(IDS_ZSET, telemetry.truckId(), score).then())
                .then();
    }

    @Override
    public Mono<TruckTelemetry> get(String truckId) {
        return telemetryTemplate.opsForValue().get(KEY_PREFIX + truckId);
    }

    @Override
    public Flux<TruckTelemetry> list(int offset, int limit) {
        long start = Math.max(0, offset);
        long end = start + Math.max(1, limit) - 1;

        return stringTemplate.opsForZSet().reverseRange(IDS_ZSET, start, end)
                .flatMap(this::get);
    }

    @Override
    public Mono<Long> count() {
        return stringTemplate.opsForZSet().zCard(IDS_ZSET);
    }

    @Override
    public Mono<Void> clearAll() {
        return stringTemplate.delete(IDS_ZSET).then();
    }
}
