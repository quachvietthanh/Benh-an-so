# Medical-record Template Management API

> User Story: **NCL-13-CN-003 — Quản lý mẫu bệnh án theo chuyên khoa**  
> Status: **Implemented in backend migration V29**
> Base URL: `http://localhost:8080/api/v1`  
> Auth: Bearer JWT

## 1. Scope and authorization

This contract manages reusable medical-record templates. A template is assigned to exactly one specialty and configures the presentation and requiredness of existing medical-record fields. It does not create arbitrary clinical fields.

All endpoints in this document require `MEDICAL_RECORD_TEMPLATE_MANAGE`. The default `ADMIN` role receives that permission. A caller without the permission receives `403 Forbidden`.

Out of scope for this story:

- specialty CRUD (specialties are seeded reference data);
- applying a template to a medical record (NCL-13-CN-004);
- hard deletion of a template.

## 2. Canonical medical-record fields

Every template section must use one of these `fieldCode` values:

| `fieldCode` | Existing medical-record field |
| --- | --- |
| `CHIEF_COMPLAINT` | `chiefComplaint` |
| `SYMPTOMS` | `symptoms` |
| `MEDICAL_HISTORY` | `medicalHistory` |
| `PHYSICAL_EXAMINATION` | `physicalExamination` |
| `CLINICAL_PROGRESS` | `clinicalProgress` |
| `TREATMENT_PLAN` | `treatmentPlan` |
| `DOCTOR_INSTRUCTIONS` | `doctorInstructions` |
| `CONCLUSION` | `conclusion` |

`label` is the specialty-specific display label. In NCL-13-CN-004, `required` is checked when the doctor signs a medical record that has applied this immutable version; it does not block saving a draft.

## 3. Data shape

### Specialty response

```json
{
  "id": "9f5b0c8a-4a29-4e0c-b63c-9cf1bc9d3f17",
  "code": "INTERNAL_MEDICINE",
  "name": "Internal medicine",
  "active": true
}
```

### Template summary response

```json
{
  "id": "ed17af6e-f9e5-4c44-849d-8e6849ae6d01",
  "specialty": {
    "id": "9f5b0c8a-4a29-4e0c-b63c-9cf1bc9d3f17",
    "code": "INTERNAL_MEDICINE",
    "name": "Internal medicine",
    "active": true
  },
  "name": "Internal medicine initial examination",
  "active": true,
  "defaultTemplate": true,
  "currentVersionNo": 1,
  "createdAt": "2026-08-26T00:00:00Z",
  "updatedAt": null
}
```

### Template detail response

The detail response extends the summary with the current version's sections.

```json
{
  "id": "ed17af6e-f9e5-4c44-849d-8e6849ae6d01",
  "specialty": {
    "id": "9f5b0c8a-4a29-4e0c-b63c-9cf1bc9d3f17",
    "code": "INTERNAL_MEDICINE",
    "name": "Internal medicine",
    "active": true
  },
  "name": "Internal medicine initial examination",
  "active": true,
  "defaultTemplate": true,
  "currentVersionNo": 1,
  "sections": [
    {
      "fieldCode": "CHIEF_COMPLAINT",
      "label": "Chief complaint",
      "required": true,
      "displayOrder": 1
    },
    {
      "fieldCode": "SYMPTOMS",
      "label": "Symptoms",
      "required": true,
      "displayOrder": 2
    }
  ],
  "createdAt": "2026-08-26T00:00:00Z",
  "updatedAt": null
}
```

## 4. Endpoints

### `GET /system/specialties?active=true`

Returns seeded specialties. `active` is optional and defaults to `true`.

### `GET /system/medical-record-templates?specialtyId={uuid}&active={boolean}`

Returns template summaries. Both filters are optional. Results are ordered by specialty, default template first, then name.

### `GET /system/medical-record-templates/{templateId}`

Returns template detail, including the current version's ordered sections.

### `POST /system/medical-record-templates`

Creates an active template and version `1`.

```json
{
  "specialtyId": "9f5b0c8a-4a29-4e0c-b63c-9cf1bc9d3f17",
  "name": "Internal medicine initial examination",
  "makeDefault": true,
  "sections": [
    {
      "fieldCode": "CHIEF_COMPLAINT",
      "label": "Chief complaint",
      "required": true,
      "displayOrder": 1
    },
    {
      "fieldCode": "SYMPTOMS",
      "label": "Symptoms",
      "required": true,
      "displayOrder": 2
    }
  ]
}
```

