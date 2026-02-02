# reactive-fleet-iot-monitor
pring Boot (WebFlux) Real-Time Logistics/Fleet IoT Monitor, includes Swagger/OpenAPI docs, product description, RSocket + WebSocket streaming, Redis live-cache, geofencing alerts, backpressure handling, and Testcontainers tests

# Real-Time Logistics / Fleet IoT Monitor (Reactive)

A **high-performance, non-blocking** fleet telemetry backend that simulates **1,000+ trucks** updating every second,
supports **bidirectional streaming** via **RSocket** and optional **WebSockets**, and caches live state in **Redis**.

This project is built to demonstrate:
- **Reactive programming (Project Reactor + Spring WebFlux)** — strictly no blocking I/O in request handling
- **Backpressure** strategies to survive bursts (e.g., 5,000 trucks online at once)
- **Geofencing** (polygon zones) with instant alert streaming
- **Real-time** streams suitable for map dashboards (truck markers move smoothly)

## Tech stack
- Spring Boot 3.4.x
- Spring WebFlux (reactive HTTP)
- RSocket (TCP streaming) + WebSocket (browser-friendly)
- Redis (Reactive) for live positions cache
- Testcontainers for integration tests

------------------------------------------------------------------------------------------------------------------------------

## Run locally

### 1) Start Redis
```bash
docker compose up -d
```

### 2) Run the API
```bash
mvn -q -DskipTests spring-boot:run
```

### 3) Swagger
- http://localhost:8080/swagger-ui/index.html

### 4) RSocket
RSocket server runs on TCP port **7000** by default.

---------------------------------------------------------------------------------------------------------

## Core flows

### A) Start/stop simulator
- `POST /api/simulator/start?count=1000`
- `POST /api/simulator/stop`

### B) Get live truck state (from Redis)
- `GET /api/fleet/trucks?limit=200&offset=0`
- `GET /api/fleet/trucks/{truckId}`

### C) Geofences & alerting
- `POST /api/geofences` create a polygon zone
- `GET /api/geofences` list zones
- Alerts stream (SSE): `GET /api/alerts/stream`

### D) Real-time stream for dashboards
**RSocket routes**
- `fleet.positions` -> stream of `TruckTelemetry` (all trucks; backpressure protected)
- `fleet.alerts` -> stream of `AlertEvent`

**WebSocket**
- `ws://localhost:8080/ws/telemetry` (server pushes `TruckTelemetry` JSON)

---

## Backpressure (hired feature)
The simulator publishes telemetry into a Reactor **Sink**.
Choose backpressure mode via env:
- `FLEET_BACKPRESSURE_MODE=latest` (default): slow consumers only get latest updates
- `FLEET_BACKPRESSURE_MODE=drop`: drops under pressure
- `FLEET_BACKPRESSURE_MODE=buffer`: buffers up to `FLEET_BACKPRESSURE_BUFFER`

Burst test:
```bash
curl -X POST "http://localhost:8080/api/simulator/start?count=5000"
```

------------------------------------------------------------------------------------------

## Sample RSocket client (Java)
Use Spring's `RSocketRequester`:
- Route: `fleet.positions`
- Data: `{ "sampleMs": 250 }`

--------------------------------------------------------------------------------------------

## Notes
- This backend is designed for a real-time map UI. For the frontend, subscribe to the RSocket/WebSocket stream and animate markers.
- For long-term history, add TimescaleDB (R2DBC) and write telemetry events asynchronously; keep the live path fast.


