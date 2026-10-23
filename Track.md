# Disaster Management System (DMS) - Project Memory & Tracker

## 1. Project Overview
DMS is an enterprise disaster coordination and emergency relief platform comprising a Spring Boot microservices backend and a React/Vite single-page frontend.

### Services Architecture
- **API Gateway (`api-gateway`)** - Port 8080: Gateway routing, JWT validation, Redis token blacklisting, Reactive Resilience4j CircuitBreakers with HTTP 503 fallback controller.
- **User Service (`user`)** - Port 8081: Identity, authentication, role profiles, credentials registry, Redis GEO user spatial indexing, RestClient HTTP timeouts, Resilience4j CircuitBreakers on notification & rescue client outbound calls.
- **Incident Service (`incident`)** - Port 8082: Incident intake, 50m/24h deduplication, Cloudinary photo uploads protected with Resilience4j CircuitBreaker, Kafka events, SLA 3h overdue escalation.
- **Rescue Service (`rescue`)** - Port 8083: Rescue stations, mission state machines (DISPATCHED -> EN_ROUTE -> ON_SCENE -> COMPLETED/CANCELLED), telemetry, and unit assignments.
- **Hospital Service (`hospital`)** - Port 8084: Proximity routing, pessimistic bed locking (`findByIdForUpdate`), patient admission/triage, and medical inventory.
- **Logistics Service (`logistics`)** - Port 8085: Warehouses, FEFO inventory allocation with pessimistic batch locking, convoy dispatches, delivery confirmations.
- **Notification Service (`notification`)** - Port 8086: Event-driven alerts via Spring Mail, RestClient HTTP timeouts, Resilience4j CircuitBreaker on User Service client calls.
- **Frontend (`dmsFrontend`)** - Port 5173: React 18 single-page application built with Vite and Tailwind/custom CSS.

### Infrastructure & Database
- PostgreSQL container on port 5432 with isolated databases:
  `userDb`, `incident_db`, `rescue_db`, `hospital_db`, `logistics_db`, `notification_db`.
- Redis on port 6379 for token blacklisting and geospatial indexing.
- Apache Kafka on port 9092 for inter-service event streaming (retry topics and DLTs preserved).

---

## 2. Seeded Test Dataset (Sahjanwa & Gorakhpur)
All accounts use standard testing passwords:
- **Citizen (Sahjanwa)**: `citizen.sahjanwa@dms.gov.in` / `Citizen@123`
- **Super Admin**: `superadmin@dms.gov.in` / `Admin@123`
- **District Admins (Gorakhpur, District@123)**:
  - Universal Verifier: `districtadmin.gkp@dms.gov.in` (`DEPARTMENT_MANAGEMENT`)
  - Fire & Safety: `districtadmin.fire.gkp@dms.gov.in` (`FIRE_SAFETY`)
  - Transit & Highway: `districtadmin.accident.gkp@dms.gov.in` (`ACCIDENT`)
  - Disaster & Flood: `districtadmin.disaster.gkp@dms.gov.in` (`DISASTER_MANAGEMENT`)
  - Hospital Administration: `districtadmin.medical.gkp@dms.gov.in` (`MEDICAL_MANAGEMENT`)
  - Emergency Casualty Command: `districtadmin.medresp.gkp@dms.gov.in` (`MEDICAL_RESPONSE`)
- **Government Officials (On-Scene Responders, Official@123)**:
  - Fire Official: `fire.sahjanwa@dms.gov.in` (`FIRE_SAFETY`)
  - Police Official: `police.sahjanwa@dms.gov.in` (`ACCIDENT`)
  - Medical Officer: `medical.sahjanwa@dms.gov.in` (`MEDICAL_RESPONSE`)
  - Disaster Officer: `disaster.sahjanwa@dms.gov.in` (`DISASTER_MANAGEMENT`)
