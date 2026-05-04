# ARCHITECTURE - be-management-kebun

## Overview
Service: `be-management-kebun`

Style:
- Layered architecture (Controller -> Service -> Repository)
- Event-driven integration (Kafka producer)
- Defensive domain validation in model/service layers

## Package Structure
- `controller` : REST endpoints (`KebunController`)
- `service` : business logic and orchestration (`KebunService`)
- `repository` : persistence contracts (`KebunRepository`)
- `model` : domain entities/value objects (`Kebun`, `Point`, Builder pattern)
- `validation` : overlap validation strategy (`OverlapValidator`, `OverlapValidatorImpl`)
- `mapper` : geometric conversion (`GeometryMapper`)
- `event` : integration events (`MandorAssignedEvent`)
- `config` : infrastructure configuration (`KafkaProducerConfig`)
- `exception` : global API exception mapping (`GlobalExceptionHandler`)

## Core Design Decisions
1. Builder pattern for domain object creation
- `Kebun.builder()` used in tests and service/controller flows.

2. Geometric domain rules at model boundary
- Exactly 4 points and square-shape validation are enforced in `Kebun.Builder`.

3. Overlap check abstraction
- `OverlapValidator` encapsulates spatial overlap policy using `GeometryMapper` and repository contract.

4. In-process concurrency guard for writes
- `KebunService` uses a fair `ReentrantLock` to serialize `create` and `update`.
- This reduces race risk between overlap-check and save in concurrent requests handled by the same instance.

5. Asynchronous integration
- Mandor assignment emits `MandorAssignedEvent` through Kafka for downstream microservices.

## Runtime Flow (Create Kebun)
1. `POST /kebun` request enters `KebunController.create`.
2. `KebunService.create` acquires write lock.
3. `OverlapValidator.validateNoOverlap` runs geometry intersection checks.
4. Repository save is executed.
5. Lock is released and response returned.

## Runtime Flow (Assign Mandor)
1. Service validates kebun exists.
2. Repository persists assignment.
3. Kafka producer publishes `MandorAssignedEvent`.

## Observability and Ops
- Spring Boot Actuator enabled.
- Prometheus metrics endpoint exposed.
- CI pipeline runs automated tests.

## Security & Clean Code Notes
- No hardcoded credentials in source.
- Configuration values are externalized (Kafka topic/servers, CI secrets).
- Exception mapping avoids leaking stack traces in API response body.

## Current Concurrency Limitation
- The write lock is process-local; in multi-instance deployment, DB-level exclusion constraints/locking should be added for strict global overlap safety.
