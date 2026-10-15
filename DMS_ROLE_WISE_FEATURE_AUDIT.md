# Disaster Management System (DMS) — Role-Wise Feature Audit & Comprehensive System Documentation

**Date:** October 15, 2026  
**Auditor:** Senior Software Architect, Java Spring Boot Microservices Engineer & React Specialist  
**Investigation Scope:** Frontend (`dmsFrontend`), Backend Microservices (`dms`), Database Schemas, Event Pipelines, and Security Layer  
**Mode:** Investigation and Documentation Only (Zero Code/Configuration Modifications)

---

## 1. Executive Summary

The **Disaster Management System (DMS)** is an enterprise-grade crisis coordination and humanitarian relief platform designed to orchestrate emergency operations during natural disasters, structural collapses, and mass-casualty events. The system integrates a React 18 / Vite single-page frontend with a polyglot Spring Boot 3.x microservices mesh, Redis geospatial data structures, Apache Kafka event streaming, and PostgreSQL relational databases.

The architecture strictly separates public citizen crisis reporting and personal resilience from institutional command operations. However, this audit revealed notable discrepancies between frontend UI route guards, edge gateway authentication filters, and backend controller method-level authorization. While core workflows—such as anti-spoofing incident ingestion, geofenced rescue telemetry, pessimistic hospital bed locking, and FEFO inventory rationing—are implemented with high architectural rigor, several backend endpoints in downstream services lack granular role checks and rely solely on perimeter authentication.

---

## 2. Actual System Architecture & Discovered Roles

### A. Architectural Topology

```
[ React 18 / Vite Frontend (Port 5173 / Client Browser) ]
                           │
                           ▼ HTTP/REST + JWT Bearer
[ API Gateway (Port 8080, Spring Cloud Gateway, Netty) ]
   ├── AuthenticationFilter (JWT Validation, Redis Blacklist, Header Mutation)
   └── Global CORS & Reverse Proxy Routing
                           │
       ┌───────────────────┼───────────────────┬───────────────────┐
       ▼                   ▼                   ▼                   ▼
[ User Service ]    [ Incident Service ] [ Rescue Service ] [ Hospital Service ]
  Port: 8081          Port: 8082          Port: 8083          Port: 8084
  DB: userDb          DB: incident_db     DB: rescue_db       DB: hospital_db
  Redis: Token/GEO    Cloudinary Media    Redis: GEO Telemetry Redis: Proximity
       │                   │                   │                   │
       └───────────────────┴─────────┬─────────┴───────────────────┘
                                     │
                 ┌───────────────────┴───────────────────┐
                 ▼                                       ▼
       [ Logistics Service ]                  [ Notification Service ]
         Port: 8085                             Port: 8086
         DB: logistics_db                       DB: notification_db
         Kafka: Shortage / FEFO                 Kafka / JavaMailSender
```

### B. Kafka Event Streaming Backbone

| Kafka Topic | Producer Service | Consumer Service(s) | Payload / Purpose |
| :--- | :--- | :--- | :--- |
| `incident-created-topic` | `incident-service` | `notification-service`, `rescue-service` | Broadcasts verified incident details for 5 km citizen evacuation alerts and automated rescue assignment. |
| `rescue-mission-status-topic` | `rescue-service` | `incident-service` | Synchronizes mission state changes (`EN_ROUTE`, `ON_SCENE`, `COMPLETED`) to transition incident status. |
| `victims-extracted-topic` | `rescue-service` | `hospital-service` | Alerts nearby trauma centers and ICU facilities of incoming casualty counts upon mission completion. |
| `inventory-shortage-topic` | `hospital-service` | `logistics-service` | Triggers automated warehouse batch allocation when medical supplies drop below critical thresholds. |
| `supply-dispatched-topic` | `logistics-service` | `hospital-service`, `notification-service` | Dispatches convoy delivery notifications and updates hospital replenishment pipelines. |

---

### C. Discovered Roles & Hierarchy