- **Hospitals (5 Facilities around Sahjanwa & Gorakhpur)**:
  - Community Health Centre (CHC) Sahjanwa
  - AIIMS Gorakhpur Emergency Trauma Centre
  - BRD Medical College & Hospital
  - Gorakhpur District Hospital (Sadar Hospital)
  - Sahjanwa Emergency Care & Trauma Clinic

---

## 3. Incident Domain Policy & Scoping
- `FIRE_SAFETY`: Verifies and manages `FIRE` and `HAZMAT` incidents.
- `DISASTER_MANAGEMENT`: Verifies and manages `FLOOD` and `COLLAPSE` incidents.
- `ACCIDENT`: Verifies and manages `ACCIDENT` incidents.
- `MEDICAL_RESPONSE`: Verifies and manages `MEDICAL` incidents.
- `DEPARTMENT_MANAGEMENT`: Universal verifier for all incident types.

---

## 4. Recent Completed Work

### Task: Implement Resilience4j Circuit Breakers & Comprehensive Automated Tests in DMS Backend
- **Problem**: DMS inter-service synchronous calls and downstream external integrations (Cloudinary, User Service, Notification Service, Rescue Service) lacked circuit breakers and connection/read timeouts, risking cascading thread pool exhaustion. Automated test coverage across failure transitions and domain logic was incomplete.
- **Status**: IMPLEMENTED & TESTED
- **What was changed**:
  1. **API Gateway (`api-gateway`)**:
     - Added `spring-cloud-starter-circuitbreaker-reactor-resilience4j`.
     - Created `FallbackController.java` returning HTTP 503 Service Unavailable JSON payload.
     - Configured reactive circuit breaker instances with 50% failure rate, 10s wait duration in open state.
     - Created `FallbackControllerTest.java` (unit tests for reactive 503 fallback responses).
  2. **User Service (`user`)**:
     - Created `RestClientConfig.java` configuring `SimpleClientHttpRequestFactory` with 2s connect and 3s read timeouts.
     - Annotated `NotificationServiceClientHttp.java` with `@CircuitBreaker(name = "notificationClient", fallbackMethod = "...")` returning 0 count on outage.
     - Annotated `RescueDepartmentDirectoryHttp.java` with `@CircuitBreaker(name = "rescueDepartmentClient", fallbackMethod = "existsFallback")` returning false on outage.
     - Created `NotificationServiceClientResilienceTest.java` and `RescueDepartmentDirectoryResilienceTest.java` testing CLOSED -> OPEN -> fallback execution -> HALF_OPEN recovery.
  3. **Notification Service (`notification`)**:
     - Configured connect/read timeouts (2s connect / 3s read) in `RestClientConfig.java`.
     - Annotated `UserServiceClient.java` with `@CircuitBreaker(name = "userServiceClient", fallbackMethod = "fetchNearbyUserEmailsFallback")` returning empty list on outage.
     - Configured instance properties in `application.properties`.
     - Removed duplicate actuator dependency in `pom.xml`.
     - Created `UserServiceClientResilienceTest.java` testing breaker tripping and graceful recovery.
  4. **Incident Service (`incident`)**:
     - Annotated `CloudinaryImageService.java` with `@CircuitBreaker(name = "cloudinaryClient", fallbackMethod = "uploadFallback")` failing fast with HTTP 503 without corrupting evidence with fake URLs.
     - Refactored `switch (action)` in `IncidentAuthorizationService.java` to standard `if-else` to eliminate the compiler synthetic `$1.class` linking issue.
     - Configured instance properties in `application.properties`.
     - Created `CloudinaryImageServiceResilienceTest.java` (testing successful uploads, validation bypass, 502/503 trip to OPEN, and HALF_OPEN recovery).
     - Created `IncidentServiceUnitTest.java` (testing unique incident creation with MATCH trust level, 50m spatial duplicate detection & linking to root parent, REMOTE_REPORT trust level, and VERIFIED status transitions with Kafka event publication).
  5. **Rescue Service (`rescue`)**:
     - Created `RescueMissionServiceUnitTest.java` (testing mission completion from `ON_SCENE`, invalid state transition rejection from `DISPATCHED`, cancellation resource cleanup, GPS telemetry arrival triggers to `ON_SCENE`).
  6. **Hospital Service (`hospital`)**:
     - Created `HospitalBedManagementUnitTest.java` (testing bed capacity updates, pessimistic `findByIdForUpdate` lock interaction, rejection of available beds exceeding total capacity, and verification activation).
  7. **Logistics Service (`logistics`)**:
     - Created `InventoryAllocationServiceUnitTest.java` (testing First-Expired, First-Out (FEFO) batch allocation, pessimistic batch locking, vehicle assignment and EN_ROUTE status update, stock exhaustion handling, and Kafka event broadcast).
  8. **Kafka & Transaction Preservation**:
     - Existing Kafka retry topics (`@RetryableTopic`), dead letter topics (DLTs), and deserializers were preserved untouched.
     - Pessimistic row-locking (`PESSIMISTIC_WRITE`) and database transaction boundaries remain intact.
