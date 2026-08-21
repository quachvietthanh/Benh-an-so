# Tra cứu Hồ sơ Bệnh án & Nhật ký truy cập (NCL-04-CN-004)

> **Epic:** NCL-04 — Khám bệnh & Bệnh án điện tử
> **Subtask:** CN-004 — Tra cứu hồ sơ bệnh án (Xem chi tiết / Xem lịch sử + Audit log)
> **Phạm vi:** Backend REST API (bệnh án được **khóa** sau khi lượt khám kết thúc — QTN-07)

## Mục tiêu

Cung cấp API để hiển thị nội dung hồ sơ bệnh án điện tử cho Bác sĩ / Điều dưỡng khi khám bệnh:

1. **Xem chi tiết hồ sơ theo lượt khám** — thông tin bệnh nhân, thông tin lượt khám, chẩn đoán ICD-10 (chính/phụ), nội dung bệnh án, trạng thái.
2. **Xem lịch sử hồ sơ theo bệnh nhân** — tất cả hồ sơ của các lượt khám trước, mới nhất trước.
3. **Nhật ký truy cập (Audit log)** — mọi lượt đọc hồ sơ đều được ghi log (QTN-02) vào bảng `medical_record_access_logs` (đã có từ migration V10).

## Quy tắc nghiệp vụ (QTN)

| Mã | Quy tắc | Thực thi |
|----|---------|----------|
| QTN-01 | Chỉ người dùng đã đăng nhập mới được xem bệnh án | Spring Security + JWT |
| QTN-02 | Mọi lượt truy cập bệnh án phải ghi audit log | `MedicalRecordAccessAuditService` (ghi `medical_record_access_logs`) |
| QTN-07 | Bệnh án / chẩn đoán bị **khóa** sau khi lượt khám `COMPLETED` | `MedicalRecord.lock()` → `LOCKED`; API chỉ đọc không sửa |

## Endpoints

> Base URL: `http://localhost:8080/api/v1` — Header: `Authorization: Bearer <token>`
>
> **Lưu ý về URL:** `server.servlet.context-path=/api/v1` nên runtime URL là `http://localhost:8080/api/v1/medical-records/...`.
> Controller khai báo mapping **tương đối** `@RequestMapping("/medical-records")` (KHÔNG bao gồm `/api/v1`).
> Trong MockMvc test (không qua servlet context-path), request path phải khớp mapping controller: `/medical-records/...`.

| Method | Endpoint | Mô tả | Vai trò |
|--------|----------|-------|---------|
| `GET` | `/medical-records/visits/{visitId}` | Chi tiết hồ sơ bệnh án theo lượt khám (kèm chẩn đoán ICD-10) | ADMIN, DOCTOR, NURSE |
| `GET` | `/medical-records/patient/{patientId}` | Lịch sử hồ sơ bệnh án của bệnh nhân | ADMIN, DOCTOR, NURSE |
| `GET` | `/medical-records/{medicalRecordId}` | Chi tiết hồ sơ (nội dung) | ADMIN, DOCTOR, NURSE |
| `GET` | `/medical-records/{medicalRecordId}/access-logs` | Nhật ký truy cập theo hồ sơ | ADMIN, DOCTOR, NURSE |
| `GET` | `/medical-records/access-logs?patientId={patientId}` | Nhật ký truy cập theo bệnh nhân | ADMIN, DOCTOR, NURSE |

## Ký bệnh án

`POST /medical-records/{medicalRecordId}/sign`

Permission: `MEDICAL_RECORD_UPDATE_STATUS`. Ngoài RBAC, chỉ bác sĩ được gán cho lượt khám mới được ký.

Request body là tùy chọn. Khi không truyền `signatureData`, hệ thống tạo chữ ký mô phỏng.

```json
{
  "signatureData": "DR_SIM_SIG"
}
```

Bệnh án phải có ít nhất một chẩn đoán và nội dung bắt buộc. Lifecycle là
`OPEN` → `SIGNED` → `LOCKED`; trạng thái `SIGNED` đã khóa mọi sửa đổi nội dung trực tiếp.
Một lần ký thứ hai trả về conflict và không tạo chữ ký/audit `SIGN` thành công thứ hai.

Response là `MedicalRecordResponse`, bao gồm `status`, `signatureData`, `signedAt` và `signedBy`.