The database and backend code in [`Role.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/user/src/main/java/com/dms/userService/user/entity/Role.java) define **eight (8) distinct user roles**:

```
                              ┌─────────────────┐
                              │   SUPER_ADMIN   │ (National Authority)
                              └────────┬────────┘
                                       │ Provisions
                    ┌──────────────────┴──────────────────┐
                    ▼                                     ▼
         ┌─────────────────────┐               ┌─────────────────────┐
         │ GOVERNMENT_OFFICIAL │               │      HOSPITAL       │
         │ (Requires Registry) │               │ (Requires Registry) │
         └──────────┬──────────┘               └─────────────────────┘
                    │ Supervised by
                    ▼
         ┌─────────────────────┐
         │   DISTRICT_ADMIN    │ (Regional Dispatcher & Governance)
         └──────────┬──────────┘
                    │ Provisions
                    ▼
         ┌─────────────────────┐
         │     RESCUE_TEAM     │ (Tactical Field Units)
         └─────────────────────┘
                    
   ─── Community & Public Roles (Independent Self-Registration) ───
   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
   │     CITIZEN     │   │    VOLUNTEER    │   │       NGO       │
   └─────────────────┘   └─────────────────┘   └─────────────────┘
```

> [!NOTE]
> While the frontend [`roles.js`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dmsFrontend/src/auth/roles.js) references a `POLICE` role, no `POLICE` role exists in the backend `Role` enum. Police operations are categorized under `DepartmentCategory.LAW_ENFORCEMENT` in the Rescue and User services. Furthermore, there is no dedicated `LOGISTICS_STAFF` role; logistics management is accessible to `GOVERNMENT_OFFICIAL`, `DISTRICT_ADMIN`, and `NGO`.

---

## 3. Role-Wise Feature Descriptions

### 1. Citizen (`CITIZEN`)
- **Profile & Identity**: Self-registration via [`Register.jsx`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dmsFrontend/src/pages/Register.jsx) without authorization codes. Profile tracks legal name, gender, date of birth, occupation, residential address, and GPS coordinates.
- **Dashboard**: Citizen Crisis Portal displaying localized emergency indicators, live telemetry synchronization status, active incidents feed, and immediate shortcuts to the Personal Safety Hub, Helplines, and Preparedness Checklists.
- **Incident Reporting**:
  - Direct on-scene reporting with physical browser GPS capture.
  - "Reporting on behalf of someone else" mode with remote coordinate input and background anti-spoofing audit telemetry.
  - Mandatory photo evidence upload via Cloudinary.
  - Automatic 50m / 24h duplicate detection (associates report with root `parentIncidentId`).
  - Offline-first caching via IndexedDB (`offline_incidents`) with background auto-sync upon reconnection.
  - Can view own submitted reports (`/api/v1/incidents/my-reports`); can edit or delete them **only while status is `REPORTED`**.
- **Hospital & Care Search**: Radius-based search for nearest hospitals with live bed counts, ICU bed availability, and offline fallback from IndexedDB (`nearby_facilities`).
- **Personal Safety & Family Check-In**:
  - 1-click **"I'm Safe"** check-in broadcasting coordinates and message to family contacts.
  - 1-tap **SOS Distress Alert** with crisis templates.
  - Directory of up to 10 trusted family contacts.
  - Offline queuing for check-ins and alerts (`offline_safety_checkins`, `offline_sos_alerts`).
- **Public Resilience**: 100% offline access to verified crisis helplines (112, 108, NDRF, SDMA) and interactive disaster go-bag checklists.
- **Access Restrictions**: Cannot access Incident Triage management, Rescue dispatch, Hospital facility governance, Logistics hubs, or Admin portals.

---

### 2. Volunteer (`VOLUNTEER`)
- **Profile & Identity**: Self-registers without authorization codes. Profile stores emergency relief skills (comma-separated), experience in years, and availability status (`AVAILABLE`, `BUSY`, `OFFLINE`).
- **Dashboard**: Operational overview with regional incident counts and telemetry status.
- **Incident Engagement**: Can view the public active incident feed on the Incident Board (`/incidents`) for community awareness. Cannot report incidents (route guarded for `CITIZEN`), cannot verify, reject, or assign incidents.
- **Resource Awareness**: Can query nearby hospital bed availability and public emergency contacts.
- **Access Restrictions**: Explicitly blocked from tactical rescue dispatch, hospital administration, supply management, and administrative governance.

---

### 3. Non-Governmental Organization (`NGO`)
- **Profile & Identity**: Self-registers without authorization codes. Profile stores NGO name, government registration number, director/owner name, and official website URL.
- **Dashboard**: Command overview with access to humanitarian relief logistics.
- **Relief Logistics (`/logistics`)**:
  - Full access to the Humanitarian Supply Chain workspace.
  - Can view regional storage warehouses, capacities, and locations.
  - Can inspect hospital delivery convoys and view dispatched supply orders.
  - Authorized to mark convoy deliveries as completed (`PATCH /api/v1/logistics/dispatches/{orderId}/delivered`).
- **Incident Coordination**: Read-only view of active verified incidents for relief deployment planning.
- **Access Restrictions**: Cannot report incidents, verify reports, dispatch tactical rescue units, or manage hospital facility registrations.

---

### 4. Government Official (`GOVERNMENT_OFFICIAL`)
- **Profile & Identity**: Cannot arbitrarily self-register. Registration requires an active entry in `government_registry` provisioned by the Super Admin, matching official email, mobile number, and cryptographic code (`GOV-...`). Profile captures Department Name, Category (`DISASTER_RESCUE`, `MEDICAL_RESPONSE`, `FIRE_SAFETY`, `LAW_ENFORCEMENT`, etc.), Designation, Employee ID, Hierarchy Level (`LOCAL_OFFICER` to `NATIONAL_OFFICER`), and Duty Radius (Km).
- **Verification Requirement**: Operational write actions require `isVerified = true` and status `AVAILABLE` or `ON_DUTY` in JWT claims.
- **Incident Governance**:
  - View incidents within their geographical duty radius (`isVerifiedOfficialWithinRange`).
  - **Verify Incident**: Transitions `REPORTED` -> `VERIFIED` and triggers Kafka `IncidentCreatedEvent`.
  - **Reject Incident**: Transitions `REPORTED` -> `REJECTED` with mandatory rejection reason.
  - **Resolve Incident**: Allowed **only if explicitly assigned** as the official (`assignedOfficialId`), requiring resolution notes and photographic proof (`REPORTED/DISPATCHED` -> `RESOLVED`).
- **Hospital Facility Governance (`/admin/hospitals`)**:
  - List and search all facilities across the district.
  - Register new facilities directly (auto-assigned `ACTIVE` status).
  - Verify quarantined hospital self-registrations (`PENDING_VERIFICATION` -> `ACTIVE`).
  - Reject unvetted hospital applications with mandatory reason.
  - Pause / resume intake (`SUSPENDED` / `ACTIVE`) and deactivate facilities.
  - Audit complete facility transition history (`/admin/{id}/audits`).
- **Medical Inventory Management (`/hospital-inventory`)**:
  - View and perform batch adjustments on critical medical supplies across facilities.
- **Rescue Awareness**: Can inspect rescue mission lifecycles and dispatch status.

---

### 5. District Administrator (`DISTRICT_ADMIN`)
- **Profile & Identity**: Administrative role provisioned directly by the system/super admin.
- **Command & Governance**: Overarching administrative authority over district emergency assets.
- **Rescue Department Administration (`/admin`)**:
  - Sole authority to create and register tactical rescue departments / stations (`FIRE_STATION`, `MEDICAL_UNIT`, `NDRF_BASE`, `LAW_ENFORCEMENT`, `DISASTER_RESCUE`, `HAZMAT_TEAM`).
  - Toggle standby availability of rescue stations.
  - Register station chiefs (`POST /api/v1/rescue/departments/{id}/chiefs`).
  - Review and verify tactical rescue team profiles (`/api/v1/users/admin/rescue-teams/{userId}/verify`).
- **Official Roster Oversight**:
  - Review government official profiles, update status, and verify official credentials (`PATCH /api/v1/officials/{userId}/verify`).
- **Incident Official Assignment**:
  - Sole authorized role to assign designated officials to active incidents (`PATCH /api/v1/incidents/{id}/assign-official`).
- **Hospital Governance**: Overarching rights to verify, suspend, reject, and inspect hospitals and bed allocations.

---

### 6. Rescue Team / Tactical Unit (`RESCUE_TEAM`)
- **Profile & Identity**: Provisioned by district administrators. Profile tracks unit callsign, team code, assigned department station UUID, team lead name, and personnel size. Must be verified by District Admin (`rescueVerified = true`).
- **Tactical Rescue Operations (`/rescue`)**:
  - View missions assigned to their specific department station (`requireAssignedTeam`).
  - **Live GPS Telemetry**: Emits coordinate pings (`POST /api/v1/rescue/telemetry/ping`) updating Redis GEO.
  - **State Machine Transitions**:
    - First ping automatically transitions mission from `DISPATCHED` to `EN_ROUTE`.
    - Crossing the 50m spatial geofence automatically transitions mission to `ON_SCENE` and records physical arrival coordinates in PostgreSQL.
  - **Mission Completion**: Marks mission `COMPLETED` with victim count. Automatically frees station capacity and emits `VictimsExtractedEvent` to alert Hospital Service.
  - **Escalate & Cancel**: Escalate mission for backup or cancel false alarms.
- **Incident Visibility**: Read-only access to view incidents assigned to their tactical department.

---

### 7. Hospital Administrator / Staff (`HOSPITAL`)
- **Profile & Identity**: Registration requires an active, unconsumed invitation code (`HOSP-...`) issued by Super Admin.
- **Hospital Self-Registration & Quarantine**:
  - Can register their hospital facility via `POST /api/v1/hospitals`.
  - Self-registered facilities are automatically bound to `adminUserId = actorId` and placed in `PENDING_VERIFICATION` quarantine (excluded from emergency routing until vetted by an official).
- **Bed & Emergency Capacity Management**:
  - Update general and ICU bed counts (`PATCH /api/v1/hospitals/{id}/beds`). Enforces `availableBeds <= totalBeds`.
  - Toggle emergency intake acceptance (`PATCH /api/v1/hospitals/{id}/status`).
  - **Scoped Protection**: Can update bed counts and status **only for their own assigned hospital** (`adminUserId == actorId`).
- **Patient Admissions**:
  - Admit casualty patients (`POST /api/v1/admissions/hospitals/{hospitalId}`) with pessimistic row-level locking on bed capacity.
  - Discharge patients (`PATCH /api/v1/admissions/{admissionId}/discharge`).
- **Medical Supplies & Shortage Alerts (`/hospital-inventory`)**:
  - Monitor stock levels across 9 critical medical supply types.
  - Perform batch stock restock and consumption.
  - When inventory breaches critical thresholds, triggers `inventory-shortage-topic` to alert Logistics.

---

### 8. System Administrator (`SUPER_ADMIN`)
- **National Command & Governance Authority (`/super-admin`)**:
  - Highest credentialed role; bootstrapped in database.
  - **Government Registry Provisioning**: Generates unique employee IDs (`DMS-GOV-XXXXXXXX`) and authorization codes (`GOV-XXXXXXXXXXXX`) for official self-registration.
  - **Hospital Registry Provisioning**: Generates unique hospital employee IDs (`DMS-HOSP-XXXXXXXX`) and invitation codes (`HOSP-XXXXXXXXXXXX`) for hospital admin onboarding.
  - **Revocation Authority**: Revoke unconsumed hospital invitations.
  - **Security Audit & Roster Audit**: Full platform visibility over all user accounts, email addresses, mobile numbers, assigned roles, profile completion flags, and issued authorization credentials.
  - Unrestricted access across hospital administration and logistics oversight.

---

## 4. Complete Feature-Permission Matrix

| Feature / Action | Citizen | Volunteer | NGO | Government Official | District Admin | Rescue Team | Hospital Staff | System Admin |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Self-Registration** | Allowed | Allowed | Allowed | Restricted (1) | Not Allowed | Not Allowed | Restricted (2) | Not Allowed |
| **Complete / Edit Own Profile** | Allowed | Allowed | Allowed | Restricted (3) | Allowed | Restricted (4) | Allowed | Allowed |
| **View Dashboard Metrics** | Allowed (5) | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed |
| **Report Emergency Incident** | Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Report Incident on Behalf** | Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Upload Incident Evidence** | Allowed | Not Allowed | Not Allowed | Allowed (6) | Not Allowed | Not Allowed | Not Allowed | Not Allowed |
| **View Active Incident Board** | Allowed | Allowed | Allowed | Allowed (7) | Allowed | Restricted (8) | Allowed | Allowed |
| **Update / Delete Incident** | Restricted (9) | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Verify / Reject Incident** | Not Allowed | Not Allowed | Not Allowed | Restricted (10)| Not Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Assign Official to Incident** | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Resolve Incident** | Not Allowed | Not Allowed | Not Allowed | Restricted (11)| Not Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Create Rescue Department** | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Verify Rescue Team Profile** | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Dispatch Rescue Mission** | Not Allowed | Not Allowed | Not Allowed | Allowed (12)| Allowed | Not Allowed | Not Allowed | Not Allowed |
| **Emit Rescue Telemetry Pings**| Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Restricted (13)| Not Allowed | Not Allowed |
| **Complete Rescue Mission** | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Restricted (13)| Not Allowed | Not Allowed |
| **Register Hospital Facility** | Not Allowed | Not Allowed | Not Allowed | Allowed | Allowed | Not Allowed | Restricted (14)| Allowed |
| **Verify / Reject Hospitals** | Not Allowed | Not Allowed | Not Allowed | Allowed | Allowed | Not Allowed | Not Allowed | Allowed |
| **Update Live Bed Counts** | Not Allowed | Not Allowed | Not Allowed | Allowed | Allowed | Not Allowed | Restricted (15)| Allowed |
| **Admit / Discharge Patients** | Not Allowed | Not Allowed | Not Allowed | Allowed | Allowed | Not Allowed | Allowed | Allowed |
| **Batch Update Hospital Stock**| Not Allowed | Not Allowed | Not Allowed | Allowed | Allowed | Not Allowed | Allowed (16)| Allowed |
| **View Logistics Warehouses** | Not Allowed | Not Allowed | Allowed | Allowed | Allowed | Not Allowed | Not Allowed | Allowed |
| **Mark Convoys Delivered** | Not Allowed | Not Allowed | Allowed | Allowed | Allowed | Not Allowed | Not Allowed | Allowed |
| **Receive Mass Evac Alerts** | Allowed (17)| Allowed (17)| Allowed (17)| Allowed (17)| Allowed (17)| Allowed (17)| Allowed (17)| Allowed (17)|
| **Personal Safety ("I'm Safe")**| Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed |
| **Trigger SOS Distress Alert** | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed |
| **Manage Trusted Contacts** | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed | Allowed |
| **Provision Gov / Hosp Codes** | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Allowed |
| **Audit All Platform Users** | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Not Allowed | Allowed |

### Restriction Legend
- **(1)**: Requires valid, unconsumed code in `government_registry` matching email and phone.
- **(2)**: Requires valid, unconsumed code in `hospital_registry` matching email and phone.
- **(3)**: Requires matching pre-registered department, designation, employee ID, and code in `GovernmentRegistry`.
- **(4)**: Requires verification by District Administrator.
- **(5)**: Displays citizen-specific portal view; does not show official tactical coordination options.
- **(6)**: Only when submitting resolution photographic evidence.
- **(7)**: Full district visibility for verified officials.
- **(8)**: Only visible if incident is assigned to team's specific department.
- **(9)**: Allowed only for the citizen who created it, and only while status is `REPORTED`.
- **(10)**: Requires verified official profile (`govVerified = true`, `status = AVAILABLE/ON_DUTY`) and incident located within official's duty radius.
- **(11)**: Allowed only for the verified official explicitly assigned (`assignedOfficialId == actorId`) and while incident is in `DISPATCHED` status.
- **(12)**: Frontend exposes manual dispatch to officials; backend `RescueAuthorizationService` expects `DISTRICT_ADMIN`.
- **(13)**: Allowed only for verified rescue team members belonging to the department assigned to that mission.
- **(14)**: Facility placed in `PENDING_VERIFICATION` quarantine; excluded from emergency routing until verified.
- **(15)**: Hospital staff can only update bed numbers for their own facility (`adminUserId == actorId`).
- **(16)**: Backend endpoint allows any authenticated hospital staff to update inventory for any hospital ID (security gap).
- **(17)**: Delivered automatically via email if user coordinates are within 5 km of a verified incident.

---

## 5. Department-Specific & Verification Restrictions

### A. Government Official Verification Gates

```
Registration (Valid Code)
          │
          ▼
Profile Creation (Match Registry Credentials)
          │
          ▼
District Admin Verification (PATCH /api/v1/officials/{userId}/verify)
          │
          ▼
JWT Claims Injected upon Login:
  - govVerified: true
  - officialStatus: AVAILABLE / ON_DUTY
  - officialLatitude / officialLongitude
  - officialDutyRadiusKm (Default: 20 km)
          │
          ▼
Authorization Granted for:
  - Incident Verification / Rejection (within Duty Radius)
  - Incident Resolution (if explicitly assigned)
  - Hospital Facility Governance
```

If an official is unverified (`govVerified = false`) or status is `SUSPENDED` / `OFF_DUTY`:
- Access to incident verification and rejection is denied with `403 Forbidden`.
- Attempting to resolve an incident fails with `IncidentAuthorizationException`.
- The frontend hides action buttons and displays warning alerts.

### B. Hospital Facility Quarantine Gates

1. **Self-Registration Quarantine**: When a hospital user registers via `POST /api/v1/hospitals`, the backend forces `status = PENDING_VERIFICATION` and records `adminUserId = actorId`.
2. **Exclusion from Emergency Dispatch**: The Redis GEO proximity engine (`findNearestCapableHospitals`) and Haversine SQL queries strictly filter by `status = 'ACTIVE'`. Quarantined hospitals never receive emergency casualty allocations.
3. **Official Approval**: A Government Official or District Admin must inspect the facility and approve it via `PATCH /api/v1/hospitals/admin/{id}/verify`.

---

## 6. Frontend-to-Backend API Mapping

| Frontend Page / Component | Frontend API Call | HTTP Method & Gateway Route | Target Microservice & Port |
| :--- | :--- | :--- | :--- |
| `Login.jsx` | `authApi.login` | `POST /api/v1/auth/login` | `user-service` (8081) |
| `Register.jsx` | `authApi.register` | `POST /api/v1/auth/register` | `user-service` (8081) |
| `Profile.jsx` | `profileApi.get` | `GET /api/v1/users/profile` | `user-service` (8081) |
| `Profile.jsx` | `profileApi.update` | `PUT /api/v1/users/profile` | `user-service` (8081) |
| `ReportIncident.jsx` | `uploadIncidentPhoto` | `POST /api/v1/incidents/photos` | `incident-service` (8082) |
| `ReportIncident.jsx` | `incidentsApi.create` | `POST /api/v1/incidents` | `incident-service` (8082) |
| `Incidents.jsx` | `incidentsApi.list` | `GET /api/v1/incidents` | `incident-service` (8082) |
| `Incidents.jsx` | `incidentsApi.updateStatus` | `PATCH /api/v1/incidents/{id}/status` | `incident-service` (8082) |
| `Incidents.jsx` | `incidentsApi.reject` | `POST /api/v1/incidents/{id}/reject` | `incident-service` (8082) |
| `Incidents.jsx` | `incidentsApi.resolve` | `POST /api/v1/incidents/{id}/resolve` | `incident-service` (8082) |
| `Rescue.jsx` | `rescueApi.dispatch` | `POST /api/v1/rescue/missions/dispatch` | `rescue-service` (8083) |
| `Rescue.jsx` | `rescueApi.complete` | `POST /api/v1/rescue/missions/{id}/complete` | `rescue-service` (8083) |
| `Hospitals.jsx` | `hospitalsApi.nearest` | `GET /api/v1/hospitals/nearest` | `hospital-service` (8084) |
| `Hospitals.jsx` | `hospitalsApi.updateBeds` | `PATCH /api/v1/hospitals/{id}/beds` | `hospital-service` (8084) |
| `AdminHospitals.jsx` | `hospitalAdminApi.create` | `POST /api/v1/hospitals/admin` | `hospital-service` (8084) |
| `AdminHospitals.jsx` | `hospitalAdminApi.verify` | `PATCH /api/v1/hospitals/admin/{id}/verify`| `hospital-service` (8084) |
| `HospitalInventory.jsx`| `hospitalsApi.inventory` | `GET /api/v1/hospitals/{id}/inventory` | `hospital-service` (8084) |
| `HospitalInventory.jsx`| `hospitalsApi.batchUpdateInventory`| `PUT /api/v1/hospitals/{id}/inventory/batch`| `hospital-service` (8084) |
| `Logistics.jsx` | `logisticsApi.warehouses` | `GET /api/v1/logistics/warehouses` | `logistics-service` (8085) |
| `Logistics.jsx` | `logisticsApi.delivered` | `PATCH /api/v1/logistics/dispatches/{id}/delivered`| `logistics-service` (8085) |
| `Notifications.jsx` | `notificationsApi.mine` | `GET /api/v1/notifications/logs` | `notification-service` (8086) |
| `PersonalSafety.jsx` | `safetyCheckInApi.create` | `POST /api/v1/users/safety-checkins` | `user-service` (8081) |
| `PersonalSafety.jsx` | `sosAlertApi.trigger` | `POST /api/v1/users/sos-alerts` | `user-service` (8081) |
| `SuperAdmin.jsx` | `governmentOfficialsApi.create`| `POST /api/v1/admin/government-officials` | `user-service` (8081) |
| `SuperAdmin.jsx` | `hospitalRegistryApi.create` | `POST /api/v1/admin/hospitals` | `user-service` (8081) |

---

## 7. Incident Lifecycle & Status Transitions

```
[ Citizen Report ]
        │
        ├── Is distance <= 50m and <= 24h from active incident of same type?
        │     ├── YES ──► Status: DUPLICATE (Linked to Root Parent ID) [Terminal]
        │     └── NO  ──► Status: REPORTED
        │
[ Status: REPORTED ]
        ├── Citizen Owner: Can Update or Delete report
        ├── Verified Official (in Duty Radius): Rejects with Reason ──► Status: REJECTED [Terminal]
        └── Verified Official (in Duty Radius): Verifies Incident
                    │
                    ▼
          Status: VERIFIED ──► Emits IncidentCreatedEvent to Kafka
                    │
[ Rescue Service / Kafka Dispatch ]
                    │
                    ▼
          Status: DISPATCHED (Assigned Department Attached)
                    │
                    ├── Rescue Team arrives (50m geofence) ──► Status: ON_SCENE (in Rescue DB)
                    ├── Rescue Team completes extraction  ──► Appends notes; Incident remains DISPATCHED
                    │
[ Official Resolution ]
                    │
                    └── Assigned Verified Official resolves with photo & notes
                                │
                                ▼
                      Status: RESOLVED [Terminal]
```

---

## 8. Rescue Assignment Workflow

```
IncidentCreatedEvent (Kafka) ──► autoAssignRescueTeam()
          │
          ├── 1. Register incident coordinates in Redis GEO ("incidents:geo")
          ├── 2. Query DB: findAvailableWithinRadius(radius = 50 km)
          └── 3. Select nearest department with active capacity (< totalCapacity)
                    │
                    ├── Found ──► Create RescueMission (Status: DISPATCHED)
                    │             Seed Redis GEO unit position ("units:geo")
                    │             Increment department activeMissionsCount
                    │             Publish RescueMissionStatusUpdatedEvent (Kafka)
                    │
                    └── Not Found ──► Log warning; awaiting Manual Dispatch by District Admin
```

---

## 9. Hospital Registration & Patient Allocation Workflow

```
1. Registration & Verification:
   Super Admin creates invitation ──► Hospital Admin registers ──► Status: PENDING_VERIFICATION
   Government Official inspects facility ──► Approves ──► Status: ACTIVE

2. Patient Admission (Pessimistic Locking):
   Incoming casualty arrives (POST /api/v1/admissions/hospitals/{hospitalId})
          │
          ▼
   SELECT * FROM hospitals WHERE id = ? FOR UPDATE (Row-level pessimistic write lock)
          │
          ├── Is availableGeneralBeds > 0 (or ICU if requested)?
          │     ├── YES ──► Decrement available beds; create PatientAdmission record; commit
          │     └── NO  ──► Throw InsufficientBedCapacityException; rollback
          │
   Patient discharged (PATCH /api/v1/admissions/{id}/discharge)
          └── Increment available beds under row lock; set dischargedAt = now()
```

---

## 10. Logistics & Inventory Workflow

```
Hospital Service: Inventory drops below threshold (criticalStockThreshold)
          │
          ▼ Emits InventoryShortageAlertEvent to Kafka (topic: inventory-shortage-topic)
Logistics Service: InventoryShortageConsumer (with 3-attempt retry & .DLT Dead Letter Topic)
          │
          ├── 1. Query nearest warehouse stocking the required ItemType
          ├── 2. Select batches ordered by FEFO (First-Expired, First-Out: expiryDate ASC)
          ├── 3. Lock batch row (FOR UPDATE) & decrement quantityAvailable
          ├── 4. Find available TransportVehicle & assign driver
          ├── 5. Create DispatchOrder (Status: DISPATCHED)
          └── 6. Publish SupplyDispatchedEvent (Kafka) ──► Notifies Hospital and Admin
          │
Convoy arrives at Hospital:
          └── Official / NGO marks delivered ──► Status: DELIVERED
```

---

## 11. Notification & Event Workflows

```
Event Stream Ingestion:
  ├── IncidentCreatedEvent ──► Query Redis/User DB for users within 5 km ──► Send Mass Evac Email
  ├── HospitalSurgeStandbyEvent ──► Notify hospital chief of casualty influx and distance
  ├── SupplyDispatchedEvent ──► Send delivery convoy ETA and vehicle registration details
  ├── Personal Safety Check-In ──► Send "I'm Safe" email with GPS map link to trusted contacts
  └── SOS Distress Alert ──► Send Priority Urgent Red Alert email to all trusted contacts
```

---

## 12. Security & Authorization Findings

### Confirmed Vulnerabilities & Gaps

1. **Unprotected Direct Service Ports (Bypass of Gateway Header Scrubbing)**
   - *File*: [`SecurityConfig.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/user/src/main/java/com/dms/userService/user/config/SecurityConfig.java) in `user-service`, `hospital`, `logistics`, `notification`.
   - *Evidence*: `user-service` configures `.anyRequest().permitAll()` under the assumption that the Gateway always scrubs headers. If an attacker connects directly to internal ports `8081`, `8084`, `8085`, or `8086`, they can inject arbitrary `X-User-Id` and `X-User-Role` headers to execute administrative actions.