- **Files changed / created**:
  - `api-gateway/pom.xml`
  - `api-gateway/src/main/resources/application.properties`
  - `api-gateway/src/main/java/com/dms/gateway/controller/FallbackController.java`
  - `api-gateway/src/test/java/com/dms/gateway/FallbackControllerTest.java`
  - `user/src/main/java/com/dms/userService/user/config/RestClientConfig.java`
  - `user/src/main/java/com/dms/userService/user/service/impl/NotificationServiceClientHttp.java`
  - `user/src/main/java/com/dms/userService/user/service/impl/RescueDepartmentDirectoryHttp.java`
  - `user/src/main/resources/application.properties`
  - `user/src/test/java/com/dms/userService/user/resilience/NotificationServiceClientResilienceTest.java`
  - `user/src/test/java/com/dms/userService/user/resilience/RescueDepartmentDirectoryResilienceTest.java`
  - `notification/pom.xml`
  - `notification/src/main/java/com/dms/notificationService/config/RestClientConfig.java`
  - `notification/src/main/java/com/dms/notificationService/client/UserServiceClient.java`
  - `notification/src/main/resources/application.properties`
  - `notification/src/test/java/com/dms/notificationService/resilience/UserServiceClientResilienceTest.java`
  - `incident/src/main/java/com/dms/incident/service/CloudinaryImageService.java`
  - `incident/src/main/java/com/dms/incident/service/IncidentAuthorizationService.java`
  - `incident/src/main/resources/application.properties`
  - `incident/src/test/java/com/dms/incident/resilience/CloudinaryImageServiceResilienceTest.java`
  - `incident/src/test/java/com/dms/incident/service/IncidentServiceUnitTest.java`
  - `rescue/src/test/java/com/dms/rescueService/rescue/service/RescueMissionServiceUnitTest.java`
  - `hospital/src/test/java/com/dms/hospitalService/hospital/service/HospitalBedManagementUnitTest.java`
  - `logistics/src/test/java/com/dms/logisticsService/logistics/service/InventoryAllocationServiceUnitTest.java`
- **Testing performed & Actual Results**:
  - `api-gateway`: `mvn test` -> 3 tests passed, 0 failures.
  - `user`: `mvn test` -> 16 tests passed, 0 failures.
  - `notification`: `mvn test` -> 4 tests passed, 0 failures.
  - `incident`: `mvn test` -> 19 tests passed, 0 failures.
  - `rescue`: `mvn test` -> 15 tests passed, 0 failures.
  - `hospital`: `mvn test` -> 18 tests passed, 0 failures.
  - `logistics`: `mvn test` -> 6 tests passed, 0 failures.
  - **Total**: 81 tests executed across all 7 backend modules, 100% BUILD SUCCESS.
- **Result**: IMPLEMENTED & TESTED.

---

