# 🚨 Disaster Management System (DMS)

> **An event-driven disaster-response platform built with Java 21, Spring Boot, Apache Kafka, Redis GEO, PostgreSQL, and full-stack observability with Prometheus, Grafana, and Zipkin.**

DMS is an enterprise-grade microservices system designed to coordinate high-stress disaster response workflows end-to-end: **incident reporting → rescue coordination → hospital readiness → emergency logistics → notifications**.

The project focuses on distributed-systems primitives rather than standard CRUD operations: asynchronous event processing, geospatial search, concurrency control, distributed tracing across network hops, metric scraping, JWT authentication, retry/DLT handling, and real-time telemetry.

---

## ✨ Features

- 🔐 **Stateless Security**: JWT authentication with BCrypt password hashing and Redis-backed token revocation.
- 🚪 **API Gateway**: Central entry point for intelligent routing, CORS handling, and identity context injection (`X-User-Id`, `X-User-Role`).
- 🚨 **Incident Lifecycle**: Comprehensive incident reporting, priority queues, and lifecycle state management.
- 📍 **Spatial Duplicate Detection**: Haversine-based duplicate incident prevention (50-meter & 24-hour threshold window).
- 🚒 **Autonomous Rescue Assignment**: Dynamic nearest-department dispatch algorithms via spatial queries.
- 📡 **Telemetry & Geofencing**: High-frequency rescue unit GPS tracking in Redis GEO with automatic `ON_SCENE` transitions.
- 🏥 **Hospital Surge & Bed Management**: Redis GEO proximity discovery combined with row-level pessimistic locking for race-free bed allocation.
- 📦 **Emergency Supply Allocation**: First-Expired, First-Out (FEFO) warehouse inventory reservation with pessimistic database locks.
- 🚚 **Automated Supply Dispatches**: Hospital inventory shortages autonomously trigger warehouse vehicle dispatches via Kafka.
- 🔁 **Kafka Fault Tolerance**: Consumer retry mechanisms with `@RetryableTopic` backoff and Dead Letter Topic (DLT) routing.
- 📧 **Event-Driven Alerts**: Real-time multi-zone evacuation emails and supply arrival notices via Spring Mail.
- 📊 **Metrics & Observability**: Real-time metric scraping via Spring Boot Actuator and Prometheus, visualized in Grafana dashboards.
- 🔍 **Distributed Tracing**: End-to-end trace context propagation across the Gateway, Kafka brokers, and downstream microservices using Micrometer Tracing & Zipkin.
- 📚 **Centralized OpenAPI/Swagger**: Interactive unified API documentation proxied through the API Gateway.

---

## 🧩 Microservices & Infrastructure Topology

| Service / Container      | Port   | Responsibility                                                  |
|----------------------------|:------:|--------------------------------------------------------------------|
| **API Gateway**            | 8080   | Routing, JWT validation, identity injection, CORS                   |
| **User Service**           | 8081   | Authentication, profiles, government-official hierarchy             |
| **Incident Service**       | 8082   | Incident lifecycle, geospatial duplicate detection                  |
| **Rescue Service**         | 8083   | Rescue hubs, missions, state pattern, GPS telemetry                  |
| **Hospital Service**       | 8084   | Bed allocation (pessimistic lock), admissions, surge readiness       |
| **Logistics Service**      | 8085   | Warehouses, FEFO inventory reservation, dispatch orders               |
| **Notification Service**   | 8086   | Event-driven mass email alerts and audit trails                     |
| **Zipkin Server**          | 9411   | Distributed trace visualization and latency profiling                |
| **Prometheus**             | 9090   | Time-series metrics collection and Actuator scraping                 |
| **Grafana**                | 3000   | Operational dashboards for throughput, JVM, and SLA metrics          |
| **Kafdrop**                | 9000   | Web UI for inspecting Kafka topics, partitions, and consumers         |
| **PostgreSQL**             | 5432   | Logical database-per-microservice pattern                            |
| **Redis 7**                | 6379   | Token blacklist, Redis GEO spatial index, mission cache               |
| **Apache Kafka**           | 9092   | Asynchronous event streaming backbone                                 |

---

## 🔄 Core Architecture & Workflows

A single incident report fans out across the distributed cluster through Kafka, with all operations tracked via correlated trace IDs in Zipkin:

```text
               Client (Web / Mobile)
                         │
                         ▼ [Trace ID: 4bf92f3577b34da6]
                ┌──────────────────┐
                │   API Gateway    │ (Port 8080)
                │ (JWT Validation) │
                └────────┬─────────┘
                         │
                         ▼
                ┌──────────────────┐
                │ Incident Service │ (Port 8082)
                └────────┬─────────┘
                         │
                         │ IncidentCreatedEvent (Trace Propagated)
                         ▼
             ═════════ Kafka Topic ═════════
             /            │                \
            /             │                 \
           ▼              ▼                  ▼
    ┌─────────────┐ ┌──────────────┐ ┌───────────────────────┐
    │Rescue Service│ │Hospital Serv.│ │ Notification Service  │
    │ (Port 8083) │ │ (Port 8084)  │ │     (Port 8086)        │
    └──────┬──────┘ └──────────────┘ └──────────┬─────────────┘
           │                                    │
           ├─ Redis GEO Lookup (<50km)          ├─ Spatial Query (User Service)
           ├─ State: DISPATCHED → EN_ROUTE      └─ Evacuation Email Alerts
           └─ Telemetry Ping → ON_SCENE
```

### Emergency Supply Logistics Loop

```text
Hospital Service (Stock <= Critical Threshold)
       │
       ▼ (Publishes InventoryShortageAlertEvent)
Kafka: inventory-shortage-topic
       │
       ▼
Logistics Service (Retryable Consumer + DLT)
       │
       ├─ Nearest Warehouse Resolution (Haversine)
       ├─ FEFO Inventory Reservation (Pessimistic Lock)
       ├─ Vehicle Assignment & Status → EN_ROUTE
       └─ Publishes SupplyDispatchedEvent
                │
         ┌──────┴──────┐
         ▼             ▼
  Hospital Service   Notification Service
  (Expect Restock)   (Dispatch Notice to Facility)
```

---

## 📊 Observability & Distributed Tracing

DMS incorporates an observability stack ensuring zero blind spots across asynchronous and network hops:

```text
Microservices (:8081 - :8086) & Gateway (:8080)
      │                                     │
      │ 1. Micrometer Span Reporting        │ 2. Scrape Actuator /prometheus
      ▼                                     ▼
┌──────────────┐                      ┌────────────┐
│ Zipkin Server│ (:9411)              │ Prometheus │ (:9090)
└──────────────┘                      └─────┬──────┘
                                             │ 3. Query Engine
                                             ▼
                                       ┌────────────┐
                                       │  Grafana   │ (:3000)
                                       └────────────┘
```

### 1. Distributed Tracing with Zipkin (`:9411`)

- **Trace Context Propagation**: B3 / W3C TraceContext headers carry `traceId` and `spanId` across Gateway HTTP calls and Kafka record headers.
- **Latency Bottleneck Detection**: Pinpoint exact delays during complex multi-service operations (e.g., measuring duration from incident creation to notification dispatch).

### 2. Metrics Scraping with Prometheus (`:9090`)

- Automatically scrapes `/actuator/prometheus` across all registered microservices.
- Captures JVM heap/non-heap utilization, HikariCP database connection pool metrics, Kafka consumer lag, and HTTP request rates.

### 3. Unified Dashboards with Grafana (`:3000`)

- Real-time visualization of cluster health, system latencies (p50, p95, p99), circuit breaker transition states (Resilience4j), and emergency dispatch counts.

---

## 📨 Kafka Event Architecture

| Topic                          | Producer          | Consumer(s)                     | Role / Impact                                                             |
|---------------------------------|--------------------|----------------------------------|-----------------------------------------------------------------------------|
| `incident-created-topic`        | Incident Service   | Rescue, Hospital, Notification   | Triggers mission assignment, hospital surge readiness, and evacuation emails |
| `rescue-mission-status-topic`   | Rescue Service      | Incident Service                 | Synchronizes mission state transitions (`EN_ROUTE`, `ON_SCENE`, `COMPLETED`)  |
| `victims-extracted-topic`       | Rescue Service      | Hospital Service                 | Prepares trauma units and reserves ICU beds ahead of patient arrival          |
| `hospital-standby-topic`        | Hospital Service    | Notification Service             | Transmits operational standby directives to medical personnel                |
| `inventory-shortage-topic`      | Hospital Service    | Logistics Service                | Requests urgent medical supplies under `@RetryableTopic` safeguards           |
| `supply-dispatched-topic`       | Logistics Service   | Hospital, Notification           | Signals incoming convoy details, payload counts, and ETA                     |

---

## 🔒 Concurrency & Reliability Controls