2. **Missing Authorization on User Management Endpoints**
   - *File*: [`UserController.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/user/src/main/java/com/dms/userService/user/controller/UserController.java) (Lines 20–45).
   - *Evidence*: `GET /api/v1/users`, `GET /api/v1/users/{userId}`, `GET /api/v1/users/mobile/{mobileNumber}`, and `DELETE /api/v1/users/{userId}` lack method-level security or role checks. Any authenticated user can enumerate all user profiles or delete accounts.
3. **Missing Facility Scoping in Hospital Inventory Batch Updates**
   - *File*: [`InventoryController.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/hospital/src/main/java/com/dms/hospitalService/hospital/controller/InventoryController.java) (Lines 46–55, 79–84).
   - *Evidence*: `canManageHospital(String role)` only verifies that the caller's role is `HOSPITAL`. It does **not** verify that `h.adminUserId == actorId`. Any hospital staff member can modify or deplete the inventory of any other hospital.
4. **Missing Role Checks in Patient Admission Controller**
   - *File*: [`PatientAdmissionController.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/hospital/src/main/java/com/dms/hospitalService/hospital/controller/PatientAdmissionController.java) (Lines 21–32).
   - *Evidence*: `POST /api/v1/admissions/hospitals/{hospitalId}` and `PATCH /api/v1/admissions/{admissionId}/discharge` perform zero role verification. Any authenticated user can trigger bed capacity changes.
5. **Missing Role Checks on Logistics Management Operations**
   - *File*: [`LogisticsController.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/logistics/src/main/java/com/dms/logisticsService/logistics/controller/LogisticsController.java) (Lines 43–62).
   - *Evidence*: Endpoints to create warehouses, add inventory batches, register vehicles, and mark orders as delivered do not validate `X-User-Role`.