Returns `201 Created` and the template detail. `makeDefault` defaults to `false`; the first active template of a specialty is made default regardless of that value.

### `PUT /system/medical-record-templates/{templateId}`

Updates a template's name and sections, creating a new immutable version. A template's specialty is immutable after creation; to move it to another specialty, create a new template for that specialty and deactivate the old template subject to the status rules.

```json
{
  "name": "Internal medicine initial examination v2",
  "sections": [
    {
      "fieldCode": "CHIEF_COMPLAINT",
      "label": "Chief complaint",
      "required": true,
      "displayOrder": 1
    }
  ]
}
```

Returns `200 OK` and the new current template detail.

### `PATCH /system/medical-record-templates/{templateId}/default`

Sets an active template as the specialty's default and atomically clears the former default.

Returns `200 OK`. An inactive template cannot become default.

### `PATCH /system/medical-record-templates/{templateId}/status`

Activates or deactivates a template.

```json
{
  "active": false,
  "replacementTemplateId": "42b76380-a53c-4a2d-a5cf-ae4188fd99fd"
}
```

`replacementTemplateId` is required only when deactivating the current default. It must identify another active template in the same specialty, which becomes default in the same transaction. A template that is the specialty's last active template cannot be deactivated.

Returns `200 OK`.

## 5. Validation and state rules

1. Template name is trimmed and compared case-insensitively within its specialty. A duplicate is rejected.
2. `specialtyId` must identify an active specialty when creating a template.
3. A template requires at least one section.
4. Each section requires a supported `fieldCode`, a non-blank `label`, and a positive `displayOrder`.
5. A template cannot contain duplicated `fieldCode` or `displayOrder` values.
6. Inactive templates cannot be selected as default.
7. A specialty must retain at least one active template.
8. A template has no hard-delete operation. A template version referenced by a medical record is retained permanently.

## 6. Error contract

| HTTP status | Error code | Condition |
| --- | --- | --- |
| `400` | `VALIDATION_FAILED` | Missing/blank fields, unsupported field code, no sections, duplicate section field/order, or an invalid enum value in the request body. |
| `400` | `INVALID_PARAMETER` | A path or query parameter cannot be parsed, including a malformed UUID. |
| `400` | `MALFORMED_JSON` | Request body is not valid JSON. |
| `401` | `AUTHENTICATION_FAILED` | Missing or invalid token. |
| `403` | `ACCESS_DENIED` | Caller lacks `MEDICAL_RECORD_TEMPLATE_MANAGE`. |
| `404` | `SPECIALTY_NOT_FOUND` | Requested specialty does not exist or is inactive for template creation. |
| `404` | `MEDICAL_RECORD_TEMPLATE_NOT_FOUND` | Requested template does not exist. |
| `409` | `MEDICAL_RECORD_TEMPLATE_NAME_DUPLICATE` | Another template in the specialty has the same normalized name. |
| `409` | `MEDICAL_RECORD_TEMPLATE_INACTIVE` | An inactive template is set as default. |
| `409` | `MEDICAL_RECORD_TEMPLATE_LAST_ACTIVE` | Deactivation would leave the specialty with no active template. |
| `409` | `MEDICAL_RECORD_TEMPLATE_DEFAULT_REPLACEMENT_REQUIRED` | A default template is deactivated without a valid replacement. |
| `409` | `MEDICAL_RECORD_TEMPLATE_INVALID_REPLACEMENT` | Replacement is the same template, inactive, missing, or belongs to another specialty. |

## 7. Acceptance-criteria mapping

| Workbook AC | Contract outcome |
| --- | --- |
| `NCL-13-CN-003-TC-01` | `POST` creates and assigns an Internal Medicine template with six valid sections. |
| `NCL-13-CN-003-TC-02` | Duplicate normalized template name in one specialty returns `409 MEDICAL_RECORD_TEMPLATE_NAME_DUPLICATE`. |
| `NCL-13-CN-003-TC-03` | Superseded by the approved rule: hard delete is unavailable; deactivation is allowed only when another active template remains, and default replacement rules hold. |
| `NCL-13-CN-003-TC-04` | A doctor calling any management endpoint receives `403 ACCESS_DENIED`. |
