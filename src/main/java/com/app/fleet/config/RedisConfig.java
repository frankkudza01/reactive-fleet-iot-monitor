package com.example.fleet.config;

import com.example.fleet.model.TruckTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Reactive Redis template with JSON serialization for TruckTelemetry.
 */
@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, TruckTelemetry> telemetryRedisTemplate(ReactiveRedisConnectionFactory factory) {
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new Jackson2JsonRedisSerializer<>(TruckTelemetry.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, TruckTelemetry> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        var context = builder.value(valueSerializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, String> stringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        var context = RedisSerializationContext.<String, String>newSerializationContext(new StringRedisSerializer())
                .value(new StringRedisSerializer())
                .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