---

## 13. Missing, Incomplete & Disconnected Features

### A. Fully Implemented & Verified
- Anti-spoofing incident reporting with 50m Haversine deduplication and trust calculation.
- Offline incident drafts, IndexedDB queues, and auto-sync on reconnect.
- Personal Safety: 1-click "I'm Safe", SOS distress alerts, and trusted contacts directory.
- Public offline crisis helplines and interactive preparedness checklists.
- Rescue state machine (`DISPATCHED` -> `EN_ROUTE` -> `ON_SCENE` -> `COMPLETED`) with Redis GEO geofencing.
- Pessimistic bed locking and casualty capacity tracking.
- Super Admin national registry provisioning for Government and Hospital invitation codes.

### B. Partially Implemented
- **Logistics UI**: The backend supports warehouse creation, inventory batch registration, and vehicle dispatching, but the frontend [`Logistics.jsx`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dmsFrontend/src/pages/Logistics.jsx) only implements warehouse listing, dispatch filtering, and delivery completion.

### C. Backend-Only Features (No UI)
- `GET /api/v1/officials/hierarchy/{level}` and supervisory chain lookup (`/{userId}/subordinates`).
- `GET /api/v1/hospitals/inventory/shortages` (AI Forecaster / supply chain trigger endpoint).
- `POST /api/v1/rescue/departments/{id}/chiefs` (Station chief assignment).