### Task: Complete Removal of Emergency Preparedness Checklists
- **Problem**: User requested removing Emergency Preparedness Checklists from all profiles and the entire frontend application.
- **Status**: IMPLEMENTED & TESTED
- **Files changed**:
  - `dmsFrontend/src/pages/PreparednessChecklist.jsx` (deleted)
  - `dmsFrontend/src/pages/Profile.jsx`
  - `dmsFrontend/src/components/ProfileDropdown.jsx`
  - `dmsFrontend/src/components/AppShell.jsx`
  - `dmsFrontend/src/components/LandingNavbar.jsx`
  - `dmsFrontend/src/pages/Landing.jsx`
  - `dmsFrontend/src/App.jsx`
  - `dmsFrontend/src/utils/offlineCareStorage.js`
- **Result**: PASSED.

---

### Task: End-to-End Application Testing, Bug Discovery & Exception/Validation Hardening
- **Problem**:
  1. **User Authentication**: Invalid login credentials caused HTTP 500 internal server errors instead of HTTP 401 Unauthorized with clean JSON error messages.
  2. **Incident Validation & Errors**: Submitting invalid incident requests returned generic errors without structured `validationErrors` maps. Incident not found returned `"User Not Found"`.
  3. **Incident Deduplication Serialization**: `IncidentResponse` DTO lacked the `parentIncidentId` field, preventing clients from knowing which parent incident a duplicate was linked to.
  4. **Hospital District Administration**: Universal District Admins (`DEPARTMENT_MANAGEMENT`) were rejected when managing hospital beds. Bed updates exceeding total capacity returned HTTP 404 instead of HTTP 400 Bad Request.
  5. **Logistics Dispatch Serialization**: Marking dispatches as delivered and retrieving hospital orders caused HTTP 500 errors due to Hibernate ByteBuddy proxy serialization failures on lazy warehouse and vehicle relationships.
  6. **Rescue Department & Mission Authorization**: Domain-specific District Admins (e.g. Fire & Safety) received empty lists for departments due to strict ID filtering without domain fallback, and were blocked with HTTP 403 Forbidden when querying auto-dispatched rescue missions for their incidents.
- **Status**: IMPLEMENTED & TESTED
- **What was changed**:
  1. **User Service**:
     - Updated `GlobalExceptionHandler.java`: Added `@ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})` returning HTTP 401 with `"Invalid email or password."`. Added `@ExceptionHandler(IllegalArgumentException.class)`.
  2. **Incident Service**:
     - Updated `ErrorResponse.java`: Added `validationErrors` map support.
     - Updated `GlobalExceptionHandler.java`: Added `@ExceptionHandler(MethodArgumentNotValidException.class)` extracting field error maps (HTTP 400), added `@ExceptionHandler(HttpMessageNotReadableException.class)` (HTTP 400), and corrected incident not found message to `"Incident Not Found"`.
     - Updated `IncidentResponse.java`: Added `parentIncidentId` field.
  3. **Hospital Service**:
     - Updated `HospitalController.java`: Allowed Universal District Admins (`DEPARTMENT_MANAGEMENT`) in `isAuthorizedHospitalAdmin`.
     - Updated `GlobalExceptionHandler.java`: Differentiated `IllegalArgumentException` (HTTP 404 if "not found", HTTP 400 otherwise), added `HttpMessageNotReadableException` (HTTP 400).
  4. **Logistics Service**:
     - Updated `DispatchOrder.java`, `Warehouse.java`, `TransportVehicle.java`: Added `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` to prevent ByteBuddy proxy serialization crashes.
  5. **Rescue Service**:
     - Updated `RescueAuthorizationService.java`: Aligned `requireMissionView` and `requireDepartmentView` with `requireDispatcher` to recognize authorized department categories (`FIRE_SAFETY`, `DISASTER_MANAGEMENT`, `ACCIDENT`, `DISASTER_RESCUE`, `DEPARTMENT_MANAGEMENT`), and exposed `departmentCategory()`.
     - Updated `DepartmentServiceImpl.java`: In `getAllDepartments()`, allowed Universal District Admins to see all departments and added domain-scoped fallback for category-specific district admins.
     - Updated `LocationTestDataInitializer.java`: Associated fire and rescue units with corresponding district admin IDs.