- **Pessimistic Bed Locking**: `SELECT ... FOR UPDATE` row locks eliminate race conditions when simultaneous mass-casualty events demand the last available ICU bed.
- **FEFO Stock Locking**: Available inventory batches ordered by `expiry_date ASC` are acquired using pessimistic write locks, preventing dual-dispatch reservations.
- **State Machine Pattern**: Rescue mission operations are bounded by explicit state transitions (`DISPATCHED` → `EN_ROUTE` → `ON_SCENE` → `COMPLETED`/`ESCALATED`), preventing out-of-order telemetry triggers.
- **Dead Letter Topics (DLT)**: Failing logistics messages undergo 3 exponential-backoff retries before being safely shunted to an isolated DLT for audit and replay.

---

## 🛠️ Tech Stack

- **Core Framework**: Java 21, Spring Boot 3.x, Spring Web, Spring Data JPA, Spring Cloud Gateway
- **Persistence & Caching**: PostgreSQL (dedicated schemas), Redis 7 (GEO indexes, expiring blacklist)
- **Messaging**: Apache Kafka, ZooKeeper, Spring Kafka (retryable & DLT support)
- **Observability & Telemetry**: Micrometer Tracing, Zipkin, Prometheus, Grafana, Spring Boot Actuator
- **Resilience & Security**: Spring Security, JJWT (HMAC-SHA256), Resilience4j
- **Documentation**: SpringDoc OpenAPI, Swagger UI
- **Infrastructure**: Docker, Docker Compose, Kafdrop, pgAdmin

---

## 🚀 Quick Start Guide

### 1. Clone & Set Up Directory

```bash
git clone https://github.com/Ashwani-yadav01/dms.git
cd dms
```

### 2. Boot Infrastructure & Observability Suite

Ensure Docker Desktop or the Docker daemon is running, then spin up the cluster:

```bash
docker compose up -d
```

Verify that all supporting systems are healthy:

```bash
docker compose ps
```

### 3. Web & Telemetry Dashboards

| Component        | URL                                        | Purpose                            |
|--------------------|---------------------------------------------|-------------------------------------|
| **API Gateway**    | `http://localhost:8080`                     | Public API entrypoint               |
| **Swagger UI**     | `http://localhost:8080/swagger-ui.html`     | Central interactive API docs        |
| **Zipkin UI**      | `http://localhost:9411`                     | Trace analysis & span tree          |
| **Prometheus**     | `http://localhost:9090`                     | Targets & metric grapher            |
| **Grafana**        | `http://localhost:3000`                     | System dashboards (`admin`/`admin`) |
| **Kafdrop**        | `http://localhost:9000`                     | Kafka topic & partition monitor     |
| **pgAdmin**        | `http://localhost:5050`                     | Database administrator console      |

### 4. Build & Run Services

In separate terminal tabs, or using your IDE's run configurations, boot the microservices in the following recommended order:

1. `api-gateway` (`:8080`)
2. `user-service` (`:8081`)
3. `incident-service` (`:8082`)
4. `rescue-service` (`:8083`)
5. `hospital-service` (`:8084`)
6. `logistics-service` (`:8085`)
7. `notification-service` (`:8086`)

---

## 📁 Project Structure

```text
dms/
├── api-gateway/            # Gateway, route predicates, JWT auth filter
├── user-service/           # Auth, roles, user/official profiles, spatial lookup
├── incident-service/       # Incident reporting, 50m duplicate Haversine engine
├── rescue-service/         # Departments, state pattern missions, Redis telemetry
├── hospital-service/       # Beds, pessimistic row locks, medical inventory
├── logistics-service/      # Warehouses, FEFO batch allocator, fleet tracking
├── notification-service/   # Kafka alert consumers, Spring Mail dispatcher
├── docker-compose.yml      # Postgres, Redis, Kafka, Zipkin, Prometheus, Grafana
└── README.md
```

---

## 🔮 Future Roadmap

- [ ] Implement service-level `@PreAuthorize` authorization checks downstream.
- [ ] Add Transactional Outbox Pattern with Debezium CDC for zero-loss Kafka events.
- [ ] Deploy multi-node Kubernetes configurations with Helm charts.
- [ ] Add real-time vehicle route optimization with OpenStreetMap / OSRM engine.
- [ ] Introduce multi-channel emergency SMS via Twilio / AWS SNS.

---

## 👨‍💻 Authors

- **Ashwani Yadav** — Backend architecture, distributed systems, Spring Boot
- **Rahul Kumar** — Frontend engineering, interactive maps, React UI
