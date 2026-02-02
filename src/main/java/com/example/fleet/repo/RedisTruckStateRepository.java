package com.example.fleet.repo;

import com.example.fleet.model.TruckTelemetry;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class RedisTruckStateRepository implements TruckStateRepository {

    private static final String KEY_PREFIX = "truck:";
    private static final String IDS_KEY = "truck:ids";

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
        return telemetryTemplate.opsForValue().set(key, telemetry)
                .then(stringTemplate.opsForSet().add(IDS_KEY, telemetry.truckId()).then())
                .then();
    }

    @Override
    public Mono<TruckTelemetry> get(String truckId) {
        return telemetryTemplate.opsForValue().get(KEY_PREFIX + truckId);
    }

    @Override
    public Flux<TruckTelemetry> list(int offset, int limit) {
        // Redis sets are unordered; for dashboards you usually page by a stable list or query by geo index.
        // For this demo, we take a slice from the set membership stream.
        return stringTemplate.opsForSet().members(IDS_KEY)
                .skip(offset)
                .take(limit)
                .flatMap(this::get);
    }

    @Override
    public Mono<Long> count() {
        return stringTemplate.opsForSet().size(IDS_KEY);
    }

    @Override
    public Mono<Void> clearAll() {
        // Delete set of ids and rely on key expiry in production; for demo we delete ids only.
        return stringTemplate.delete(IDS_KEY).then();
    }
}
