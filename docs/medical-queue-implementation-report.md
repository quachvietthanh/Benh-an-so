# Medical Queue — Technical Implementation Report

## 1. Database Migrations

### V9 — `V9__create_medical_queue_tables.sql`

**File:** `V9__create_medical_queue_tables.sql`

#### Table: `medical_queue`

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `id` | `BINARY(16)` | PK | UUID |
| `patient_id` | `BINARY(16)` | FK → `patients(id)`, NOT NULL | |
| `doctor_id` | `BINARY(16)` | FK → `users(id)` | null cho đến khi được gọi |
| `room_number` | `VARCHAR(10)` | | |
| `queue_number` | `INT` | NOT NULL | Số thứ tự trong ngày (reset mỗi ngày) |
| `status` | `VARCHAR(30)` | NOT NULL, CHECK | `WAITING`, `SKIPPED`, `IN_PROGRESS`, `WAITING_FOR_RESULT`, `COMPLETED`, `CANCELLED` |
| `priority_level` | `VARCHAR(20)` | NOT NULL, DEFAULT 'REGULAR', CHECK | `EMERGENCY`, `APPOINTMENT`, `REGULAR` |
| `notes` | `TEXT` | | |
| `checked_in_at` | `TIMESTAMP` | | null; set khi create (WAITING) |
| `called_at` | `TIMESTAMP` | | null; set khi call() |
| `started_at` | `TIMESTAMP` | | null; set khi call() |
| `waiting_for_result_at` | `TIMESTAMP` | | null; set khi sendToWaitingForResult() |
| `completed_at` | `TIMESTAMP` | | null; set khi complete() |
| `cancelled_at` | `TIMESTAMP` | | null; set khi cancel() |
| `cancel_reason` | `VARCHAR(500)` | | |
| `created_by` | `BINARY(16)` | FK → `users(id)`, NOT NULL | |
| `created_at` | `TIMESTAMP` | NOT NULL | |
| `updated_at` | `TIMESTAMP` | NOT NULL | |

#### Original Indexes (V9)

| Index | Columns | Purpose |
|-------|---------|---------|
| `idx_medical_queue_created_at` | `created_at` | Sort/filter by time |
| `idx_medical_queue_status` | `status` | Filter active queues |
| `idx_medical_queue_patient` | `patient_id` | Lookup by patient |
| `idx_medical_queue_doctor` | `doctor_id` | Lookup by doctor |
| `idx_medical_queue_created_by` | `created_by` | Audit trail |
| `idx_medical_queue_room_status` | `room_number, status` | **Composite** — primary query pattern |
| `idx_medical_queue_priority_status` | `priority_level, status` | Priority-based queuing |

---

### V10 — `V10__optimize_medical_queue.sql`

**File:** `V10__optimize_medical_queue.sql`

#### New Columns

| Column | Type | Notes |
|--------|------|-------|
| `version` | `BIGINT` | Default `0`. **Optimistic Locking** — tự động tăng khi update, chống lost update. |
| `queue_date` | `DATE` | **Generated column** — `CAST(created_at AS DATE) STORED`. Dùng cho UNIQUE constraint. |

#### New UNIQUE Constraint

| Constraint | Columns | Purpose |
|-----------|---------|---------|
| `uk_room_queue_date` | `(room_number, queue_number, queue_date)` | **Chống trùng số thứ tự** trong cùng một ngày tại một phòng khám. Khi 2 request đồng thời cùng lấy `max+1`, cái thứ 2 sẽ bị `DataIntegrityViolationException` → AddToQueueService retry. |

#### Optimized Indexes

| Index | Columns | Purpose | Type |
|-------|---------|---------|------|
| `idx_queue_sorting` | `(room_number, status, priority_level, checked_in_at)` | Query queue theo phòng, ưu tiên EMERGENCY trước, theo thời gian check-in | **New** |
| ~~`idx_medical_queue_room_status`~~ | ~~`(room_number, status)`~~ | Covered by `idx_queue_sorting` | **Dropped** |
| ~~`idx_medical_queue_priority_status`~~ | ~~`(priority_level, status)`~~ | Covered by `idx_queue_sorting` | **Dropped** |
| `idx_medical_queue_created_at` | `created_at` | Sort/filter by time | Kept |
| `idx_medical_queue_status` | `status` | Filter active queues | Kept |
| `idx_medical_queue_patient` | `patient_id` | Lookup by patient | Kept |
| `idx_medical_queue_doctor` | `doctor_id` | Lookup by doctor | Kept |
| `idx_medical_queue_created_by` | `created_by` | Audit trail | Kept |

