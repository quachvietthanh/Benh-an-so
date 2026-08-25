# Diagnosis Catalog & Medical-record Diagnoses API

> User Story: **NCL-13-CN-002 — Ghi chẩn đoán kèm mã bệnh**  
> Base URL: `http://localhost:8080/api/v1`  
> Auth: Bearer Token (JWT)

## 1. Business contract

This document is the source of truth for recording diagnoses with disease codes.

| Topic | Agreed contract |
| --- | --- |
| Who can record diagnoses | Only a user with the `DOCTOR` role. `ADMIN` is explicitly excluded, even when the user has `MEDICAL_RECORD_UPDATE`. |
| Primary diagnosis | Required and must reference an active disease-code catalog entry. |
| Secondary diagnoses | Optional list. Each entry can either reference an active catalog entry or be free text; an entry that supplies a catalog ID must reference an active entry. |
| Source of code/name | When `diagnosisCatalogId` is supplied, the server resolves and persists the catalog code and name. Client-provided code/name is not authoritative. |
| Record state | The medical record must be editable and its visit must be active. |
| Audit | A successful replacement records the doctor and timestamp for each persisted diagnosis and an UPDATE access-log entry for the medical record. |

`QTN-11` and `QTN-22` are applied together: only a doctor may save, and the primary diagnosis cannot be saved without a valid disease code.

## 2. Search disease-code catalog

### `GET /diagnosis-catalog?search={keyword}`

Searches active ICD-10/disease-code entries by code or name so a doctor can select the primary diagnosis.

| Query parameter | Required | Description |
| --- | --- | --- |
| `search` | No | Partial code or disease name. |

Roles: `DOCTOR` for selection while recording; `ADMIN` may manage the catalog through `/system/diagnosis-catalog/**`.

Example response:

```json
[
  {
    "id": "a1000000-0000-0000-0000-000000000019",
    "code": "J00",
    "name": "Common cold",
    "diseaseGroup": "Respiratory",
    "description": "Acute nasopharyngitis",
    "active": true,
    "createdAt": "2026-08-25T00:00:00Z",
    "updatedAt": null
  }
]
```

## 3. Replace diagnoses for a medical record

### `PUT /medical-records/{medicalRecordId}/diagnoses`

Replaces the diagnosis list for one editable medical record. This is the retained endpoint; `/examinations/{examinationId}/diagnosis` is not part of the API contract.

Roles: `DOCTOR` only.

```json
{
  "primaryDiagnosis": {
    "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000019",
    "note": "Mild symptoms"
  },
  "secondaryDiagnoses": [
    {
      "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000020",
      "note": "Associated fever"
    },
    {
      "name": "Clinical observation not yet coded",
      "note": "Monitor at follow-up"
    }
  ]
}
```

### Request rules

| Field | Primary diagnosis | Secondary diagnosis |
| --- | --- | --- |
| `diagnosisCatalogId` | Required; must exist and be active | Optional; if supplied, it must exist and be active |
| `name` | Not part of the request DTO | Required when no catalog ID is supplied; it must be omitted when a catalog ID is supplied |
| `note` | Optional | Optional |

Each secondary diagnosis must provide exactly one of `diagnosisCatalogId` or `name`. The API returns the persisted diagnoses:

```json
[
  {
    "id": "b2000000-0000-0000-0000-000000000001",
    "medicalRecordId": "c3000000-0000-0000-0000-000000000001",
    "diagnosisCode": "J00",
    "diagnosisName": "Common cold",
    "diagnosisType": "PRIMARY",
    "note": "Mild symptoms",
    "diagnosedBy": "d4000000-0000-0000-0000-000000000001",
    "diagnosedAt": "2026-08-25T00:00:00Z"
  },
  {
    "id": "b2000000-0000-0000-0000-000000000002",
    "medicalRecordId": "c3000000-0000-0000-0000-000000000001",
    "diagnosisCode": null,
    "diagnosisName": "Clinical observation not yet coded",
    "diagnosisType": "SECONDARY",
    "note": "Monitor at follow-up",
    "diagnosedBy": "d4000000-0000-0000-0000-000000000001",
    "diagnosedAt": "2026-08-25T00:00:00Z"
  }
]
```

### Errors

| HTTP status | Condition |
| --- | --- |
| `400 Bad Request` | Missing primary catalog ID, unknown/inactive supplied catalog ID, or a secondary diagnosis that provides neither/both catalog ID and name |
| `403 Forbidden` | Caller is not a doctor, including an administrator |
| `404 Not Found` | Medical record does not exist |
| `409 Conflict` | The medical record is locked or its visit is no longer active |

## 4. Read diagnoses

### `GET /medical-records/{medicalRecordId}/diagnoses`

Returns the persisted primary and secondary diagnoses. Read authorization remains governed by `MEDICAL_RECORD_READ` and the medical-record access policy; this does not grant the right to edit diagnoses.

## 5. Implementation status

The NCL-13-CN-002 backend implementation is complete: authorization, request/command mapping, diagnosis persistence, schema compatibility, and acceptance-test coverage are in place. Consumers must not rely on the legacy examination endpoints.
