# Disaster Management System (DMS) - Complete Technical Project Dossier

> **Analysis principle:** this document describes what is actually present in the source tree. Features that are only configured, partially implemented, or absent are explicitly called out.

---

## 1. Executive Summary

DMS is a **Java 21 / Spring Boot microservices backend** for coordinating disaster response across incident reporting, rescue operations, hospitals, emergency logistics, user management, and notifications.

The system is organized around:

- **REST APIs** exposed through an API Gateway.
- **JWT-based authentication** at the gateway, with Redis-backed token revocation.
- **PostgreSQL** persistence for each business service.
- **Apache Kafka** for asynchronous event-driven communication.
- **Redis GEO** for location-aware lookup and live rescue-unit tracking.
- **Email** for operational alerts.
- **Swagger/OpenAPI** for service API documentation.
- **Docker Compose** for PostgreSQL, Redis, Kafka, ZooKeeper, pgAdmin, and Kafdrop.

The strongest architectural idea in the project is the separation between:
1. synchronous command/query APIs, and
2. asynchronous disaster-response workflows driven by Kafka events.

---

# 2. What the System Can Do

## User Service

- Register users.
- Login with email/password.
- Generate JWT access tokens.
- Logout by blacklisting the remaining lifetime of a JWT in Redis.
- Maintain multiple user roles.
- Maintain a generic user profile.
- Maintain specialized citizen, volunteer, NGO, and government-official profiles.
- Maintain government hierarchy such as supervisors/subordinates.
- Verify government officials.
- Change official status.
- Search officials by department, hierarchy, employee ID, and jurisdiction-related information.
- Find user email addresses within a geographic radius.

## Incident Service

- Create disaster/incident reports.
- Store incident type, severity, coordinates, description, reporter and status.
- Detect likely duplicate incidents within **50 meters** and the previous 24 hours.
- Link duplicate reports to a parent/root incident.
- Query incidents by ID, status, severity, user and pagination.
- Query active incidents.
- Query active incidents within a radius.
- Update incident status.
- Update/delete incidents.
- Publish a Kafka `IncidentCreatedEvent` for non-duplicate incidents.

## Rescue Service

- Register rescue departments.
- Register station chiefs/personnel.
- Track department capacity and availability.
- Find available departments near an incident.
- Automatically assign a nearby rescue department to a newly created incident.
- Manually assign a department.
- Create rescue missions.
- Track mission status.
- Track rescue-unit GPS telemetry.
- Use a geofence to transition a unit to `ON_SCENE`.
- Complete, cancel, or escalate missions.
- Publish mission-status events through Kafka.
- Propagate completed rescue information toward the hospital workflow.
- Use Redis GEO for spatial tracking.

## Hospital operations

- Register hospitals.
- Track hospital location.
- Track accepting/not-accepting status.
- Track general and ICU bed capacity.
- Track medical specialities.
- Find nearby hospitals using Redis GEO.
- Filter hospitals by ICU requirement and medical speciality.
- Admit patients.
- Discharge patients.
- Use database row locking for concurrent bed allocation.
- Maintain medical inventory.
- Detect critical inventory shortages.
- Publish shortage events to logistics.
- Receive supply-dispatched events and automatically restock inventory.
- Receive incident-created events and alert nearby hospitals for surge standby.
- Include hospital elevation data and an elevation-based repository query for flood-related filtering.

## Logistics Service

- Register warehouses.
- Maintain warehouse locations.
- Register emergency transport vehicles.
- Maintain vehicle status/location/capacity.
- Maintain inventory batches.
- Track expiry dates.
- Respond to hospital inventory-shortage Kafka events.
- Select the nearest active warehouse.
- Allocate inventory using FEFO (First Expired, First Out).
- Use pessimistic locking while allocating inventory.
- Select an available vehicle.
- Calculate an estimated arrival time.
- Create dispatch orders.
- Publish `SupplyDispatchedEvent`.
- Mark deliveries as delivered and release the vehicle.

## Notifications Service

- Consume incident-created events.
- Find users within a **5 km** radius of an incident.
- Send evacuation emails.
- Consume hospital standby events.
- Send hospital surge emails.
- Consume logistics dispatch events.
- Send supply-arrival emails.
- Persist notification audit records.
- Record `SENT` or `FAILED` notification status.

---

# 3. High-Level Architecture