> ⚠️ Audit log được tự động ghi khi gọi **các endpoint đọc hồ sơ** (`getDetailByVisitId`, `getHistoryByPatientId`, `getById`, `getByVisitId`).

## 1. GET /medical-records/visits/{visitId}

Trả về **thông tin đầy đủ để hiển thị bệnh án** bao gồm: thông tin bệnh nhân, thông tin lượt khám, bác sĩ điều trị, chẩn đoán chính/phụ, nội dung bệnh án và trạng thái.

### Response 200 OK

```json
{
  "patient": {
    "id": "e7b1f0e4-2b8e-4c5a-9f6d-3d3e8a2b1c0a",
    "patientCode": "BN-0001",
    "fullName": "Nguyễn Văn A",
    "dateOfBirth": "1990-01-01",
    "gender": "MALE",
    "phone": "0900000000",
    "identityNumber": "001090000001",
    "insuranceNumber": "GD6900000000001"
  },
  "visit": {
    "id": "f8c2e5d0-1a3b-4c6d-8e9f-2a4b6c8d0e1f",
    "visitCode": "VS-2025-0001",
    "visitType": "WALK_IN",
    "status": "COMPLETED",
    "visitAt": "2025-07-30T08:30:00Z",
    "startedAt": "2025-07-30T08:45:00Z",
    "completedAt": "2025-07-30T09:20:00Z",
    "reason": "Đau đầu kéo dài",
    "note": null,
    "doctorId": "d5a1c3e2-4f6a-4b8c-9d0e-1a2b3c4d5e6f",
    "doctorName": "BS. Trần Văn B"
  },
  "medicalRecordId": "9b2d4f6e-8a0c-4e2f-9b3d-5f7a9c1e3d5b",
  "chiefComplaint": "Đau đầu vùng trán kéo dài 3 ngày",
  "symptoms": "Đau nhói từng cơn, kèm chóng mặt",
  "medicalHistory": "Tiền sử tăng huyết áp 2 năm",
  "physicalExamination": "Mạch 80, HA 140/90",
  "clinicalProgress": "Bệnh nhân tỉnh, tiếp xúc tốt",
  "treatmentPlan": "Nghỉ ngơi, theo dõi huyết áp",
  "doctorInstructions": "Tái khám sau 5 ngày",
  "conclusion": "Đau đầu do tăng huyết áp",
  "status": "LOCKED",
  "lockedAt": "2025-07-30T09:20:00Z",
  "lockedBy": "d5a1c3e2-4f6a-4b8c-9d0e-1a2b3c4d5e6f",
  "primaryIcdCode": "G43",
  "primaryIcdName": "Migraine",
  "secondaryIcdCodes": ["J00"],
  "diagnoses": [
    {
      "id": "c1b3d5f7-9a2c-4e4f-8b6d-0a1c3e5f7a9b",
      "medicalRecordId": "9b2d4f6e-8a0c-4e2f-9b3d-5f7a9c1e3d5b",
      "diagnosisCode": "G43",
      "diagnosisName": "Migraine",
      "diagnosisType": "PRIMARY",
      "note": "Chẩn đoán chính",
      "diagnosedBy": "d5a1c3e2-4f6a-4b8c-9d0e-1a2b3c4d5e6f",
      "diagnosedAt": "2025-07-30T09:05:00Z"
    },
    {
      "id": "2d4f6a8c-0b3e-4f5a-9c7e-1b2d4f6a8c0b",
      "medicalRecordId": "9b2d4f6e-8a0c-4e2f-9b3d-5f7a9c1e3d5b",
      "diagnosisCode": "J00",
      "diagnosisName": "Viêm mũi họng cấp",
      "diagnosisType": "SECONDARY",
      "note": null,
      "diagnosedBy": "d5a1c3e2-4f6a-4b8c-9d0e-1a2b3c4d5e6f",
      "diagnosedAt": "2025-07-30T09:05:00Z"
    }
  ]
}
```

### Error

| Status | Khi nào |
|--------|---------|
| `401` | Thiếu / sai token |
| `403` | Không có quyền (`RECEPTIONIST`, `PATIENT`...) |
| `404` | Không tìm thấy hồ sơ theo `visitId` |