---

### V11 — `V11__update_queue_enums_and_constraints.sql`

**File:** `V11__update_queue_enums_and_constraints.sql`

#### What changed

1. **Thêm `SKIPPED`** vào CHECK constraint của cột `status`
2. **Thêm `APPOINTMENT`** vào CHECK constraint của cột `priority_level`

#### Updated CHECK Constraints

```sql
ALTER TABLE medical_queue
  DROP CHECK medical_queue_status_check,
  DROP CHECK medical_queue_priority_level_check;

ALTER TABLE medical_queue
  ADD CONSTRAINT medical_queue_status_check
    CHECK (status IN ('WAITING','SKIPPED','IN_PROGRESS',
                      'WAITING_FOR_RESULT','COMPLETED','CANCELLED')),
  ADD CONSTRAINT medical_queue_priority_level_check
    CHECK (priority_level IN ('EMERGENCY','APPOINTMENT','REGULAR'));
```

---

## 2. REST API Endpoints

Base path: `/api/v1/queue`

| Method | Endpoint | Auth (Role) | Request Body / Params | Response | Description |
|--------|----------|-------------|-----------------------|----------|-------------|
| **POST** | `/api/v1/queue` | ADMIN, RECEPTIONIST | `AddToQueueRequest` | `201` → `MedicalQueueResponse` | Thêm bệnh nhân vào hàng đợi, auto-số thứ tự + tự động phát hiện APPOINTMENT priority |
| **POST** | `/api/v1/queue/call-next` | ADMIN, DOCTOR | `CallNextRequest` | `200` → `MedicalQueueResponse` | Gọi bệnh nhân kế tiếp từ WAITING/SKIPPED lên IN_PROGRESS |
| **PUT** | `/api/v1/queue/{id}/status` | ADMIN, DOCTOR | `UpdateQueueStatusRequest` | `200` → `MedicalQueueResponse` | Chuyển trạng thái queue (hỗ trợ SKIPPED) |
| **GET** | `/api/v1/queue/room/{roomNumber}` | Authenticated | `?status=&page=&size=` | `200` → `PageResponse<MedicalQueueResponse>` | DS hàng đợi theo phòng (phân trang, sắp xếp priority) |
| **GET** | `/api/v1/queue/doctor/{doctorId}` | ADMIN, DOCTOR | `?status=&page=&size=` | `200` → `PageResponse<MedicalQueueResponse>` | DS hàng đợi theo bác sĩ (phân trang, sắp xếp priority) |
| **GET** | `/api/v1/queue/count` | Authenticated | `?roomNumber=&doctorId=&status=` | `200` → `Long` | Đếm số lượng |

### Request DTOs

#### `AddToQueueRequest`
```java
public record AddToQueueRequest(
    @NotNull UUID patientId,
    @NotNull PriorityLevel priorityLevel,
    @NotNull String roomNumber,
    UUID doctorId                       // optional - gán bác sĩ ngay khi tạo
) {}
```

#### `CallNextRequest`
```java
public record CallNextRequest(
    @NotNull UUID doctorId,
    @NotNull String roomNumber
) {}
```

#### `UpdateQueueStatusRequest`
```java
public record UpdateQueueStatusRequest(
    @NotNull QueueStatus newStatus,
    UUID doctorId,
    String cancelReason
) {}
```

#### `PageResponse<T>` (Generic Pagination DTO)
```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static <T> PageResponse<T> of(
            List<T> content, int page, int size, long totalElements
    ) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
```

#### `MedicalQueueResponse`
```java
public record MedicalQueueResponse(
    UUID id, UUID patientId, String patientName,
    UUID doctorId, String doctorName,
    String roomNumber, int queueNumber,
    QueueStatus status, PriorityLevel priorityLevel,
    String notes,
    Instant checkedInAt, Instant calledAt, Instant startedAt,
    Instant waitingForResultAt, Instant completedAt,
    Instant cancelledAt, String cancelReason,
    Instant createdAt, Instant updatedAt
) {}
```