```text
                         ┌─────────────────────┐
                         │      Client         │
                         │ Web / Mobile / API  │
                         └──────────┬──────────┘
                                    │ HTTP
                                    ▼
                         ┌─────────────────────┐
                         │    API Gateway      │
                         │      :8080          │
                         │ Routing + JWT check │
                         └──────┬──────┬───────┘
                                │      │
              ┌─────────────────┘      └───────────────────┐
              ▼                                            ▼
      ┌──────────────┐                              ┌──────────────┐
      │ User Service │                              │   Incident   │
      │    :8081     │                              │   Service    │
      └──────┬───────┘                              │    :8082     │
             │                                      └──────┬───────┘
             │                                             │
             │                                             │ Kafka
             │                                             ▼
             │                                      ┌──────────────┐
             │                                      │    Rescue    │
             │                                      │    :8083     │
             │                                      └──────┬───────┘
             │                                             │
             │                                             │ Kafka
             │                                             ▼
             │                                      ┌──────────────┐
             │                                      │   Hospital   │
             │                                      │    :8084     │
             │                                      └──────┬───────┘
             │                                             │
             │                                             │ Kafka
             │                                             ▼
             │                                      ┌──────────────┐
             │                                      │  Logistics   │
             │                                      │    :8085     │
             │                                      └──────┬───────┘
             │                                             │
             │                                             │ Kafka
             │                                             ▼
             │                                      ┌──────────────┐
             └──────────── HTTP ───────────────────│ Notification │
                                                    │    :8086     │
                                                    └──────────────┘

       ┌──────────────────────────────────────────────────────────┐
       │                    Infrastructure                        │
       │ PostgreSQL | Redis | Kafka | ZooKeeper | pgAdmin | Kafdrop│
       └──────────────────────────────────────────────────────────┘
```

---

# 4. Services and Ports

| Component | Port | Main responsibility |
|---|---:|---|
| API Gateway | 8080 | Routing, CORS, JWT validation, Redis blacklist check |
| User Service | 8081  | Identity, profiles, government hierarchy |
| Incident Service | 8082 | Disaster incident lifecycle |
| Rescue Service | 8083 | Rescue departments, missions, telemetry |
| Hospital Service | 8084 | Hospitals, beds, admissions, inventory |
| Logistics Service | 8085 | Warehouses, stock, vehicles, dispatch |
| Notification Service | 8086 | Kafka-driven email alerts and audit logs |


# 5. Technology Stack

## Language and framework

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Spring Security
- Spring Kafka
- Spring Data Redis
- Spring Cloud Gateway
- SpringDoc OpenAPI / Swagger
- Lombok
- ModelMapper

## Data

- PostgreSQL
- Redis
- Hibernate Spatial appears in Rescue and Hospital dependencies.

## Messaging

- Apache Kafka
- ZooKeeper-based Kafka container
- Kafka JSON serialization/deserialization
- Retryable Kafka topics
- Dead Letter Topic handling in Logistics

## Resilience / observability

- Resilience4j dependency and circuit-breaker configuration
- Spring Boot Actuator
- Health/circuit-breaker endpoint exposure
- Kafdrop for Kafka visibility
- pgAdmin for PostgreSQL administration

## Infrastructure

- Docker Compose
- Docker volumes
- Docker bridge network

## Notification

- Gmail SMTP
- Spring Mail

---

# 6. User Service

## Responsibility

The User Service is the identity and profile domain.

### Roles

The source defines:

- `CITIZEN`
- `VOLUNTEER`
- `NGO`
- `GOVERNMENT_OFFICIAL`
- `DISTRICT_ADMIN`
- `RESCUE_TEAM`

### Authentication flow

```text
Register/Login
      │
      ▼
User Service
      │
      ├── PostgreSQL user lookup
      │
      ├── BCrypt password verification
      │
      ▼
JWT generated
      │
      ▼
Client
```

The JWT contains:

- subject = username/email
- userId
- role
- issued-at
- expiration

### Logout flow

```text
Client sends Bearer JWT
          │
          ▼
User Service extracts token
          │
          ▼
Calculate remaining expiration
          │
          ▼
Redis:
jwt_blacklist:<token> = revoked
TTL = remaining JWT lifetime
```

This avoids storing revoked tokens forever.

### Geographic user lookup

The User Service executes a PostgreSQL native query using the spherical-distance formula to find users whose profile coordinates are within a requested radius.

This is consumed by Notification Service during incident alerts.

---

# 7. Incident Service

## Core entity

`Incident`

Important fields include:

- UUID ID
- title
- description
- incident type
- latitude
- longitude
- severity
- status
- image URL
- reporter ID
- parent incident ID
- timestamps

## Duplicate detection

The service checks active incidents from the previous 24 hours.

Two reports are considered duplicates when:

- incident type matches
- coordinates are within **50 meters**

The service calculates distance using the Haversine formula.

If a duplicate is detected:

```text
New incident
    │
    ▼
status = DUPLICATE
    │
    ▼
parentIncidentId = root incident
```

Duplicate incidents do **not** publish `IncidentCreatedEvent`.

## Event publishing

For a normal incident:

```text
POST /api/v1/incidents
        │
        ▼
Save Incident
        │
        ▼
IncidentCreatedEvent
        │
        ▼
Kafka: incident-created-topic
        │
        ├──────────────► Rescue Service
        ├──────────────► Hospital Service
        └──────────────► Notification Service
```