### Postman

```
GET {{baseUrl}}/medical-records/visits/f8c2e5d0-1a3b-4c6d-8e9f-2a4b6c8d0e1f
Authorization: Bearer {{token}}
```

## 2. GET /medical-records/patient/{patientId}

Trả về **danh sách lịch sử hồ sơ bệnh án** của một bệnh nhân (chỉ những lượt khám **đã có** hồ sơ), sắp xếp theo thời gian khám giảm dần. Mỗi phần tử có cấu trúc giống Response của endpoint 1.

### Response 200 OK

```json
[
  {
    "patient": { "...": "như trên" },
    "visit": { "...": "như trên" },
    "medicalRecordId": "9b2d4f6e-...",
    "chiefComplaint": "Đau đầu vùng trán kéo dài 3 ngày",
    "status": "LOCKED",
    "primaryIcdCode": "G43",
    "primaryIcdName": "Migraine",
    "secondaryIcdCodes": ["J00"],
    "diagnoses": [ "...": "như trên" ]
  }
]
```

### Postman

```
GET {{baseUrl}}/medical-records/patient/e7b1f0e4-2b8e-4c5a-9f6d-3d3e8a2b1c0a
Authorization: Bearer {{token}}
```

## 3. Nhật ký truy cập (Audit log — QTN-02)

Mỗi lần một người dùng xem bệnh án, hệ thống tự ghi một dòng vào bảng `medical_record_access_logs`:

| Cột | Nội dung |
|-----|----------|
| `patient_id` | Bệnh nhân của hồ sơ |
| `visit_id` | Lượt khám liên quan |
| `medical_record_id` | Hồ sơ được xem |
| `accessed_by` | User id người xem |
| `action` | `RECORD_VIEW` / `HISTORY_VIEW` |
| `accessed_at` | Thời điểm xem |

**Lệnh kiểm tra log (sau khi đã gọi API xem):**

```sql
SELECT * FROM medical_record_access_logs ORDER BY accessed_at DESC LIMIT 10;
```

## Kiến trúc triển khai (Hexagonal)

```
adapter/inbound/rest/controller
└── MedicalRecordController        (@RequestMapping("/medical-records") — tương đối,
                                    context-path /api/v1 tự prefix ở runtime:
                                    GET /api/v1/medical-records/visits/{visitId},
                                    GET /api/v1/medical-records/patient/{patientId})
          │
          ▼
port/inbound/medicalrecord
└── GetMedicalRecordUseCase        (getDetailByVisitId, getHistoryByPatientId)
          │
          ▼
application/ucservice/medicalrecord
├── GetMedicalRecordService        (orchestration + audit logging)
└── MedicalRecordResultMapper      (map domain → MedicalRecordDetailResult)
          │
          ▼
port/outbound/repository/crudRepository
├── MedicalRecordRepository
├── MedicalRecordDiagnosisRepository   (★ mới: query chẩn đoán ICD-10)
├── VisitRepository                    (★ mới: findByPatientIdOrderByVisitAtDesc)
├── PatientRepository
└── UserRepository
```

**Class mới trong NCL-04-CN-004:**

| Layer | Class |
|-------|-------|
| Port DTO | `MedicalRecordDetailResult`, `MedicalRecordDiagnosisResult` |
| Inbound Port | `GetMedicalRecordUseCase` (mở rộng) |
| Outbound Port | `MedicalRecordDiagnosisRepository` (mới) |
| Application | `GetMedicalRecordService` (mở rộng), `MedicalRecordResultMapper` (mở rộng) |
| Persistence | `JpaMedicalRecordDiagnosisRepository`, `MedicalRecordDiagnosisRepositoryAdapter` (mới) |
| REST Adapter | `MedicalRecordController.getPatientMedicalRecords` (mới — endpoint chuyển từ `PatientController`), `MedicalRecordDetailRestMapper`, `MedicalRecordDetailResponse`, `MedicalRecordDiagnosisResponse` (mới) |

> **Flyway:** Không cần migration mới — bảng `medical_record_access_logs` đã có từ `V10__create_medical_record_access_logs.sql`; dữ liệu chẩn đoán ICD-10 được lưu ở bảng `medical_record_diagnoses` (V12) và tra cứu qua `diagnosis_catalog` (V13).
