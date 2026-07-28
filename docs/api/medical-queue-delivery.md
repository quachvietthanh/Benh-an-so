# Medical Queue Module — Delivery Documentation

**Module:** NCL-03 — Khám bệnh & bệnh án điện tử (Subtasks: Queue DB → Queue Features → Integration)  
**Owner:** Backend Engineer (Queue)  
**Base Path:** `/api/v1/queue`  
**Legacy Compatibility Path:** `/api/v1/appointments/queue`  
**Date:** 2026-07-28  
**Version:** 1.0  

---

## 1. Architecture Overview

### 1.1 Queue State Machine

```
                          ┌──────────────────────────────────────────────┐
                          │                                              │
                          v                                              │
    ┌──────────┐    callNext()    ┌──────────────┐   sendToResult()   ┌──────────────────┐
    │  WAITING  │ ───────────────> │ IN_PROGRESS   │ ─────────────────>│ WAITING_FOR_RESULT│
    └─────┬─────┘                  └──────┬─────────┘                   └────────┬─────────┘
          │                               │                                       │
          │  skip()                       │  complete()                           │  resumeFromResult()
          v                               v                                       v
    ┌──────────┐                   ┌──────────────┐                   ┌──────────────────┐
    │  SKIPPED  │                  │  COMPLETED   │                   │  IN_PROGRESS ─────┘
    └─────┬─────┘                  └──────────────┘                   │  (resumed)
          │                                                            └──────────────────
          │  resumeFromSkipped()
          v
    ┌──────────┐
    │IN_PROGRESS│
    └──────────┘

    *Any state* ─── cancel(reason) ───> CANCELLED (terminal)
```

**Transition Rules:**

| From → To | Allowed? | Via Method | Who |
|-----------|---------|-----------|-----|
| WAITING → IN_PROGRESS | ✅ | `call()` | Doctor |
| WAITING → SKIPPED | ✅ | `skip()` | Doctor |
| WAITING → CANCELLED | ✅ | `cancel()` | Admin/Doctor |
| IN_PROGRESS → WAITING_FOR_RESULT | ✅ | `sendToWaitingForResult()` | Doctor |
| IN_PROGRESS → COMPLETED | ✅ | `complete()` | Doctor |
| IN_PROGRESS → CANCELLED | ✅ | `cancel()` | Admin/Doctor |
| WAITING_FOR_RESULT → IN_PROGRESS | ✅ | `resumeFromWaitingForResult()` | Doctor |
| WAITING_FOR_RESULT → COMPLETED | ✅ | `complete()` | Doctor |
| WAITING_FOR_RESULT → CANCELLED | ✅ | `cancel()` | Admin/Doctor |
| SKIPPED → IN_PROGRESS | ✅ | `resumeFromSkipped()` | Doctor |
| SKIPPED → CANCELLED | ✅ | `cancel()` | Admin/Doctor |
| COMPLETED → *any* | ❌ | — | Terminal state |
| CANCELLED → *any* | ❌ | — | Terminal state |

---

## 2. REST API Contracts

### 2.1 Add Patient to Queue

Creates a new queue entry for a patient, auto-assigning the next sequential number for the day.

```
POST /api/v1/queue
Legacy: N/A (use the new endpoint)
```

**Request Body:**

