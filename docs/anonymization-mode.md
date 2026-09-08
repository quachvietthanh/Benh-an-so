# Anonymization Mode (NCL-15-CN-003) — Architecture & Contract

This document is the specification and contract for the patient data anonymization (ẩn danh) feature. The frontend can consume these APIs without knowing any database or persistence detail.

## 1. Purpose

When anonymization is **ON**, the backend masks patient-identifying fields in REST responses and exports so that demonstrations/training never expose real patient data. The underlying business data is never changed or persisted in masked form.

## 2. Fields masked (CV-01 scope)

Only these three fields are masked (required by the Excel spec):

| Field    | Masked value                                                     | Where exposed |
|----------|------------------------------------------------------------------|---------------|
| fullName | `BỆNH NHÂN #<patientCode>` (or `BỆNH NHÂN` when no patient code) | Patient, Visit, Medical Record, Prescription, Queue, Billing, PDF Exports |
| phone    | `<first 2 digits>******<last 2 digits>`                          | Patient, Visit, Medical Record |
| address  | `[ĐỊA CHỈ ĐÃ ẨN DANH]`                                          | Patient |

Other fields (`identityNumber`, `insuranceNumber`, `email`, `dateOfBirth`, `emergencyContact`, `emergencyPhone`) are intentionally **not** masked (candidate fields, out of current scope).

The public patient-portal phone format (`091***001`) is a separate, always-on historical contract and is unaffected by this mode.

## 3. Anonymization strategy (CV-02)

Deterministic, readable masking via pure domain policy `PatientAnonymizer`:
- **Full name** → `BỆNH NHÂN #<patientCode>` so the same patient stays recognizable during a demo.
- **Phone** → keeps only the first two and last two digits (e.g. `0912345678` → `09******78`). Non-digit formatting is stripped first.
- **Address** → fixed placeholder `[ĐỊA CHỈ ĐÃ ẨN DANH]`.

## 4. API Endpoints

### 1. Read current mode

```http
GET /system/anonymization
```

Response `200 OK`:
```json
{
  "enabled": false,
  "updatedAt": "2026-09-08T14:30:00Z"
}
```

### 2. Turn ON / OFF

```http
PATCH /system/anonymization
```

Request body:
```json
{
  "enabled": true
}
```

Response `200 OK`:
```json
{
  "enabled": true,
  "updatedAt": "2026-09-08T14:30:00Z"
}
```

## 5. Permissions (RBAC)

| Endpoint                  | Required permission      |
|---------------------------|--------------------------|
| `GET  /system/anonymization`  | `SYSTEM_CONFIG_READ`   |
| `PATCH /system/anonymization` | `SYSTEM_CONFIG_UPDATE` |

Both permissions are granted to the `ADMIN` role only.

## 6. HTTP status codes

| Code | Meaning                                                |
|------|--------------------------------------------------------|
| 200  | Success (GET / PATCH)                                  |
| 400  | Missing/invalid `enabled` field                        |
| 401  | Not authenticated                                      |
| 403  | Authenticated but lacks the required permission        |

## 7. Presentation & Export Masking

- **REST Responses:** Masking is applied in REST mappers:
  - `PatientRestMapper` — fullName, phone, address.
  - `VisitRestMapper` — fullName, phone.
  - `MedicalRecordDetailRestMapper` — fullName, phone.
  - `PrescriptionRestMapper` — fullName.
  - `QueueRestMapper` — fullName (`BỆNH NHÂN #<patientCode>`).
  - `BillingRestMapper` — fullName.
- **Exports (TC-02):**
  - `ExportPrescriptionService` — masks patient name in prescription PDF.
  - `IssueMedicalRecordCopyService` — masks patient name in medical record copy PDF.
  - Operational report CSV does not contain patient identity and is unchanged.

## 8. Database Safety & Update Protection

Original patient data remains unchanged in the database. Masking happens only at presentation/export boundaries.

If a masked value (e.g. `BỆNH NHÂN #BN001`, `09******78`, `[ĐỊA CHỈ ĐÃ ẨN DANH]`) is submitted back to a patient update endpoint, `UpdatePatientService` detects it and preserves the existing real value instead of persisting the synthetic value.

## 9. Audit Logging (TC-04)

Every ON/OFF change writes an audit log entry:
- action: `UPDATE`
- resource type: `CONFIGURATION`
- detail: `Anonymization mode changed from <before> to <after>`
- actor: the current user ID
- timestamp: current instant

## 10. Persistence & State Cache

The mode is stored in the persistent `system_configuration` table (created via Flyway `V40__create_system_configuration_table.sql`) and survives restarts. `AnonymizationModeState` acts as an in-memory cache loaded on startup and updated synchronously after every change.