---

## 3. State Machine (Workflow)

### State Diagram

```
                    ┌──────────────────────────┐
                    │         WAITING           │ ◄── Khởi tạo (create)
                    │  (checked_in_at = now)    │
                    └───┬──────────┬────────────┘
                        │          │
              ┌─────────▼──┐  ┌────▼───────────┐
              │  SKIPPED   │  │   IN_PROGRESS   │
              │            │  │(called,started) │
              └──┬─────────┘  └───┬──────────┬──┘
                 │ (resume)       │          │
                 └──────► IN_PROGRESS        │
                                     ┌──────▼────────┐
                                     │ WAITING_FOR    │
                                     │ _RESULT        │
                                     └───┬───────────┘
                                         │ (resume)
                                         └──► IN_PROGRESS

    WAITING ──────────────────────────────────► CANCELLED
    SKIPPED ──────────────────────────────────► CANCELLED
    IN_PROGRESS ──────────────────────────────► CANCELLED
    WAITING_FOR_RESULT ──────────────────────► CANCELLED

    IN_PROGRESS ─────────────────────────► COMPLETED
    WAITING_FOR_RESULT ──────────────────► COMPLETED
```

### Allowed Transitions

| From ↓ / To → | SKIPPED | IN_PROGRESS | WAITING_FOR_RESULT | COMPLETED | CANCELLED |
|---------------|:-------:|:-----------:|:------------------:|:---------:|:---------:|
| **WAITING** | ✅ (skip) | ✅ (call) | ❌ | ❌ | ✅ (cancel) |
| **SKIPPED** | — | ✅ (resume) | ❌ | ❌ | ✅ (cancel) |
| **IN_PROGRESS** | ❌ | — | ✅ (sendToResult) | ✅ (complete) | ✅ (cancel) |
| **WAITING_FOR_RESULT** | ❌ | ✅ (resume) | — | ✅ (complete) | ✅ (cancel) |
| **COMPLETED** | ❌ | ❌ | ❌ | — | ❌ |
| **CANCELLED** | ❌ | ❌ | ❌ | ❌ | — |

### Business Rules enforced via `validateTransition()` + `cancel()` checks

1. **WAITING** → `SKIPPED` (skip), `IN_PROGRESS` (call), or `CANCELLED` (cancel)
2. **SKIPPED** → `IN_PROGRESS` (resumeFromSkipped) or `CANCELLED` (cancel)
3. **IN_PROGRESS** → `WAITING_FOR_RESULT`, `COMPLETED`, or `CANCELLED`
4. **WAITING_FOR_RESULT** → `IN_PROGRESS` (resume), `COMPLETED`, or `CANCELLED`
5. **COMPLETED / CANCELLED** are terminal states — no transitions allowed
6. `complete()` and `sendToWaitingForResult()` call `validateTransition()` generic validation
7. `cancel()` has explicit check: **cannot cancel COMPLETED**
8. `resumeFromWaitingForResult()` checks current status is exactly `WAITING_FOR_RESULT`
9. `resumeFromSkipped()` checks current status is exactly `SKIPPED`
10. All invalid transitions throw `InvalidStatusTransitionException` (extends `RuntimeException`)

### Timestamp Mapping

| Trigger | Fields Set |
|---------|-----------|
| `create()` | `checked_in_at`, `created_at`, `updated_at` |
| `call(doctorId)` | `doctorId`, `called_at`, `started_at`, `updated_at` |
| `skip()` | `updated_at` |
| `resumeFromSkipped()` | `called_at`, `started_at`, `updated_at` |
| `sendToWaitingForResult()` | `waiting_for_result_at`, `updated_at` |
| `resumeFromWaitingForResult()` | `updated_at` |
| `complete()` | `completed_at`, `updated_at` |
| `cancel(reason)` | `cancel_reason`, `cancelled_at`, `updated_at` |

---

## 4. Priority & Ordering

### Priority Levels (sorted high → low)

| Priority | Numeric Value | Description |
|----------|:-------------:|-------------|
| **EMERGENCY** | 0 | Cấp cứu — ưu tiên cao nhất |
| **APPOINTMENT** | 1 | Có lịch hẹn trước — tự động phát hiện khi thêm vào queue |
| **REGULAR** | 2 | Thông thường |