```json
{
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
    "priorityLevel": "APPOINTMENT",
    "roomNumber": "Phòng 101",
    "doctorId": "660e8400-e29b-41d4-a716-446655440111"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `patientId` | UUID | ✅ | Patient UUID |
| `priorityLevel` | Enum | ✅ | `REGULAR`, `APPOINTMENT`, `EMERGENCY` |
| `roomNumber` | String | ✅ | Clinic room identifier |
| `doctorId` | UUID | ❌ | Assigned doctor (nullable) |

**Response `201 Created`:**

```json
{
    "id": "770e8400-e29b-41d4-a716-446655440222",
    "patientId": "550e8400-e29b-41d4-a716-446655440000",
    "patientName": "Nguyễn Văn A",
    "doctorId": null,
    "doctorName": null,
    "roomNumber": "Phòng 101",
    "queueNumber": 42,
    "status": "WAITING",
    "priorityLevel": "APPOINTMENT",
    "notes": null,
    "checkedInAt": "2026-07-28T09:00:00Z",
    "calledAt": null,
    "startedAt": null,
    "waitingForResultAt": null,
    "completedAt": null,
    "cancelledAt": null,
    "cancelReason": null,
    "createdAt": "2026-07-28T09:00:00Z",
    "updatedAt": "2026-07-28T09:00:00Z"
}
```

**Security:** `hasAnyRole('ADMIN', 'RECEPTIONIST')`

---

### 2.2 Call Next Patient

Moves the next WAITING patient from the queue into IN_PROGRESS status based on priority (EMERGENCY > APPOINTMENT > REGULAR) then queue number (FIFO).

```
POST /api/v1/queue/call-next
Legacy: POST /api/v1/appointments/queue/call-next  (no body — uses current user as doctor)
```

**Request Body:**

```json
{
    "doctorId": "660e8400-e29b-41d4-a716-446655440111",
    "roomNumber": "Phòng 101"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `doctorId` | UUID | ✅ | Doctor who will see the patient |
| `roomNumber` | String | ✅ | Room where the patient is called to |

**Response `200 OK`:** Single `MedicalQueueResponse` (same shape as 2.1 response, but `status: "IN_PROGRESS"`, `calledAt` and `startedAt` populated).

**Error `404 Not Found`:** If no WAITING patients exist in the queue.

**Security:** `hasAnyRole('ADMIN', 'DOCTOR')`

---

### 2.3 Update Queue Item Status

Transitions a specific queue item to a new status according to the state machine.

```
PUT /api/v1/queue/{id}/status
Legacy: N/A
```

**Path Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `id` | UUID | Queue item UUID |

**Request Body:**

```json
{
    "newStatus": "COMPLETED",
    "doctorId": "660e8400-e29b-41d4-a716-446655440111",
    "cancelReason": null
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `newStatus` | Enum | ✅ | Target status: `IN_PROGRESS`, `WAITING_FOR_RESULT`, `SKIPPED`, `COMPLETED`, `CANCELLED` |
| `doctorId` | UUID | ❌ | Doctor performing the action |
| `cancelReason` | String | ❌ | Reason (required when `CANCELLED` — validate in service) |

**Response `200 OK`:** Single `MedicalQueueResponse` with updated status and timestamps.

**Error `400 Bad Request`:** If the transition is invalid (e.g., COMPLETED → WAITING).

**Security:** `hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')`

---

### 2.4 Get Queue by Room

Paginated list of queue items filtered by room, with optional status filter.

```
GET /api/v1/queue/room/{roomNumber}
Legacy: N/A
```

**Path Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `roomNumber` | String | Room identifier |

**Query Parameters:**

| Param | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `status` | Enum | ❌ | — | Filter by `QueueStatus` |
| `page` | int | ❌ | 0 | Zero-based page index |
| `size` | int | ❌ | 20 | Page size (max 100) |

**Response `200 OK`:**

```json
{
    "content": [ /* array of MedicalQueueResponse */ ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
}
```

**Security:** `isAuthenticated()`

---

### 2.5 Get Queue by Doctor

Paginated list of queue items filtered by doctor.

```
GET /api/v1/queue/doctor/{doctorId}
Legacy: N/A
```

**Path Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `doctorId` | UUID | Doctor UUID |

**Query Parameters:** Same as 2.4 (`status`, `page`, `size`).

**Response `200 OK`:** `PageResponse<MedicalQueueResponse>` (same shape as 2.4).

**Security:** `hasAnyRole('ADMIN', 'DOCTOR')`

---

### 2.6 Count Queue Items

Returns the count of queue items matching optional filters.

```
GET /api/v1/queue/count
Legacy: N/A
```

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `roomNumber` | String | ❌ | Filter by room |
| `doctorId` | UUID | ❌ | Filter by doctor |
| `status` | Enum | ❌ | Filter by `QueueStatus` |

**Response `200 OK`:** Plain number (e.g. `15`).

**Security:** `isAuthenticated()`

---

### 2.7 Legacy Endpoints (Frontend Compatibility)

These endpoints exist at `/api/v1/appointments/queue` for backward compatibility. They delegate to the same use cases as the new endpoints.

```
GET  /api/v1/appointments/queue          → Returns all queue items as flat JSON array
POST /api/v1/appointments/queue/call-next → Calls next patient (inferred doctor from JWT)
```

**Why legacy endpoints are needed:** The frontend `appointmentApi.js` calls:
- `axiosClient.get('/appointments/queue')` — expects `response.data` as a flat array
- `axiosClient.post('/appointments/queue/call-next')` — sends no body

Both are served by `LegacyAppointmentQueueController`.

---

## 3. Key DTOs

### 3.1 `MedicalQueueResponse`

| Field | Type | Always Present | Notes |
|-------|------|---------------|-------|
| `id` | UUID | ✅ | Queue item UUID |
| `patientId` | UUID | ✅ | |
| `patientName` | String | ❌ | Null if patient not found |
| `doctorId` | UUID | ❌ | Null if no doctor assigned |
| `doctorName` | String | ❌ | Null if no doctor assigned |
| `roomNumber` | String | ❌ | Null if not set |
| `queueNumber` | int | ✅ | Auto-increment per day |
| `status` | Enum | ✅ | `QueueStatus` |
| `priorityLevel` | Enum | ✅ | `PriorityLevel` |
| `notes` | String | ❌ | Clinical notes |
| `checkedInAt` | Instant | ✅ | When patient was added to queue |
| `calledAt` | Instant | ❌ | When called |
| `startedAt` | Instant | ❌ | When examination started |
| `waitingForResultAt` | Instant | ❌ | When sent for results |
| `completedAt` | Instant | ❌ | When completed |
| `cancelledAt` | Instant | ❌ | When cancelled |
| `cancelReason` | String | ❌ | Cancellation reason |
| `createdAt` | Instant | ✅ | Record creation timestamp |
| `updatedAt` | Instant | ✅ | Last update timestamp |

### 3.2 Enums

#### `QueueStatus`
```
WAITING             → Initial state after check-in
IN_PROGRESS         → Patient is being examined
WAITING_FOR_RESULT  → Sent for lab/imaging
SKIPPED             → Temporarily skipped
COMPLETED           → Final state (terminal)
CANCELLED           → Final state (terminal)
```

#### `PriorityLevel`
```
REGULAR     → Walk-in / normal
APPOINTMENT → Has a scheduled appointment
EMERGENCY   → Urgent (skips to front)
```

### 3.3 `PageResponse<T>`

| Field | Type | Description |
|-------|------|-------------|
| `content` | List<T> | Page items |
| `page` | int | Zero-based page index |
| `size` | int | Page size |
| `totalElements` | long | Total items across all pages |
| `totalPages` | int | Total number of pages |

---

## 4. Postman Collection

Save the following JSON as `medical-queue.postman_collection.json` and import into Postman.

```json
{
    "info": {
        "name": "Bệnh Án Số — Medical Queue API",
        "description": "Queue management endpoints for the clinic.\nBase URL: http://localhost:8080/api/v1\nLegacy path prefix: /api/v1/appointments/queue",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        "_exporter_id": "backend-queue"
    },
    "item": [
        {
            "name": "1️⃣ Add Patient to Queue",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json",
                        "type": "text"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"patientId\": \"550e8400-e29b-41d4-a716-446655440000\",\n    \"priorityLevel\": \"APPOINTMENT\",\n    \"roomNumber\": \"Phòng 101\",\n    \"doctorId\": \"660e8400-e29b-41d4-a716-446655440111\"\n}"
                },
                "url": {
                    "raw": "{{base_url}}/api/v1/queue",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "queue"]
                },
                "description": "Add a patient to the queue. Auto-assigns next queue number. Created by RECEPTIONIST (requires ADMIN or RECEPTIONIST role)."
            },
            "response": [
                {
                    "name": "201 Created",
                    "status": "Created",
                    "code": 201,
                    "header": [
                        { "key": "Content-Type", "value": "application/json" }
                    ],
                    "body": "{\n    \"id\": \"770e8400-e29b-41d4-a716-446655440222\",\n    \"patientId\": \"550e8400-e29b-41d4-a716-446655440000\",\n    \"patientName\": \"Nguyễn Văn A\",\n    \"doctorId\": null,\n    \"doctorName\": null,\n    \"roomNumber\": \"Phòng 101\",\n    \"queueNumber\": 42,\n    \"status\": \"WAITING\",\n    \"priorityLevel\": \"APPOINTMENT\",\n    \"notes\": null,\n    \"checkedInAt\": \"2026-07-28T09:00:00Z\",\n    \"calledAt\": null,\n    \"startedAt\": null,\n    \"waitingForResultAt\": null,\n    \"completedAt\": null,\n    \"cancelledAt\": null,\n    \"cancelReason\": null,\n    \"createdAt\": \"2026-07-28T09:00:00Z\",\n    \"updatedAt\": \"2026-07-28T09:00:00Z\"\n}"
                }
            ]
        },
        {
            "name": "2️⃣ Call Next Patient",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json",
                        "type": "text"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"doctorId\": \"660e8400-e29b-41d4-a716-446655440111\",\n    \"roomNumber\": \"Phòng 101\"\n}"
                },
                "url": {
                    "raw": "{{base_url}}/api/v1/queue/call-next",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "queue", "call-next"]
                },
                "description": "Call the next WAITING patient. The system picks the highest priority (EMERGENCY > APPOINTMENT > REGULAR), then lowest queue number (FIFO)."
            },
            "response": [
                {
                    "name": "200 OK",
                    "status": "OK",
                    "code": 200,
                    "body": "{\n    \"id\": \"770e8400-e29b-41d4-a716-446655440222\",\n    \"patientId\": \"550e8400-e29b-41d4-a716-446655440000\",\n    \"patientName\": \"Nguyễn Văn A\",\n    \"doctorId\": \"660e8400-e29b-41d4-a716-446655440111\",\n    \"doctorName\": \"BS. Phạm Hồng Anh\",\n    \"roomNumber\": \"Phòng 101\",\n    \"queueNumber\": 42,\n    \"status\": \"IN_PROGRESS\",\n    \"priorityLevel\": \"APPOINTMENT\",\n    \"notes\": null,\n    \"checkedInAt\": \"2026-07-28T09:00:00Z\",\n    \"calledAt\": \"2026-07-28T09:15:00Z\",\n    \"startedAt\": \"2026-07-28T09:15:00Z\",\n    \"waitingForResultAt\": null,\n    \"completedAt\": null,\n    \"cancelledAt\": null,\n    \"cancelReason\": null,\n    \"createdAt\": \"2026-07-28T09:00:00Z\",\n    \"updatedAt\": \"2026-07-28T09:15:00Z\"\n}"
                }
            ]
        },
        {
            "name": "3️⃣ Update Queue Status",
            "request": {
                "method": "PUT",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    },
                    {
                        "key": "Content-Type",
                        "value": "application/json",
                        "type": "text"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"newStatus\": \"WAITING_FOR_RESULT\",\n    \"doctorId\": \"660e8400-e29b-41d4-a716-446655440111\",\n    \"cancelReason\": null\n}"
                },
                "url": {
                    "raw": "{{base_url}}/api/v1/queue/770e8400-e29b-41d4-a716-446655440222/status",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "queue", "770e8400-e29b-41d4-a716-446655440222", "status"]
                },
                "description": "Transition a queue item to a new status. Valid targets: IN_PROGRESS, WAITING_FOR_RESULT, SKIPPED, COMPLETED, CANCELLED. Returns 400 if transition is invalid per the state machine."
            },
            "response": [
                {
                    "name": "200 OK — WAITING_FOR_RESULT",
                    "status": "OK",
                    "code": 200,
                    "body": "{\n    \"id\": \"770e8400-e29b-41d4-a716-446655440222\",\n    \"patientId\": \"550e8400-e29b-41d4-a716-446655440000\",\n    \"patientName\": \"Nguyễn Văn A\",\n    \"doctorId\": \"660e8400-e29b-41d4-a716-446655440111\",\n    \"doctorName\": \"BS. Phạm Hồng Anh\",\n    \"roomNumber\": \"Phòng 101\",\n    \"queueNumber\": 42,\n    \"status\": \"WAITING_FOR_RESULT\",\n    \"priorityLevel\": \"APPOINTMENT\",\n    \"notes\": null,\n    \"checkedInAt\": \"2026-07-28T09:00:00Z\",\n    \"calledAt\": \"2026-07-28T09:15:00Z\",\n    \"startedAt\": \"2026-07-28T09:15:00Z\",\n    \"waitingForResultAt\": \"2026-07-28T09:45:00Z\",\n    \"completedAt\": null,\n    \"cancelledAt\": null,\n    \"cancelReason\": null,\n    \"createdAt\": \"2026-07-28T09:00:00Z\",\n    \"updatedAt\": \"2026-07-28T09:45:00Z\"\n}"
                }
            ]
        },
        {
            "name": "4️⃣ Get Queue by Room",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    }
                ],
                "url": {
                    "raw": "{{base_url}}/api/v1/queue/room/Phòng 101?status=WAITING&page=0&size=20",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "queue", "room", "Phòng 101"],
                    "query": [
                        { "key": "status", "value": "WAITING" },
                        { "key": "page", "value": "0" },
                        { "key": "size", "value": "20" }
                    ]
                },
                "description": "Get paginated queue items for a room. Optionally filter by status."
            },
            "response": [
                {
                    "name": "200 OK",
                    "status": "OK",
                    "code": 200,
                    "body": "{\n    \"content\": [],\n    \"page\": 0,\n    \"size\": 20,\n    \"totalElements\": 0,\n    \"totalPages\": 0\n}"
                }
            ]
        },
        {
            "name": "5️⃣ Get Queue by Doctor",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    }
                ],
                "url": {
                    "raw": "{{base_url}}/api/v1/queue/doctor/660e8400-e29b-41d4-a716-446655440111?page=0&size=20",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "queue", "doctor", "660e8400-e29b-41d4-a716-446655440111"],
                    "query": [
                        { "key": "page", "value": "0" },
                        { "key": "size", "value": "20" }
                    ]
                },
                "description": "Get paginated queue items for a specific doctor."
            },
            "response": [
                {
                    "name": "200 OK",
                    "status": "OK",
                    "code": 200,
                    "body": "{\n    \"content\": [],\n    \"page\": 0,\n    \"size\": 20,\n    \"totalElements\": 0,\n    \"totalPages\": 0\n}"
                }
            ]
        },
        {
            "name": "6️⃣ Count Queue Items",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    }
                ],
                "url": {
                    "raw": "{{base_url}}/api/v1/queue/count?roomNumber=Phòng 101&status=WAITING",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "queue", "count"],
                    "query": [
                        { "key": "roomNumber", "value": "Phòng 101" },
                        { "key": "status", "value": "WAITING" }
                    ]
                },
                "description": "Count queue items matching optional filters. Returns a plain number."
            },
            "response": [
                {
                    "name": "200 OK",
                    "status": "OK",
                    "code": 200,
                    "body": "15"
                }
            ]
        },
        {
            "name": "🔁 Legacy: Get Queue (Flat Array)",
            "request": {
                "method": "GET",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    }
                ],
                "url": {
                    "raw": "{{base_url}}/api/v1/appointments/queue",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "appointments", "queue"]
                },
                "description": "LEGACY: Frontend compatibility endpoint. Returns all queue items as a flat JSON array (not wrapped in PageResponse)."
            },
            "response": [
                {
                    "name": "200 OK",
                    "status": "OK",
                    "code": 200,
                    "body": "[]"
                }
            ]
        },
        {
            "name": "🔁 Legacy: Call Next (No Body)",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Authorization",
                        "value": "Bearer {{jwt_token}}",
                        "type": "text"
                    }
                ],
                "url": {
                    "raw": "{{base_url}}/api/v1/appointments/queue/call-next",
                    "host": ["{{base_url}}"],
                    "path": ["api", "v1", "appointments", "queue", "call-next"]
                },
                "description": "LEGACY: Frontend compatibility endpoint. Calls next patient. Doctor ID is inferred from the JWT token (no body sent)."
            },
            "response": [
                {
                    "name": "200 OK",
                    "status": "OK",
                    "code": 200,
                    "body": "{\n    \"id\": \"770e8400-e29b-41d4-a716-446655440222\",\n    \"patientId\": \"550e8400-e29b-41d4-a716-446655440000\",\n    \"patientName\": \"Nguyễn Văn A\",\n    \"doctorId\": \"660e8400-e29b-41d4-a716-446655440111\",\n    \"doctorName\": \"BS. Phạm Hồng Anh\",\n    \"roomNumber\": \"Phòng 101\",\n    \"queueNumber\": 42,\n    \"status\": \"IN_PROGRESS\",\n    \"priorityLevel\": \"APPOINTMENT\",\n    \"notes\": null,\n    \"checkedInAt\": \"2026-07-28T09:00:00Z\",\n    \"calledAt\": \"2026-07-28T09:15:00Z\",\n    \"startedAt\": \"2026-07-28T09:15:00Z\",\n    \"waitingForResultAt\": null,\n    \"completedAt\": null,\n    \"cancelledAt\": null,\n    \"cancelReason\": null,\n    \"createdAt\": \"2026-07-28T09:00:00Z\",\n    \"updatedAt\": \"2026-07-28T09:15:00Z\"\n}"
                }
            ]
        }
    ],
    "variable": [
        {
            "key": "base_url",
            "value": "http://localhost:8080",
            "type": "string"
        },
        {
            "key": "jwt_token",
            "value": "YOUR_JWT_TOKEN_HERE",
            "type": "string"
        }
    ]
}
```

---

## 5. Test Coverage Summary

| Test Class | Type | Count | Status |
|-----------|------|-------|--------|
| `MedicalQueueTest` | Unit (domain) | Domain logic + state transitions | ✅ 54 total |
| `AddToQueueServiceTest` | Unit (service) | AddToQueueService business rules | ✅ |
| `CallNextServiceTest` | Unit (service) | CallNextService priority/FIFO rules | ✅ |
| `GetQueueListServiceTest` | Unit (service) | Pagination, filtering | ✅ |
| `UpdateQueueStatusServiceTest` | Unit (service) | Status transition validation | ✅ |
| `MedicalQueueControllerTest` | Integration | REST controller integration | ✅ |

All 54 existing queue tests continue to pass. The `LegacyAppointmentQueueController` delegates to the same use cases, so no new tests are needed — coverage is inherited.

---

## 6. Integration Notes

### Frontend ↔ Backend Compatibility

| Frontend Call | Backend Handler | Notes |
|--------------|----------------|-------|
| `GET /appointments` | (Appointment module) | Not Queue scope |
| `GET /appointments/queue` | ✅ `LegacyAppointmentQueueController.getQueue()` | Returns flat array of `MedicalQueueResponse` |
| `POST /appointments` | (Appointment module) | Not Queue scope |
| `POST /appointments/queue/call-next` | ✅ `LegacyAppointmentQueueController.callNext()` | No body — doctor from JWT |
| `PATCH /appointments/{id}/check-in` | 🔴 Unguided | This is an Appointment operation. Frontend should be migrated to call `POST /api/v1/queue` directly |
| `PATCH /appointments/{id}/cancel` | (Appointment module) | Not Queue scope |
| `PATCH /appointments/{id}/no-show` | (Appointment module) | Not Queue scope |
| `PATCH /appointments/{id}/complete` | (Appointment module) | Not Queue scope |

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Frontend API base URL |

### Postman Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `{{base_url}}` | `http://localhost:8080` | Server URL |
| `{{jwt_token}}` | — | JWT from login response's `accessToken` |

---

*Generated by: Backend Engineer (Queue Module)*  
*Guideline: .clinerules — Decision Tree (Step 2 Reuse: all legacy endpoints reuse existing use cases)*