This is one of the central event fan-out points in the architecture.

---

# 8. Rescue Service

## Main entities

### RescueDepartment

Contains:

- department name
- department type
- jurisdiction
- coordinates
- station chief
- personnel
- contact phone
- total capacity
- active mission count
- availability

### RescueMission

Contains:

- incident ID
- assigned department
- leader
- status
- SLA minutes
- arrival coordinates
- arrival time
- SLA breach flag
- dispatch/completion timestamps
- notes

### RescuePersonnel

Contains:

- name
- badge
- phone
- department
- chief flag
- availability

---

# 9. Rescue Auto-Assignment

When Rescue Service receives `IncidentCreatedEvent`:

```text
IncidentCreatedEvent
       │
       ▼
Register incident in Redis GEO
       │
       ▼
Find available departments
within 50 km
       │
       ▼
Sort by distance
       │
       ▼
Choose nearest department
       │
       ▼
Find and occupy station chief
       │
       ▼
Create RescueMission
       │
       ▼
Increment department active missions
       │
       ▼
Publish DISPATCHED status
```

The department repository uses a PostgreSQL Haversine query to find available departments within the configured radius.

---

# 10. Rescue Mission State Machine

The project uses the **State Pattern**.

States include:

```text
DISPATCHED
     │
     ▼
 EN_ROUTE
     │
     ▼
 ON_SCENE
     │
     ├────────► COMPLETED
     │
     ├────────► CANCELLED
     │
     └────────► ESCALATED

EN_ROUTE ─────► ESCALATED
```

## State behavior

### DISPATCHED

The first telemetry movement transitions the mission to `EN_ROUTE`.

### EN_ROUTE

When distance to the incident is within the configured geofence, the mission transitions to `ON_SCENE`.

### ON_SCENE

Can be:

- completed
- cancelled
- escalated

### Terminal states

- COMPLETED
- CANCELLED

When a mission reaches a terminal state, the service:

- releases department capacity
- releases the assigned chief where applicable
- updates mission state
- cleans spatial Redis tracking
- publishes a Kafka status event

---

# 11. Rescue Telemetry

Telemetry is deliberately optimized around Redis.

```text
GPS ping
   │
   ▼
Rescue Service
   │
   ▼
Redis GEO update
   │
   ▼
Calculate distance to incident
   │
   ├── > geofence → stay EN_ROUTE
   │
   └── <= geofence → ON_SCENE
                         │
                         ▼
                    PostgreSQL update
                         │
                         ▼
                    Kafka status event
```

This avoids writing every GPS coordinate to PostgreSQL.

The source specifically uses Redis GEO for the frequently changing spatial position while PostgreSQL stores mission state.

---

# 12. Hospital Service

## Hospital model

Tracks:

- name
- email
- facility type
- coordinates
- elevation
- general bed capacity
- ICU capacity
- specialities
- inventory
- admissions
- accepting-patients flag

## Nearest hospital search

```text
Incident coordinates
       │
       ▼
Redis GEO
       │
       ▼
Nearby hospital IDs
       │
       ▼
PostgreSQL
       │
       ▼
Filter:
- accepting patients
- ICU availability
- general beds
- speciality
       │
       ▼
Nearest capable hospitals
```

This is a good example of using Redis for fast spatial candidate selection and PostgreSQL for authoritative business data.

---

# 13. Hospital Surge Workflow

When an incident is created:

```text
incident-created-topic
        │
        ▼
Hospital Service
        │
        ▼
Find hospitals within 15 km
        │
        ▼
Keep hospitals accepting patients
        │
        ▼
Create HospitalSurgeStandbyEvent
        │
        ▼
hospital-standby-topic
        │
        ▼
Notification Service
        │
        ▼
Hospital email alert
```

The current implementation uses:

- default hospital alert radius = **15 km**
- default estimated casualties = **15**

These are source-code defaults, not external predictions.

---

# 14. Hospital Inventory

Each inventory item has:

- hospital
- item type
- current quantity
- critical threshold
- last updated time

When stock changes:

```text
currentQuantity <= criticalThreshold
             │
             ▼
InventoryShortageAlertEvent
             │
             ▼
inventory-shortage-topic
             │
             ▼
Logistics Service
```

The requested restock amount is calculated from the critical threshold with a minimum requested amount.

## Concurrency

Inventory updates use a **pessimistic write lock**.

This matters because two simultaneous emergency requests must not both read the same old stock level and overwrite each other's updates.

---

# 15. Logistics Service

## Main entities

### Warehouse

- location
- address
- active status

### InventoryBatch

- warehouse
- item type
- batch number
- quantity
- reserved quantity
- expiry date
- temperature class

### TransportVehicle

- vehicle number
- vehicle type
- status
- payload capacity
- base warehouse
- current coordinates

### DispatchOrder

- target hospital
- source warehouse
- item
- requested quantity
- dispatched quantity
- assigned vehicle
- status
- ETA