### Queue Ordering

Tất cả query danh sách queue (`findByRoomNumberAndStatus`, `findByDoctorIdAndStatus`, `findNextWaiting`) đều sắp xếp theo:

```
ORDER BY
  CASE priority_level
    WHEN 'EMERGENCY'  THEN 0
    WHEN 'APPOINTMENT' THEN 1
    WHEN 'REGULAR'    THEN 2
  END ASC,
  queueNumber ASC
```


→ Bệnh nhân EMERGENCY được phục vụ trước, kế đến APPOINTMENT, cuối cùng là REGULAR.

### Auto-detect APPOINTMENT Priority

Khi gọi `POST /api/v1/queue`, `AddToQueueService.resolvePriority()` tự động:

1. Nếu `priorityLevel = EMERGENCY` → giữ nguyên EMERGENCY (không ghi đè)
2. Kiểm tra bệnh nhân có lịch hẹn hôm nay (qua `AppointmentBusinessSpecification`)
3. Nếu có → gán `PriorityLevel.APPOINTMENT` (ưu tiên hơn REGULAR)
4. Nếu không → giữ nguyên priorityLevel được gửi lên

---

## 5. Concurrency & Locking Strategy

Hệ thống sử dụng **3 cơ chế** để đảm bảo dữ liệu hàng đợi luôn nhất quán dưới concurrent access:

| Cơ chế | Nơi áp dụng | Mục tiêu |
|--------|-------------|----------|
| **PESSIMISTIC_WRITE (`FOR UPDATE`)** | `CallNextService` — `findNextWaiting()` | Đảm bảo chỉ một doctor lấy được bệnh nhân WAITING kế tiếp. Lock row cho đến khi transaction kết thúc. |
| **Optimistic Locking (`@Version`)** | `MedicalQueueEntity` + `MedicalQueue.restore()` | Chống **lost update** khi nhiều transaction cùng update cùng một queue record. JPA/Hibernate tự động kiểm tra version khi flush; nếu version mismatch → `OptimisticLockException`. |
| **UNIQUE Constraint + Retry** | `AddToQueueService.addToQueue()` | Chống duplicate `queue_number` trong cùng ngày/phòng. Khi 2 request đồng thời cùng tính `max+1`, cái thứ 2 bị `DataIntegrityViolationException` → service tự động retry tối đa **3 lần** với `findMaxQueueNumberForToday()` mới. |

**Flow retry:**
```
Client ──► AddToQueueService.addToQueue()
               │
               ├── doAddToQueue()  (attempt 1)
               │       │
               │       ├── findMaxQueueNumberForToday() → 5
               │       ├── save(new queue #6)
               │       └── ❌ DataIntegrityViolationException (trùng UNIQUE)
               │
               ├── doAddToQueue()  (attempt 2)
               │       │
               │       ├── findMaxQueueNumberForToday() → 6 (đã có queue #6)
               │       ├── save(new queue #7)
               │       └── ✅ Thành công
               │
               └── Return QueueResult
```

---

## 6. Implementation Architecture (Hexagonal)

```
Controller (REST)
    │  ◄── MedicalQueueRestMapper
    ▼
UseCase Interface (Port Inbound)
    ▲
    │
Service (Use Case Implementation)
    │  ◄── MedicalQueue (Domain Entity) — state machine + factory
    ▼
MedicalQueueRepository (Port Outbound)
    ▲
    │
RepositoryAdapter (Persistence Adapter)
    │  ◄── JpaMedicalQueueRepository (Spring Data JPA)
    ▼
MySQL (medical_queue table)
```

### Files Created

