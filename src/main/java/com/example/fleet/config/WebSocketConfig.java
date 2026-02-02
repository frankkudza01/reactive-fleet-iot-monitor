package com.example.fleet.config;

import com.example.fleet.ws.TelemetryWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.socket.server.support.SimpleUrlHandlerMapping;

import java.util.Map;

@Configuration
@EnableWebFlux
public class WebSocketConfig {

    @Bean
    public SimpleUrlHandlerMapping handlerMapping(TelemetryWebSocketHandler handler) {
        var mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of("/ws/telemetry", handler));
        mapping.setOrder(1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