---

# 16. Emergency Supply Allocation

When Logistics receives an inventory shortage:

```text
Hospital
  │
  │ shortage
  ▼
inventory-shortage-topic
  │
  ▼
Logistics
  │
  ▼
Find active warehouses
  │
  ▼
Sort by Haversine distance
  │
  ▼
Nearest warehouse
  │
  ▼
Find stock batches
  │
  ▼
FEFO allocation
  │
  ▼
Lock available vehicle
  │
  ▼
Mark vehicle EN_ROUTE
  │
  ▼
Create DispatchOrder
  │
  ▼
Publish SupplyDispatchedEvent
```

---

# 17. FEFO Inventory Strategy

The logistics inventory query orders batches by:

```text
expiryDate ASC
```

Therefore the earliest-expiring usable batch is reserved first.

This is **FEFO: First Expired, First Out**.

The allocation query uses a pessimistic write lock so concurrent emergency dispatches cannot casually reserve the same stock.

This is one of the stronger interview-worthy implementation details in the project.

---

# 18. Logistics Reliability

The inventory-shortage Kafka consumer uses:

- `@RetryableTopic`
- 3 attempts
- exponential-ish backoff configuration
- Dead Letter Topic handling

Flow:

```text
Kafka event
    │
    ▼
Consumer attempt 1
    │
    ├── success → done
    │
    └── failure
           │
           ▼
       retry
           │
           ▼
Consumer attempt 2
           │
           ▼
Consumer attempt 3
           │
           ├── success → done
           │
           └── failure
                  │
                  ▼
                 DLT
```

This is substantially more defensible in an interview than simply saying "I used Kafka."

---

# 19. Notification Service

Notification Service is event-driven.

It listens to:

- `incident-created-topic`
- `hospital-standby-topic`
- `supply-dispatched-topic`

## Incident alert

```text
IncidentCreatedEvent
       │
       ▼
Notification Service
       │
       ▼
HTTP call to User Service
       │
       ▼
Find users within 5 km
       │
       ▼
Send evacuation email
       │
       ▼
Persist NotificationLog
```

If no nearby users are found, the current implementation sends the alert to the configured admin email.

## Hospital alert

Hospital standby event causes an email containing:

- hospital
- disaster type
- distance
- estimated casualties
- operational instructions

## Logistics alert

Supply dispatch event causes an email containing:

- supply type
- quantity
- source warehouse
- vehicle
- ETA

---

# 20. Notification Audit Trail

`NotificationLog` stores:

- recipient email
- subject
- message body
- alert type
- status
- error message
- timestamp

The service records both successful and failed email attempts.

This provides an operational audit trail instead of treating email as a fire-and-forget side effect.

---

# 21. Kafka Event Architecture

The important topics found in the source include:

| Topic | Producer | Consumers / purpose |
|---|---|---|
| `incident-created-topic` | Incident | Rescue, Hospital, Notification |
| `rescue-mission-status-topic` | Rescue | Incident |
| `victims-extracted-topic` | Rescue | Hospital-side workflow |
| `hospital-standby-topic` | Hospital | Notification |
| `inventory-shortage-topic` | Hospital | Logistics |
| `supply-dispatched-topic` | Logistics | Hospital, Notification |

Conceptually:

```text
                         incident-created
                         /       |       \
                        /        |        \
                       ▼         ▼         ▼
                   Rescue    Hospital   Notification
                     │          │
                     │          ├── hospital-standby ──► Notification
                     │          │
                     │          └── inventory-shortage ─► Logistics
                     │                                      │
                     │                                      ▼
                     │                              supply-dispatched
                     │                                 /          \
                     │                                ▼            ▼
                     │                            Hospital     Notification
                     │
                     └── rescue-mission-status ──► Incident
```

---

# 22. Synchronous vs Asynchronous Communication

## Synchronous

Used where an immediate response is required:

- Client → API Gateway
- API Gateway → business service
- Notification Service → User Service for nearby email lookup

## Asynchronous

Used for workflows that should not block the original operation:

- Incident → Rescue
- Incident → Hospital
- Incident → Notification
- Hospital → Logistics
- Logistics → Hospital
- Logistics → Notification
- Rescue → Incident
- Rescue → Hospital

This separation reduces direct coupling between core disaster workflows.

---

# 23. Redis Usage

Redis is used for several different jobs.

## JWT blacklist

```text
jwt_blacklist:<token>
```

Used for logout/revocation.

## Rescue spatial tracking

Redis GEO stores:

```text
rescue_units
active_incidents
```

Used for:

- rescue-unit coordinates
- incident coordinates
- distance calculation

## Mission status cache

```text
mission_status:<missionId>
```

Used for fast mission-status access.

## Hospital spatial index

```text
hospitals:locations
```

Used to find hospitals within a radius.

---

# 24. PostgreSQL Usage

The services are designed around separate business databases:

- user database
- incident database
- rescue database
- hospital database
- logistics database
- notification database

The Docker Compose setup provides a PostgreSQL server, while service configurations use different database names.

This follows the microservice principle of keeping service-owned persistence separate at the logical database level.

---

# 25. Concurrency Control

Two particularly important pessimistic-locking implementations exist.

## Hospital beds

The hospital row is locked before changing available beds.

This prevents:

```text
Request A reads 1 ICU bed
Request B reads 1 ICU bed
A consumes it
B also consumes it
```

Instead:

```text
Request A → lock hospital row → consume bed → commit
Request B → waits → reads new count
```

## Logistics inventory

Available inventory batches are selected with a pessimistic write lock before reservation.

This protects stock allocation during concurrent dispatch operations.

---

# 26. API Inventory

## User

```text
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/logout

GET    /api/v1/users/{userId}
GET    /api/v1/users/email/{email}
GET    /api/v1/users/mobile/{mobileNumber}
GET    /api/v1/users
DELETE /api/v1/users/{userId}
GET    /api/v1/users/nearby/emails

GET    /api/v1/officials/employee/{employeeId}
GET    /api/v1/officials/department
GET    /api/v1/officials/hierarchy/{level}
GET    /api/v1/officials/{userId}/supervisor
GET    /api/v1/officials/{userId}/subordinates
GET    /api/v1/officials/departments/suggestions
PATCH  /api/v1/officials/{userId}/status
PATCH  /api/v1/officials/{userId}/verify
GET    /api/v1/officials/eligible

POST   /api/v1/users/{userId}/profile
GET    /api/v1/users/{userId}/profile
PUT    /api/v1/users/{userId}/profile
PATCH  /api/v1/users/{userId}/profile/photo
DELETE /api/v1/users/{userId}/profile
```

## Incident

```text
POST   /api/v1/incidents
GET    /api/v1/incidents/{id}
GET    /api/v1/incidents
GET    /api/v1/incidents/my-reports
GET    /api/v1/incidents/public/active
GET    /api/v1/incidents/public/nearby
PATCH  /api/v1/incidents/{id}/status
PUT    /api/v1/incidents/{id}
DELETE /api/v1/incidents/{id}
```

## Rescue

```text
POST   /api/v1/rescue/departments
GET    /api/v1/rescue/departments
GET    /api/v1/rescue/departments/{id}
GET    /api/v1/rescue/departments/jurisdiction/{jurisdictionCode}
GET    /api/v1/rescue/departments/nearby
PUT    /api/v1/rescue/departments/{id}
PATCH  /api/v1/rescue/departments/{id}/availability
DELETE /api/v1/rescue/departments/{id}
POST   /api/v1/rescue/departments/{id}/chiefs

POST   /api/v1/rescue/telemetry/ping

POST   /api/v1/rescue/missions/dispatch
GET    /api/v1/rescue/missions/{id}
GET    /api/v1/rescue/missions/incident/{incidentId}
GET    /api/v1/rescue/missions/{id}/live-status
POST   /api/v1/rescue/missions/{id}/complete
POST   /api/v1/rescue/missions/{id}/cancel
POST   /api/v1/rescue/missions/{id}/escalate
```

## Hospital

```text
POST   /api/v1/hospitals
GET    /api/v1/hospitals/{id}
PATCH  /api/v1/hospitals/{id}/status
GET    /api/v1/hospitals/nearest
DELETE /api/v1/hospitals/{id}

POST   /api/v1/hospitals/{hospitalId}/inventory
PATCH  /api/v1/hospitals/{hospitalId}/inventory
GET    /api/v1/hospitals/{hospitalId}/inventory
GET    /api/v1/hospitals/inventory/shortages
DELETE /api/v1/hospitals/inventory/{id}

POST   /api/v1/hospitals/{hospitalId}/inventory/shortage

POST   /api/v1/admissions/hospitals/{hospitalId}
PATCH  /api/v1/admissions/{admissionId}/discharge
```

## Logistics

```text
GET    /api/v1/logistics/warehouses
GET    /api/v1/logistics/dispatches/hospital/{hospitalId}
POST   /api/v1/logistics/warehouses
POST   /api/v1/logistics/inventory/batches
POST   /api/v1/logistics/vehicles
PATCH  /api/v1/logistics/dispatches/{orderId}/delivered
```

## Notification

```text
GET    /api/v1/notifications/logs
GET    /api/v1/notifications/logs/recipient
POST   /api/v1/notifications/test-email
```

---

# 27. API Gateway

The gateway:

- listens on port 8080
- routes requests to individual services
- applies CORS configuration
- validates JWTs on protected routes
- checks Redis JWT blacklist
- adds `X-User-Id`
- adds `X-User-Role`
- exposes aggregated Swagger API-document routes

Protected route groups include:

- users
- incidents
- rescue
- hospitals/admissions
- logistics
- notifications

Public route examples include:

- authentication endpoints
- public incident endpoints

---

# 28. Security Architecture

```text
Client
  │
  │ Authorization: Bearer JWT
  ▼
API Gateway
  │
  ├── Is Authorization header present?
  ├── Is JWT blacklisted?
  ├── Is JWT signature valid?
  ├── Is JWT expired?
  │
  ▼
Extract:
  userId
  role
  │
  ▼
Forward:
X-User-Id
X-User-Role
```

Passwords are encoded using BCrypt.

The services use stateless security configuration.

### Important security limitation

The business services largely trust the API Gateway and use `permitAll()` in the User Service security configuration. There are no `@PreAuthorize` annotations found in the inspected source.

Therefore, the architecture currently provides **edge authentication**, but not a strong service-level authorization boundary.

For production, services should independently validate trusted identity/context and enforce role-based permissions where required.

---

# 29. Docker Infrastructure

The Compose file includes:

- PostgreSQL
- pgAdmin
- Redis 7 Alpine
- ZooKeeper
- Kafka
- Kafdrop

Kafka exposes:

```text
localhost:9092
kafka:29092
```

The Compose network is:

```text
microservice-network
```

Persistent volumes are configured for:

- PostgreSQL
- pgAdmin
- Redis

---

# 30. Swagger / OpenAPI

Each service uses SpringDoc.

The Gateway exposes service documentation routes such as:

```text
/user-service/v3/api-docs
/incident-service/v3/api-docs
/rescue-service/v3/api-docs
/hospital-service/v3/api-docs
/logistics-service/v3/api-docs
/notification-service/v3/api-docs
```

This gives a centralized Swagger experience through the gateway.

---

# 31. Resilience4j

Several services contain Resilience4j dependencies and configuration.

Configured defaults include:

- sliding window = 10 calls
- minimum calls = 5
- failure threshold = 50%
- open-state wait = 10 seconds
- half-open calls = 3
- slow-call threshold = 2 seconds
- slow-call rate threshold = 50%

### Important distinction

The dependency/configuration exists, but the inspected source did **not** show actual `@CircuitBreaker` annotations.

Therefore:

> **Resilience4j is configured but does not currently appear to be actively applied to business methods.**

Do not describe this in an interview as "all downstream calls are protected by circuit breakers" unless you implement and verify that behavior.

---

# 32. Testing

Each service contains a Spring Boot application test class.

The tests currently appear primarily to be application-context tests rather than a comprehensive suite of:

- unit tests
- integration tests
- Kafka tests
- concurrency tests
- repository tests
- controller tests
- end-to-end disaster scenarios

This is a clear area for improvement.

---

# 33. Strongest Technical Features

The most technically defensible parts of the project are:

1. **Event-driven microservice communication with Kafka**
2. **Redis GEO for spatial operations**
3. **JWT authentication**
4. **Redis-backed JWT revocation**
5. **Rescue mission State Pattern**
6. **Automatic rescue-team assignment**
7. **Live rescue telemetry**
8. **Geofence-based mission transition**
9. **Pessimistic locking for hospital beds**
10. **Pessimistic locking for logistics inventory**
11. **FEFO emergency inventory allocation**
12. **Kafka retry + DLT handling**
13. **Hospital surge standby workflow**
14. **Automated inventory shortage → logistics dispatch workflow**
15. **Centralized API Gateway**
16. **Centralized Swagger routing**
17. **Notification audit trail**

These are the areas worth knowing deeply before an interview.

---

# 34. Complete Disaster Scenario

A realistic end-to-end flow implemented by the architecture is:

```text
1. Citizen reports a flood/fire/earthquake
                  │
                  ▼
2. API Gateway authenticates request
                  │
                  ▼
3. Incident Service stores incident
                  │
                  ▼
4. Duplicate detection checks nearby active incidents
                  │
                  ▼
5. IncidentCreatedEvent published
                  │
       ┌──────────┼───────────┐
       ▼          ▼           ▼
   Rescue      Hospital   Notification
       │          │           │
       │          │           └── find users within 5 km
       │          │               and email them
       │          │
       │          └── find hospitals within 15 km
       │              and publish standby alerts
       │
       └── find rescue department within 50 km
           │
           ▼
       create mission
           │
           ▼
       DISPATCHED
           │
           ▼
       GPS telemetry
           │
           ▼
       EN_ROUTE
           │
           ▼
       within geofence
           │
           ▼
       ON_SCENE
           │
           ▼
       rescue completed
           │
           ├── mission completed
           ├── release department capacity
           ├── publish rescue status
           └── publish victims extracted
                       │
                       ▼
                   Hospital workflow

6. Hospital inventory becomes critically low
                  │
                  ▼
7. InventoryShortageAlertEvent
                  │
                  ▼
8. Logistics selects nearest warehouse
                  │
                  ▼
9. FEFO inventory reservation
                  │
                  ▼
10. Assign vehicle
                  │
                  ▼
11. Create dispatch order
                  │
                  ▼
12. SupplyDispatchedEvent
             ┌────┴────┐
             ▼         ▼
         Hospital   Notification
             │         │
             ▼         ▼
          Restock   Email ETA
```