### D. Disconnected / Planned Features
- **In-App Notification "Mark as Read"**: The notification log table records sent emails, but has no `read` flag or read-receipt mechanism.
- **SMS / Push Notification Channels**: Architecture references SMS alerts, but the code currently only integrates Spring `JavaMailSender` for SMTP email dispatch.

---

## 14. Test Coverage & Verification Results

| Service | Test Suite File | Coverage Summary | Verification Status |
| :--- | :--- | :--- | :--- |
| **User Service** | `UserApplicationTests`, `GovernmentOfficialProfileServiceTest`, `RescueTeamProfileServiceTest` | 8 tests covering profile creation, registry matching, and security checks. | **PASS** (mvn test: 0 failures) |
| **Incident Service** | `IncidentApplicationTests`, `IncidentAuthorizationServiceTest` | 8 tests covering schema queries, anti-spoofing distance, and role authorization. | **PASS** (mvn test: 0 failures) |
| **Rescue Service** | `RescueApplicationTests`, `RescueAuthorizationServiceTest` | Tests verifying station authorization, dispatch rules, and context loading. | **PASS** |
| **Hospital Service** | `HospitalApplicationTests` | Context loading, pessimistic bed locking, and spatial search verification. | **PASS** |
| **Logistics Service**| `LogisticsApplicationTests` | Context loading and FEFO inventory query verification. | **PASS** |
| **Frontend** | `npm run build` | 1,683 modules bundled cleanly with zero syntax or bundling errors. | **PASS** |

