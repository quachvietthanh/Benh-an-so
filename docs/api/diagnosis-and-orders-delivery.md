# Diagnosis & Clinical Orders API

> Module: **NCL-04 - Khám bệnh & Bệnh án điện tử**
> 
> Base URL: `http://localhost:8080/api/v1`
>
> Auth: Bearer Token (JWT)

---

## 1. Danh mục chẩn đoán (Diagnosis Catalog)

### GET /diagnosis-catalog

Tra cứu danh mục ICD-10 theo mã hoặc tên bệnh.

**Query Parameters:**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `search` | string | No | Từ khóa tìm kiếm (mã ICD-10 hoặc tên bệnh) |

**Sample Request:**
```
GET /api/v1/diagnosis-catalog?search=cold
Authorization: Bearer <token>
```

**Sample Response (200 OK):**
```json
[
  {
    "id": "a1000000-0000-0000-0000-000000000019",
    "code": "J00",
    "name": "Common cold",
    "description": "Viêm mũi hầu cấp tính",
    "active": true,
    "createdAt": "2026-07-30T01:00:00Z",
    "updatedAt": null
  }
]
```

**Roles allowed:** `ADMIN`, `DOCTOR`

---

## 2. Ghi chẩn đoán (Record Diagnosis)

### POST /examinations/{examinationId}/diagnosis

Ghi chẩn đoán chính và chẩn đoán phụ cho một lượt khám.

**Path Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `examinationId` | UUID (BINARY(16)) | ID của lượt khám (Visit/Examination) |

**Request Body:**
```json
{
  "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000019",
  "primaryIcdCode": "J00",
  "primaryIcdName": "Common cold",
  "secondaryIcdCodes": [
    {
      "code": "R50.9",
      "name": "Fever, unspecified"
    }
  ],
  "clinicalNotes": "Patient presents with runny nose, mild fever"
}
```

**Validation Rules:**
- `primaryIcdCode`: required, not blank
- `primaryIcdName`: required, not blank
- QTN-07: Visit must be active (`WAITING`, `IN_PROGRESS`, or `WAITING_FOR_RESULT`)

**Sample Response (200 OK):**
```json
{
  "id": "generated-uuid",
  "visitId": "examination-id",
  "doctorId": "doctor-uuid",
  "primaryIcdCode": "J00",
  "primaryIcdName": "Common cold",
  "secondaryDiagnoses": [
    {
      "id": null,
      "code": "R50.9",
      "name": "Fever, unspecified"
    }
  ],
  "clinicalNotes": "Patient presents with runny nose, mild fever",
  "diagnosedAt": "2026-07-30T01:00:00Z",
  "clinicalOrders": []
}
```

**Error Responses:**

| Status | Description |
|--------|-------------|
| 400 | Validation error or visit not found |
| 403 | Insufficient permissions |
| 409 | Visit is COMPLETED or CANCELLED (QTN-07) |

**Roles allowed:** `ADMIN`, `DOCTOR`

---

## 3. Xem chẩn đoán (Get Diagnosis)

### GET /examinations/{examinationId}/diagnosis

Lấy thông tin chẩn đoán và các chỉ định cận lâm sàng của một lượt khám.

**Sample Request:**
```
GET /api/v1/examinations/{examinationId}/diagnosis
Authorization: Bearer <token>
```

**Sample Response (200 OK):**
```json
{
  "id": "diagnosis-id",
  "visitId": "examination-id",
  "doctorId": "doctor-uuid",
  "primaryIcdCode": "J00",
  "primaryIcdName": "Common cold",
  "secondaryDiagnoses": [],
  "clinicalNotes": "Patient stable",
  "diagnosedAt": "2026-07-30T01:00:00Z",
  "clinicalOrders": [
    {
      "id": "order-id",
      "orderCode": "ORD-1712345678000",
      "serviceCode": "LAB-GLU",
      "serviceName": "Blood glucose",
      "status": "ORDERED",
      "orderedAt": "2026-07-30T01:00:00Z"
    }
  ]
}
```

**Roles allowed:** `ADMIN`, `DOCTOR`, `NURSE`

---

## 4. Tạo chỉ định cận lâm sàng (Create Clinical Order)

### POST /examinations/{examinationId}/clinical-orders

Tạo chỉ định cận lâm sàng cho một lượt khám.