- **Files changed**:
  - `user/src/main/java/com/dms/userService/user/exception/GlobalExceptionHandler.java`
  - `incident/src/main/java/com/dms/incident/exception/ErrorResponse.java`
  - `incident/src/main/java/com/dms/incident/exception/GlobalExceptionHandler.java`
  - `incident/src/main/java/com/dms/incident/dto/response/IncidentResponse.java`
  - `hospital/src/main/java/com/dms/hospitalService/hospital/controller/HospitalController.java`
  - `hospital/src/main/java/com/dms/hospitalService/hospital/exception/GlobalExceptionHandler.java`
  - `logistics/src/main/java/com/dms/logisticsService/logistics/entity/DispatchOrder.java`
  - `logistics/src/main/java/com/dms/logisticsService/logistics/entity/Warehouse.java`
  - `logistics/src/main/java/com/dms/logisticsService/logistics/entity/TransportVehicle.java`
  - `rescue/src/main/java/com/dms/rescueService/rescue/security/RescueAuthorizationService.java`
  - `rescue/src/main/java/com/dms/rescueService/rescue/service/impl/DepartmentServiceImpl.java`
  - `rescue/src/main/java/com/dms/rescueService/rescue/config/LocationTestDataInitializer.java`
- **Testing performed & Actual Results**:
  - Module Unit Tests:
    - `api-gateway`: 3/3 passed
    - `user`: 16/16 passed
    - `incident`: 19/19 passed
    - `rescue`: 15/15 passed
    - `hospital`: 18/18 passed
    - `logistics`: 6/6 passed
    - `notification`: 4/4 passed
    - **Total**: 81/81 unit tests passed (100% BUILD SUCCESS).
  - Comprehensive Live End-to-End Test Suite (`e2e_test_suite.py`):
    - **Total**: 32/32 tests PASSED (100% SUCCESS) across live Docker infra (Postgres, Redis, Kafka) and all 7 running microservices.
    - Verified Section A: Authentication, invalid credentials 401, unauthenticated/forged JWT rejection, token blacklisting.
    - Verified Section B: Incident DTO validation 400 with field errors, GPS trust calculation, duplicate linking with `parentIncidentId`, cross-department verification restriction, authorized verification, reason-enforced rejection.
    - Verified Section C: Rescue department fetching, Kafka `IncidentCreatedEvent` auto-dispatch to nearest unit, mission inspection by authorized district admin.
    - Verified Section D: Nearest hospital spatial discovery, universal district admin bed management, bed capacity limits (HTTP 400), patient triage admission and discharge.
    - Verified Section E: Logistics warehouse querying, dispatch delivery with serialized lazy relations, role-based access control.
    - Verified Section F: Gateway fallback 503 circuit breaker endpoint and Actuator health checks across all services.
- **Result**: IMPLEMENTED & TESTED.

---

### Task: Frontend Incident Verification & Domain Category Scoping Hardening
- **Problem**:
  1. In `dmsFrontend/src/api/incidents.js`, `incidentsApi.verify` was creating an empty `FormData` payload and explicitly setting `Content-Type: multipart/form-data`. In Axios, explicitly setting this header without the boundary parameter caused Spring Boot multipart resolution rejections on PATCH requests.
  2. In `dmsFrontend/src/auth/roles.js`, `getIncidentTypesForCategory` omitted `HAZMAT` for `FIRE_SAFETY`, and omitted `COLLAPSE` and `OTHER` for `DISASTER_MANAGEMENT` and `DEPARTMENT_MANAGEMENT`, causing relevant incidents to be hidden from authorized district command boards when scoped by category.
- **Status**: IMPLEMENTED & TESTED
- **What was changed**:
  1. `dmsFrontend/src/api/incidents.js`: Refactored `verify(id)` to send `status=VERIFIED` cleanly as query params (`{ params: { status: 'VERIFIED' } }`) directly mapping to the backend `@RequestParam IncidentStatus status`, and removed manual `multipart/form-data` header in `resolve` so Axios sets boundary automatically.
  2. `dmsFrontend/src/auth/roles.js`: Updated `getIncidentTypesForCategory` to include `HAZMAT` for `FIRE_SAFETY`, `COLLAPSE` and `OTHER` for `DISASTER_MANAGEMENT` / `DISASTER_RESCUE`, and all types for `DEPARTMENT_MANAGEMENT`, aligning with `IncidentDepartmentPolicy.java`.