| Layer | File | Purpose |
|-------|------|---------|
| **Domain** | `domain/queue/MedicalQueue.java` | Entity with state machine, create(), call(), skip(), resumeFromSkipped(), complete(), cancel(), restore() |
| **Domain** | `domain/queue/enums/QueueStatus.java` | Enum: WAITING, SKIPPED, IN_PROGRESS, WAITING_FOR_RESULT, COMPLETED, CANCELLED |
| **Domain** | `domain/queue/enums/PriorityLevel.java` | Enum: EMERGENCY, APPOINTMENT, REGULAR |
| **Domain** | `domain/queue/exception/QueueNotFoundException.java` | Exception: queue not found |
| **Domain** | `domain/queue/exception/InvalidStatusTransitionException.java` | Exception: invalid state transition |
| **Port Inbound** | `port/inbound/queue/AddToQueueUseCase.java` | Interface for adding to queue |
| **Port Inbound** | `port/inbound/queue/CallNextUseCase.java` | Interface for calling next |
| **Port Inbound** | `port/inbound/queue/UpdateQueueStatusUseCase.java` | Interface for updating status |
| **Port Inbound** | `port/inbound/queue/GetQueueListUseCase.java` | Interface for listing queues |
| **Port Outbound** | `port/outbound/repository/crudRepository/queue/MedicalQueueRepository.java` | Repository interface (save, findMaxQueueNumberForToday, ...) |
| **Application** | `application/ucservice/queue/AddToQueueService.java` | Implements AddToQueueUseCase (auto-numbering + retry on conflict + auto APPOINTMENT detect) |
| **Application** | `application/ucservice/queue/CallNextService.java` | Implements CallNextUseCase (find WAITING → call, PESSIMISTIC_WRITE lock) |
| **Application** | `application/ucservice/queue/UpdateQueueStatusService.java` | Implements UpdateQueueStatusUseCase (delegate to domain, full enum switch) |
| **Application** | `application/ucservice/queue/GetQueueListService.java` | Implements GetQueueListUseCase (filter + sort + paginate → `PageResponse`) |
| **Adapter REST** | `adapter/inbound/rest/controller/MedicalQueueController.java` | REST controller (6 endpoints) |
| **Adapter REST** | `adapter/inbound/rest/request/queue/AddToQueueRequest.java` | Request DTO (jakarta.validation) |
| **Adapter REST** | `adapter/inbound/rest/request/queue/CallNextRequest.java` | Request DTO |
| **Adapter REST** | `adapter/inbound/rest/request/queue/UpdateQueueStatusRequest.java` | Request DTO |
| **Adapter REST** | `adapter/inbound/rest/response/queue/MedicalQueueResponse.java` | Response DTO |
| **Adapter REST** | `adapter/inbound/rest/mapper/MedicalQueueRestMapper.java` | Mapper: request → command, domain → response (includes `toPageResponse()`) |
| **DTO** | `port/dto/result/PageResponse.java` | Generic pagination wrapper DTO |
| **DTO** | `port/dto/command/queue/AddToQueueCommand.java` | Command DTO (thêm doctorId) |
| **DTO** | `port/dto/command/queue/UpdateQueueStatusCommand.java` | Command DTO |
| **Persistence** | `persistence/entity/queue/MedicalQueueEntity.java` | JPA entity mapping (`@Version` for optimistic locking) |
| **Persistence** | `persistence/jpaRepository/queue/JpaMedicalQueueRepository.java` | Spring Data JPA (native query with `FOR UPDATE`, priority ordering) |
| **Persistence** | `persistence/mapper/queue/MedicalQueuePersistenceMapper.java` | JPA Entity ↔ Domain mapping (maps `version`) |
| **Persistence** | `persistence/adapterRepository/queue/MedicalQueueRepositoryAdapter.java` | Repository implementation |
| **Migration** | `resources/db/migration/V9__create_medical_queue_tables.sql` | Flyway migration (V9) |
| **Migration** | `resources/db/migration/V10__optimize_medical_queue.sql` | Flyway migration (V10 — UNIQUE, index, version column) |
| **Migration** | `resources/db/migration/V11__update_queue_enums_and_constraints.sql` | Flyway migration (V11 — SKIPPED + APPOINTMENT) |

---

## 7. AppointmentQueue Module (Appointment-based Queue)

**Note:** This section documents the **AppointmentQueue** module (`domain/appointment/AppointmentQueue`), which is separate from the **MedicalQueue** (`domain/queue/MedicalQueue`) documented above.

The `AppointmentQueue` manages queue positioning for patients who already have an appointment (checked in → waiting → called → in-progress → completed/skipped).

### Enum Naming Convention

After resolving a **name collision** where two `QueueStatus.java` files existed in different packages:

| Enum | Package | Purpose |
|------|---------|---------|
| `AppointmentQueueStatus` | `appointment/enums/AppointmentQueueStatus` | Queue status for appointment-based queue (WAITING, CALLING, IN_PROGRESS, COMPLETED, SKIPPED) |
| `QueueStatus` | `queue/enums/QueueStatus` | Queue status for medical queue (WAITING, SKIPPED, IN_PROGRESS, WAITING_FOR_RESULT, COMPLETED, CANCELLED) |
| `AppointmentStatus` | `appointment/enums/AppointmentStatus` | Appointment lifecycle status (SCHEDULED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW) |

### Files

| Layer | File | Purpose |
|-------|------|---------|
| **Enum** | `domain/appointment/enums/AppointmentQueueStatus.java` | Enum: WAITING, CALLING, IN_PROGRESS, COMPLETED, SKIPPED |
| **Domain** | `domain/appointment/AppointmentQueue.java` | State machine + factory: create(), call(), start(), complete(), skip(), restore() |
| **Entity** | `persistence/entity/appointment/AppointmentQueueEntity.java` | JPA entity mapping to `appointment_queues` table |
| **Mapper** | `persistence/mapper/appointment/AppointmentQueuePersistenceMapper.java` | JPA Entity ↔ Domain mapping |
| **Repository** | `port/outbound/repository/crudRepository/appointment/AppointmentQueueRepository.java` | Repository interface |
| **JPA Repo** | `persistence/jpaRepository/appointment/JpaAppointmentQueueRepository.java` | Spring Data JPA interface |
| **Adapter** | `persistence/adapterRepository/appointment/AppointmentQueueRepositoryAdapter.java` | Repository implementation |

### State Machine (AppointmentQueue)

```
WAITING ──── call() ────► CALLING ──── start() ────► IN_PROGRESS ──── complete() ────► COMPLETED
  │                          │
  └──── skip() ──────────────┘
                     
WAITING ──── skip() ───► SKIPPED
CALLING ──── skip() ───► SKIPPED
```

---

## 8. Test Results — `mvn test`

### Total: **112 tests — 0 failures, 0 errors, 0 skipped** ✅


| Test Suite | Tests | Type | Scope |
|-----------|:-----:|------|-------|
| **MedicalQueue - Domain State Machine Tests** | 20 | Unit | State transitions (thêm SKIPPED, resumeFromSkipped), creation, call, complete, cancel, resume, invalid, restore |
| **AddToQueueService Tests** | 5 | Unit | Queue with next number + first of day + retry on conflict + APPOINTMENT auto-detect + EMERGENCY keeps priority |
| **CallNextService Tests** | 2 | Unit | Success + no WAITING queues (QueueNotFoundException) |
| **UpdateQueueStatusService Tests** | 13 | Unit | 9 valid transitions (thêm WAITING→SKIPPED, SKIPPED→IN_PROGRESS) + 4 invalid transitions |
| **GetQueueListService Tests** | 3 | Unit | Filter by room, by doctor, empty result |
| **MedicalQueueController - MockMvc Tests** | 11 | Integration | 6 endpoints — thêm roomNumber validation cho addToQueue và callNext |
| MedicalHistoryController - MockMvc Tests | 4 | Integration | Existing |
| GetPatientMedicalHistoryQueryHandler | 8 | Unit | Existing |
| Role-Permission Mapping Tests | 7 | Unit | Existing |
| UserSession - Session Security Tests | 6 | Unit | Existing |
| User Domain Tests | 10 | Unit | Existing |
| PermissionEvaluator Tests | 12 | Unit | Existing |
| User Repository & Service Integration Tests | 11 | Integration | Existing |

### Test Coverage Highlights

- **State Machine**: Cả 20 transitions (valid + invalid) được test, bao gồm SKIPPED ↔ IN_PROGRESS
- **Service Layer**: Repository mocked — tested business logic (numbering, exceptions, filtering, retry, priority auto-detect)
- **Controller**: `@WebMvcTest` with `@MockitoBean` use cases — validates HTTP status codes + JSON response + `@NotNull` validation
- **Test Isolation**: Each test suite loads only what it needs (no redundant Spring context)
- **Mockito Strictness**: Test EMERGENCY priority cases không mock appointment repository (`findOne`) — tránh `UnnecessaryStubbingException` vì `resolvePriority()` return ngay cho EMERGENCY