This is the core story of the project.

---

# 35. What Is NOT Currently Evident in the Source

These should not be claimed as implemented unless additional code exists outside the uploaded ZIP:

- Eureka Service Discovery
- Spring Cloud Config Server
- OpenFeign
- Kubernetes deployment manifests
- Prometheus/Grafana integration
- Zipkin distributed tracing
- AI/LLM functionality
- AI-based disaster impact prediction
- AI-based inventory demand forecasting
- route optimization using an external routing engine
- SMS/WhatsApp push notifications
- a frontend application

Some of these were discussed/planned for the project, but they are not supported by the inspected source tree.

---

# 36. Important Code/Configuration Issues Found

## Issue 1 - User Service configuration

The uploaded User Service `application.properties` appears to contain Gateway configuration and port `8080`.

This conflicts with the expected User Service port `8081`.

**Priority: very high.**

---

## Issue 2 - Secrets in configuration

The repository contains sensitive credentials/configuration in application properties, including:

- JWT secret
- SMTP credential

These should **not** remain in GitHub.

The SMTP credential should be considered compromised if it has ever been committed/pushed.

**Immediate action:**

1. Rotate/revoke the SMTP app password.
2. Rotate the JWT secret.
3. Remove secrets from Git history if they were pushed publicly.
4. Use environment variables or a secret manager.

The documentation deliberately does not reproduce the credentials.

---

## Issue 3 - Hard-coded service URLs

Notification Service directly references the User Service using a localhost URL.

That is acceptable for local development but weak for deployment.

A production architecture should use:

- service discovery,
- environment-based configuration,
- Docker DNS,
- or Kubernetes service names.

---

## Issue 4 - No service-level authorization

The gateway performs authentication, but business services do not appear to enforce role-level authorization consistently.

An authenticated user may therefore reach endpoints that should logically be restricted unless another layer handles authorization.

---

## Issue 5 - Resilience4j is mostly configuration

The project has Resilience4j dependencies/configuration, but the inspected source did not show actual circuit-breaker annotations.

Therefore the feature is currently closer to **prepared infrastructure** than a demonstrated resilience mechanism.

---

## Issue 6 - Hard-coded emergency radii/defaults

Examples include:

- 5 km notification radius
- 15 km hospital surge radius
- 50 km rescue search radius
- 50 m/100 m rescue geofence behavior
- default estimated casualty counts

These are reasonable demo defaults but should ideally become configurable policies.

---

## Issue 7 - Event consistency

The system performs database changes and then publishes Kafka events separately.

Without an Outbox Pattern, there is a possible failure window:

```text
DB commit succeeds
       │
       ▼
Kafka publish fails
       │
       ▼
Other services never receive event
```

For a serious distributed system, an **Transactional Outbox Pattern** would make event publication more reliable.

---

# 37. Design Patterns Present

## State Pattern

Used in rescue mission lifecycle management.

## Repository Pattern

Spring Data repositories abstract persistence.

## Service Layer

Business logic is separated from controllers.

## DTO Pattern

Request and response DTOs separate API contracts from entities.

## Event-Driven Architecture

Kafka events decouple services.

## Cache-aside / spatial indexing concept

Redis stores location indexes and frequently changing mission state while PostgreSQL remains the persistent source.

## Strategy-like selection

Nearest warehouse / nearest department / nearest hospital selection is based on distance ranking and filtering.

---

# 38. Why Kafka Makes Sense Here

Kafka is useful because an incident can trigger several independent actions:

```text
Incident Created
       │
       ├── Rescue
       ├── Hospital
       └── Notification
```

With synchronous REST calls, Incident Service would need to know about all three services.

With Kafka:

```text
Incident Service
      │
      ▼
 Kafka topic
      │
      ├── Rescue independently consumes
      ├── Hospital independently consumes
      └── Notification independently consumes
```

This provides lower coupling and allows consumers to process the event independently.

---

# 39. Why Redis GEO Makes Sense

Rescue telemetry can generate frequent location updates.

Persisting every GPS ping directly into PostgreSQL would create unnecessary database writes.

Instead:

```text
GPS → Redis GEO
```

and only meaningful state changes such as:

```text
EN_ROUTE → ON_SCENE
```

are persisted to PostgreSQL.

Redis is also appropriate for:

- nearby-user/hospital/rescue lookup
- temporary spatial state
- low-latency reads

---

# 40. Why Pessimistic Locking Matters

Disaster systems have high-concurrency operations.

Two examples:

### Hospital

Two patients attempt to consume the final ICU bed.

### Logistics

Two dispatch workers attempt to reserve the same inventory.