- **Files changed**:
  - `dmsFrontend/src/api/incidents.js`
  - `dmsFrontend/src/auth/roles.js`
- **Testing performed & Results**:
  - `npm run build` in `dmsFrontend` completed cleanly with 0 errors.
  - End-to-end verified with live backend: Citizen reported FIRE incident around Sahjanwa -> Fire District Admin (`districtadmin.fire.gkp@dms.gov.in`) verified it using frontend API request -> Status updated to `VERIFIED` -> Kafka auto-dispatched to nearest rescue post (`Pipiganj Rescue Post`) with status `DISPATCHED`.
  - Comprehensive test suite `e2e_test_suite.py`: 32/32 tests passed (100%).
- **Result**: IMPLEMENTED & TESTED.

---

### Task: Fix Misleading Session Expiration Message on Failed Login
- **Problem**: When a user entered an incorrect password or invalid credentials during login, the frontend displayed `"Your session has expired. Please sign in again."` instead of the actual credential failure message (`"Invalid email or password."`).
- **Status**: IMPLEMENTED & TESTED
- **What was changed**:
  1. `dmsFrontend/src/api/client.js`:
     - Updated the Axios response error interceptor to distinguish authentication requests (`/api/v1/auth/login`, `/api/v1/auth/register`) from authenticated API calls.
     - Prevented triggering the global `dms:unauthorized` session-clearing event when the request itself was an authentication/login request.
     - Updated error precedence to use the clean backend message (`"Invalid email or password."`) or fallback to `"Invalid email or password. Please verify your credentials."` when HTTP 401 is returned on authentication requests, while preserving `"Your session has expired. Please sign in again."` for genuine session/JWT expiration on protected routes.
     - Also prioritized clean backend error messages for domain errors (400, 403, 404, 409) over generic status code text.
- **Files changed**:
  - `dmsFrontend/src/api/client.js`
- **Important implementation decisions**:
  - Maintained backward compatibility and full safety: stack traces and 500 errors continue to be masked with generic user-friendly messages.
  - Genuine session expirations on protected application routes still trigger `clearSession()` and dispatch `dms:unauthorized` to redirect the user to login.
- **Testing performed**:
  - Executed automated unit tests covering wrong password on login, login without response body, session expiration on protected endpoints, custom 403 authorization messages, and 500 stack trace sanitization.
  - Ran production Vite build (`npm run build` in `dmsFrontend`): built cleanly with 0 errors.
- **Result**: IMPLEMENTED & TESTED.
- **Remaining issues**: None.

---

### Task: Fix Flyway Table Pre-Existence Migration Failures and Hospital Builder Collection Dereference on Fresh Databases
- **Problem**:
  1. Starting `user` service on a fresh empty `userDb` caused Flyway migration `V12` to crash with `relation "government_official_profiles" does not exist` because Flyway runs before Hibernate creates the tables, and `V12` was missing an existence check guard.
  2. Starting `incident` service on a fresh empty `incident_db` caused Flyway migration `V5` to crash with `relation "tbl_incidents" does not exist` for the same reason.
  3. Starting `hospital` service failed with `HibernateException: A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance: Hospital.admissions` during `LocationTestDataInitializer` boot due to Lombok `@Builder` defaulting collections to `null` and a redundant double-save inside the same transaction.