**Request Body:**
```json
{
  "clinicalReason": "Check blood glucose levels",
  "items": [
    {
      "serviceId": "f0000000-0000-0000-0000-000000000001",
      "serviceCode": "LAB-GLU",
      "serviceName": "Blood glucose",
      "instruction": "Fasting sample required"
    },
    {
      "serviceId": "f0000000-0000-0000-0000-000000000002",
      "serviceCode": "IMG-CTH",
      "serviceName": "Head CT scan",
      "instruction": "Non-contrast"
    }
  ]
}
```

**Validation Rules:**
- `items`: required, must not be empty
- Each item requires `serviceCode` and `serviceName` (not empty)
- QTN-13: Order is linked to the examination encounter
- QTN-07: Visit must be active

**Sample Response (200 OK):**
```json
{
  "id": "order-uuid",
  "orderCode": "ORD-1712345678000",
  "visitId": "examination-id",
  "patientId": "patient-uuid",
  "orderedBy": "doctor-uuid",
  "clinicalReason": "Check blood glucose levels",
  "status": "ORDERED",
  "orderedAt": "2026-07-30T01:00:00Z",
  "completedAt": null,
  "items": [
    {
      "id": "item-uuid-1",
      "serviceCode": "LAB-GLU",
      "serviceName": "Blood glucose",
      "instruction": "Fasting sample required",
      "status": "ORDERED"
    },
    {
      "id": "item-uuid-2",
      "serviceCode": "IMG-CTH",
      "serviceName": "Head CT scan",
      "instruction": "Non-contrast",
      "status": "ORDERED"
    }
  ]
}
```

**Roles allowed:** `ADMIN`, `DOCTOR`

---

## 5. Business Rules Enforced

| Rule | Description | Enforced In |
|------|-------------|-------------|
| **QTN-07** | Lock diagnosis after encounter completed. Diagnosis & orders cannot be modified once visit status is `COMPLETED` or `CANCELLED`. | `ExaminationDiagnosisService`, `CreateClinicalOrderService` |
| **QTN-11** | Only DOCTOR role can record diagnosis and clinical orders. | `@PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")` on controllers |
| **QTN-13** | Each clinical order must be linked to an examination encounter. | `CreateClinicalOrderService` validates `visitId` |

---

## 6. Postman Collection

### Environment Variables
```json
{
  "baseUrl": "http://localhost:8080/api/v1",
  "token": "<jwt-token>"
}
```

### Diagnosis Catalog Search
```json
{
  "name": "Search Diagnosis Catalog",
  "request": {
    "method": "GET",
    "url": "{{baseUrl}}/diagnosis-catalog?search=cold",
    "header": [
      { "key": "Authorization", "value": "Bearer {{token}}" }
    ]
  }
}
```

### Record Diagnosis
```json
{
  "name": "Record Diagnosis",
  "request": {
    "method": "POST",
    "url": "{{baseUrl}}/examinations/{{examinationId}}/diagnosis",
    "header": [
      { "key": "Authorization", "value": "Bearer {{token}}" },
      { "key": "Content-Type", "value": "application/json" }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\"diagnosisCatalogId\": \"a1000000-0000-0000-0000-000000000019\", \"primaryIcdCode\": \"J00\", \"primaryIcdName\": \"Common cold\", \"secondaryIcdCodes\": [{\"code\": \"R50.9\", \"name\": \"Fever\"}], \"clinicalNotes\": \"Mild symptoms\"}"
    }
  }
}
```

### Create Clinical Order
```json
{
  "name": "Create Clinical Order",
  "request": {
    "method": "POST",
    "url": "{{baseUrl}}/examinations/{{examinationId}}/clinical-orders",
    "header": [
      { "key": "Authorization", "value": "Bearer {{token}}" },
      { "key": "Content-Type", "value": "application/json" }
    ],
    "body": {
      "mode": "raw",
      "raw": "{\"clinicalReason\": \"Check glucose\", \"items\": [{\"serviceCode\": \"LAB-GLU\", \"serviceName\": \"Blood glucose\", \"instruction\": \"Fasting\"}]}"
    }
  }
}
```

### Get Diagnosis
```json
{
  "name": "Get Diagnosis & Orders",
  "request": {
    "method": "GET",
    "url": "{{baseUrl}}/examinations/{{examinationId}}/diagnosis",
    "header": [
      { "key": "Authorization", "value": "Bearer {{token}}" }
    ]
  }
}
```