Pessimistic locking ensures these operations serialize around the critical database rows/batches.

---

# 41. Interview Explanation

## 30-second version

> "I built a disaster-management backend as a Spring Boot microservices system. It separates incident management, rescue coordination, hospital operations, emergency logistics, notifications, and user management. I use Kafka for asynchronous event-driven communication, Redis GEO for location-aware operations and rescue telemetry, PostgreSQL for service-owned transactional data, JWT for authentication, and an API Gateway as the entry point. A key workflow is that an incident can automatically trigger rescue-team assignment, nearby hospital standby alerts, and citizen notifications, while hospital inventory shortages can trigger automated logistics dispatch."

## 2-minute version

> "The system is centered around an event-driven disaster-response workflow. A citizen or operator creates an incident through the API Gateway. The Incident Service performs duplicate detection using a 50-meter geographic threshold and publishes an IncidentCreatedEvent to Kafka for valid incidents. Rescue, Hospital, and Notification services consume that event independently.
>
> Rescue Service finds the nearest available department within its configured search radius, assigns a mission, and tracks the mission using a State Pattern. GPS telemetry is stored in Redis GEO, and reaching the incident geofence transitions the mission from EN_ROUTE to ON_SCENE. Mission state changes are published back through Kafka.
>
> Hospital Service maintains beds, ICU capacity, admissions, and medical inventory. It uses Redis GEO to find nearby capable hospitals and pessimistic database locking to prevent concurrent bed-allocation races. When inventory falls below a critical threshold, it publishes a shortage event.
>
> Logistics consumes that event, selects a nearby warehouse, allocates stock using FEFO with pessimistic locking, assigns an available vehicle, creates a dispatch order, and publishes a supply-dispatched event. Hospital and Notification services consume that event to update inventory and notify operators.
>
> Authentication uses JWT, with Redis storing revoked tokens until their remaining expiry. The Gateway validates tokens before forwarding protected requests."

---

# 42. Questions You Should Be Able to Answer

### Architecture

1. Why microservices instead of a monolith?
2. Why Kafka?
3. Why not REST between every service?
4. What happens if Kafka is unavailable?
5. What happens if a consumer crashes?
6. Why separate databases?
7. How would you deploy this?

### Kafka

8. What is a consumer group?
9. Why does each service use its own consumer group?
10. What happens when a consumer fails?
11. What is a DLT?
12. How does retryable topic processing work?
13. What is message ordering?
14. What happens if the database commits but Kafka publishing fails?

### Redis

15. Why Redis GEO?
16. Why not PostgreSQL for all location queries?
17. What happens when Redis goes down?
18. Is Redis the source of truth?
19. Why cache mission status?

### Concurrency

20. Why pessimistic locking?
21. What race condition does hospital locking solve?
22. What race condition does FEFO inventory locking solve?
23. When would optimistic locking be better?

### Rescue

24. Why use the State Pattern?
25. How does GPS cause a state transition?
26. What is a geofence?
27. Why shouldn't every GPS ping be persisted?
28. How do you release department capacity?

### Security

29. How does JWT authentication work?
30. How does logout work with stateless JWT?
31. Why Redis for blacklist?
32. How are passwords stored?
33. Where should authorization be enforced?
34. What are the security risks of gateway-only authentication?

### Reliability

35. Where is Resilience4j actually used?
36. Why would you add a circuit breaker?
37. What is the Outbox Pattern?
38. How would you guarantee event delivery?
39. How would you make Kafka processing idempotent?

---

# 43. Recommended Next Improvements

If this is your flagship backend project, the highest-value improvements are:

### Priority 1

- Fix User Service configuration.
- Remove/rotate exposed credentials.
- Move secrets to environment variables.
- Add service-level authorization.
- Add proper automated tests.

### Priority 2

- Implement actual Resilience4j circuit breakers.
- Add Kafka idempotency.
- Add transactional outbox.
- Replace hard-coded radii/defaults with configuration.

### Priority 3

- Add service discovery/config management if genuinely needed.
- Add Prometheus/Grafana metrics.
- Add distributed tracing.
- Add Kubernetes manifests.
- Add integration tests with Kafka/PostgreSQL/Redis.

### Priority 4

- Add AI components only if they solve a real problem and can be explained technically.
- Add route optimization using an actual routing engine rather than claiming "AI route optimization."
- Add better notification channels.

---

# 44. Final Project Identity

The project is best described as:

> **An event-driven disaster-response coordination platform built with Java 21 and Spring Boot microservices, using Kafka for asynchronous workflows, Redis GEO for location-aware operations and real-time rescue tracking, PostgreSQL for transactional service data, JWT authentication, and automated hospital/logistics coordination.**

The most compelling technical story is not simply "I made a disaster management app."

It is:

> **"I designed a distributed system where one disaster event propagates through independent response domains, and each domain performs its own transactional, spatial, and asynchronous work."**

That is the story worth mastering for interviews.
