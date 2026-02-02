package com.example.fleet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reactive Fleet IoT Monitor API")
                        .version("0.1.0")
                        .description("Reactive fleet telemetry simulator with Redis cache, RSocket/WebSocket streaming, geofencing and backpressure."));
    }
}
