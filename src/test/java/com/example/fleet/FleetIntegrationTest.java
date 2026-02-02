package com.example.fleet;

import com.example.fleet.controller.SimulatorController;
import com.example.fleet.model.TruckTelemetry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class FleetIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.redis.host", () -> redis.getHost());
        r.add("spring.redis.port", () -> redis.getMappedPort(6379));
        r.add("fleet.simulator.auto-start", () -> "false");
        r.add("fleet.simulator.tick-ms", () -> "100");
    }

    @Autowired
    WebTestClient web;

    @Test
    void startSimulator_thenRedisHasTruckStates() {
        // Start simulator with 200 trucks (kept modest for CI speed)
        web.post().uri("/api/simulator/start?count=200")
                .exchange()
                .expectStatus().isOk()
                .expectBody(SimulatorController.Status.class)
                .value(s -> assertTrue(s.running()));

        // Wait a short time for a few ticks, then assert we can read some states.
        Mono.delay(Duration.ofMillis(350)).block();

        web.get().uri("/api/fleet/trucks?limit=20")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TruckTelemetry.class)
                .value(list -> assertFalse(list.isEmpty()));
    }
}