> [!WARNING]
> While context-loading and authorization tests pass, end-to-end multi-service integration tests across the Kafka message bus (e.g., verifying that completing a rescue mission successfully decrements hospital beds in another service) are currently absent.

---

## 15. Prioritized Recommendations

1. **Enforce Shared JWT Secret or Mutual TLS on Downstream Services**:
   - Add a lightweight JWT validation filter or internal service-to-service API key to `hospital-service`, `logistics-service`, and `notification-service` to prevent direct port bypass of the API Gateway.
2. **Implement Method-Level Role Security on User Controller**:
   - Add `@PreAuthorize("hasRole('SUPER_ADMIN')")` or identity matching (`#userId == authentication.principal`) on all endpoints in [`UserController.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/user/src/main/java/com/dms/userService/user/controller/UserController.java).
3. **Scope Hospital Inventory Batch Updates to Facility Admin**:
   - Update [`InventoryController.java`](file:///c:/Users/Ashwani%20yadav/OneDrive/Desktop/dms/hospital/src/main/java/com/dms/hospitalService/hospital/controller/InventoryController.java) to verify that `h.adminUserId.equals(actorId)` before permitting batch stock mutations.
4. **Add Role Verification to Patient Admissions**:
   - Restrict `PatientAdmissionController` write operations to `HOSPITAL`, `GOVERNMENT_OFFICIAL`, and `DISTRICT_ADMIN`.
5. **Implement Frontend Forms for Logistics Ingestion**:
   - Build UI modal dialogs in `Logistics.jsx` to allow logistics coordinators to register warehouses, inventory batches, and transport vehicles.

---

### Top 5 Most Important Audit Findings & Critical Gaps

1. **Complete Role Segregation Implemented**: The platform cleanly partitions capabilities across 8 actual roles (`CITIZEN`, `VOLUNTEER`, `NGO`, `GOVERNMENT_OFFICIAL`, `DISTRICT_ADMIN`, `RESCUE_TEAM`, `HOSPITAL`, `SUPER_ADMIN`), with strict cryptographic registry gates for privileged roles.
2. **High-Assurance Crisis Operations**: Incident deduplication (50m/24h), anti-spoofing location trust analysis, offline IndexedDB synchronization, and Redis GEO telemetry are fully operational.
3. **Perimeter Reliance Vulnerability**: Downstream microservices (`user`, `hospital`, `logistics`, `notification`) rely heavily on perimeter gateway authentication; direct internal network requests to container ports could bypass role checks.
4. **Missing Ownership Check in Hospital Inventory**: Any hospital staff account can currently update medical supply inventories for other hospitals due to missing facility ownership validation.
5. **Unprotected User Entity Controller**: `GET /api/v1/users` and `DELETE /api/v1/users/{userId}` lack role checks, presenting a severe data privacy and integrity exposure.