- **Status**: IMPLEMENTED & TESTED
- **What was changed**:
  1. `user/src/main/resources/db/migration/V12__update_government_official_profiles_department_category_check.sql`:
     - Wrapped the `ALTER TABLE government_official_profiles` block in `DO $$ BEGIN IF to_regclass('public.government_official_profiles') IS NOT NULL THEN ... END IF; END $$;` to align with migrations `V1`, `V2`, `V5`, `V7`, `V8`, `V10`.
  2. `incident/src/main/resources/db/migration/V5__add_overdue_notified_to_incidents.sql`:
     - Wrapped the `ALTER TABLE tbl_incidents` block in `DO $$ BEGIN IF to_regclass('public.tbl_incidents') IS NOT NULL THEN ... END IF; END $$;`.
  3. `hospital/src/main/java/com/dms/hospitalService/hospital/entity/Hospital.java`:
     - Added `@Builder.Default` to `specialities`, `inventories`, and `admissions` collections so `Hospital.builder().build()` never generates `null` collections.
  4. `hospital/src/main/java/com/dms/hospitalService/hospital/config/LocationTestDataInitializer.java`:
     - Refactored `seed()` to instantiate the `Hospital` in `.orElseGet(...)` without persisting inside the lambda, followed by administrator binding and a single clean `hospitals.save(hospital)`.
- **Files changed**:
  - `user/src/main/resources/db/migration/V12__update_government_official_profiles_department_category_check.sql`
  - `incident/src/main/resources/db/migration/V5__add_overdue_notified_to_incidents.sql`
  - `hospital/src/main/java/com/dms/hospitalService/hospital/entity/Hospital.java`
  - `hospital/src/main/java/com/dms/hospitalService/hospital/config/LocationTestDataInitializer.java`
- **Testing performed & Results**:
  - `hospital`: `mvn test` -> 18/18 tests passed (100% BUILD SUCCESS, `HospitalApplicationTests.contextLoads` passed).
  - `user`: `mvn test` -> 16/16 tests passed (100% BUILD SUCCESS, Flyway migration V12 executed, `DistrictAdminFlowTest` and `UserApplicationTests` passed).
  - `incident`: `mvn test` -> 19/19 tests passed (100% BUILD SUCCESS, Flyway migration V5 applied cleanly).
- **Result**: IMPLEMENTED & TESTED.
- **Remaining issues**: None.

---

### Task: Restrict Rescue Departments Administration Exclusively to Department Management District Admins
- **Problem**: In `dmsFrontend/src/auth/roles.js`, `canManageRescueStations` allowed multiple District Admin categories (`DISASTER_RESCUE`, `DISASTER_MANAGEMENT`, `ACCIDENT`, `FIRE_SAFETY`) to see and administer Rescue Departments (`/admin`), rather than restricting it solely to District Admins with `DEPARTMENT_MANAGEMENT` category.
- **Status**: IMPLEMENTED & TESTED
- **What was changed**:
  1. `dmsFrontend/src/auth/roles.js`:
     - Updated `canManageRescueStations(role, departmentCategory)` to strictly require `role === 'DISTRICT_ADMIN' && cat === 'DEPARTMENT_MANAGEMENT'` (alongside `SUPER_ADMIN`).
     - Scoped sidebar navigation link ("Rescue Departments" in `AppShell.jsx`), profile menu item ("Rescue Department Governance" in `ProfileDropdown.jsx`), and route protection for `/admin` ("Rescue Departments Administration" in `App.jsx`).
- **Files changed**:
  - `dmsFrontend/src/auth/roles.js`
- **Testing performed & Results**:
  - Verified role logic matrix in Node (`DEPARTMENT_MANAGEMENT` -> `true`, `FIRE_SAFETY` -> `false`, `ACCIDENT` -> `false`, `DISASTER_MANAGEMENT` -> `false`, `MEDICAL_RESPONSE` -> `false`, `SUPER_ADMIN` -> `true`, `CITIZEN` -> `false`).
  - Production Vite build (`npm run build` in `dmsFrontend`): built cleanly with 0 errors.
- **Result**: IMPLEMENTED & TESTED.
- **Remaining issues**: None.

---

## 5. Known Issues & Remaining Work
1. **3-Hour SLA Overdue Escalations**:
   - Automated escalation notifications to District Admins when incidents remain incomplete > 3 hours.
