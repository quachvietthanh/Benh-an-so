# Anonymization Mode (NCL-15-CN-003) — Backend Contract

This document is the backend contract for the anonymization (ẩn danh) feature. The
frontend can consume these APIs without knowing any database or persistence detail.

## Purpose

When anonymization is **ON**, the backend masks patient-identifying fields in REST
responses and exports so that demonstrations/training never expose real patient data.
The underlying business data is never changed or persisted in masked form.

## Fields masked

Only these three fields are masked (CV-01 scope):

| Field    | Masked value                                                     |
|----------|------------------------------------------------------------------|
| fullName | `BỆNH NHÂN #<patientCode>` (or `BỆNH NHÂN` when no patient code) |
| phone    | `<first 2 digits>******<last 2 digits>`                          |
| address  | `[ĐỊA CHỈ ĐÃ ẨN DANH]`                                          |

Other fields (identityNumber, insuranceNumber, email, dateOfBirth, emergencyContact,
emergencyPhone) are intentionally **not** masked.

The public patient-portal phone format (`091***001`) is a separate, always-on
historical contract and is unaffected by this mode.

## API

### 1. Read current mode

```
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

```
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

## Permissions (RBAC)

| Endpoint                  | Required permission      |
|---------------------------|--------------------------|
| `GET  /system/anonymization`  | `SYSTEM_CONFIG_READ`   |
| `PATCH /system/anonymization` | `SYSTEM_CONFIG_UPDATE` |

Both permissions are granted to the `ADMIN` role only.

## HTTP status codes

| Code | Meaning                                                |
|------|--------------------------------------------------------|
| 200  | Success (GET / PATCH)                                  |
| 400  | Missing/invalid `enabled` field                        |
| 401  | Not authenticated                                      |
| 403  | Authenticated but lacks the required permission        |

## Behavior

- **OFF (default):** all responses and exports return the original PII unchanged.
- **ON:** fullName / phone / address are masked at the REST/export boundary only.

The toggle takes effect at runtime — no restart is required.

## Update safety

If a masked value (e.g. `BỆNH NHÂN #BN001`, `09******78`, `[ĐỊA CHỈ ĐÃ ẨN DANH]`) is
submitted back to a patient update endpoint, the backend detects it and keeps the
existing real value instead of persisting the synthetic value.

## Audit

Every ON/OFF change writes an audit log entry:

- action: `UPDATE`
- resource type: `CONFIGURATION`
- detail: `Anonymization mode changed from <before> to <after>`
- actor: the current user id

History can be read through the existing admin-only audit-log endpoints.

## Persistence

The mode is stored in a persistent system-configuration table and survives restarts.
The in-memory value is only a cache. The frontend does not need to know these
implementation details.
