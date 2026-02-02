# Product Description — Real-Time Logistics / Fleet IoT Monitor

## One-liner
A **real-time fleet telemetry platform** that streams live truck locations, fuel, and speed at 1Hz+ with built-in backpressure and geofencing alerts.

## Target users
- Logistics operations (dispatch, routing, compliance)
- Field support and incident response
- Data engineering teams validating IoT pipelines
- Engineering teams that need low-latency, high-throughput streaming

## Core user stories
1) As an operator, I want to see all trucks moving on a live map and updating every second.
2) As a supervisor, I want alerts the instant a truck enters a restricted/priority zone.
3) As an engineer, I want to handle burst events (5,000 trucks) without memory blowups.
4) As a developer, I want a clean reactive API with Swagger docs and repeatable tests.

## Differentiators (why it gets you hired)
- End-to-end reactive pipeline with **non-blocking I/O**
- Backpressure strategies clearly implemented and configurable
- Real-time streaming via **RSocket** (and WebSockets for browsers)
- Geofencing algorithm integrated into the live stream
- Integration tests with **real Redis** via Testcontainers
