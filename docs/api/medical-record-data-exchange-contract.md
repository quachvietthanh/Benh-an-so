# Đặc tả API: Xuất Hồ Sơ Bệnh Án Theo Cấu Trúc Trao Đổi Dữ Liệu (NCL-11-CN-007)

> **Module:** NCL-11 — Ký duyệt và lưu trữ hồ sơ bệnh án  
> **User Story:** NCL-11-CN-007 — Xuất hồ sơ bệnh án theo cấu trúc trao đổi dữ liệu  
> **Phạm vi:** Backend REST API xuất dữ liệu bệnh án điện tử chuẩn trao đổi (5 khối thông tin)  
> **Quy tắc liên quan:** QTN-41, QTN-22, QTN-19  

---

## 1. Mục Tiêu & Mô Tả Nghiệp Vụ

Cung cấp API cho **Quản trị viên (`ADMIN`)** và **Quản lý phòng khám (`MANAGER`)** để xuất một hoặc nhiều hồ sơ bệnh án đã ký ra tệp định dạng chuẩn trao đổi dữ liệu y tế (**JSON format**), phục vụ việc tích hợp, liên thông dữ liệu với các hệ thống y tế khác (HIS, LIS, PACS bên ngoài hoặc cổng liên thông Bộ Y Tế).

### 1.1. Điều kiện tiên quyết (Preconditions)
1. **Hồ sơ đã ký (`QTN-41`):** Bệnh án phải ở trạng thái `SIGNED`, `LOCKED`, hoặc `ARCHIVED`. Các hồ sơ ở trạng thái `DRAFT` hoặc `OPEN` sẽ bị từ chối với lỗi `MEDICAL_RECORD_NOT_SIGNED` (HTTP 400).
2. **Chẩn đoán gắn mã bệnh (`QTN-22`):** Bệnh án phải có chẩn đoán chính gắn mã bệnh ICD-10 hợp lệ trong danh mục mã bệnh của hệ thống.
3. **Phân quyền:** Chỉ tài khoản có vai trò `ADMIN` hoặc `MANAGER` mang quyền `MEDICAL_RECORD_EXPORT`.
4. **Nguyên tắc toàn vẹn (All-or-Nothing):** Trong một yêu cầu xuất hàng loạt (batch export), nếu có bất kỳ hồ sơ nào chưa ký hoặc thiếu mã bệnh, toàn bộ yêu cầu bị từ chối để ngăn chặn xuất dữ liệu dở dang hoặc rò rỉ dữ liệu chưa hoàn tất.

### 1.2. Kết quả sau hoàn thành (Postconditions)
1. Xuất ra tệp JSON có cấu trúc định nghĩa chuẩn, có thể đọc và parse lại được mà không mất bất kỳ trường bắt buộc nào (`TC-03`).
2. Ghi nhận nhật ký kiểm toán hệ thống (`audit_logs`) ghi nhận người xuất, phạm vi lượt khám/hồ sơ, và thời điểm xuất (`TC-04`).
3. Ghi nhận nhật ký truy cập bệnh án (`medical_record_access_logs`) với action `EXPORT` cho từng hồ sơ được xuất.

---

## 2. Danh Sách Endpoints

> **Base URL:** `http://localhost:8080/api/v1`  
> **Authentication:** Bearer JWT Token (`Authorization: Bearer <token>`)

| Method | Endpoint | Quyền (Permission) | Vai trò được phép | Mô tả |
|---|---|---|---|---|
| `POST` | `/medical-records/export` | `MEDICAL_RECORD_EXPORT` | `ADMIN`, `MANAGER` | Xuất danh sách 1 hoặc nhiều hồ sơ bệnh án theo cấu trúc trao đổi dữ liệu |
| `GET` | `/medical-records/{medicalRecordId}/export` | `MEDICAL_RECORD_EXPORT` | `ADMIN`, `MANAGER` | Xuất nhanh 1 hồ sơ bệnh án cụ thể theo cấu trúc trao đổi dữ liệu |

---

## 3. Chi Tiết Endpoints

### 3.1. POST /medical-records/export

Xuất một hoặc nhiều lượt khám/hồ sơ bệnh án thành tệp dữ liệu có cấu trúc.

#### Request Headers
```http
Content-Type: application/json
Authorization: Bearer <jwt-token>
```

#### Request Body
```json
{
  "medicalRecordIds": [
    "9b2d4f6e-8a0c-4e2f-9b3d-5f7a9c1e3d5b",
    "c8a1b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c"
  ],
  "visitIds": [],
  "format": "JSON"
}
```

*Lưu ý:* Có thể truyền `medicalRecordIds` hoặc `visitIds`. Tối đa 100 ID mỗi lần xuất để đảm bảo hiệu năng.

#### Response Headers (200 OK)
```http
Content-Disposition: attachment; filename="emr-exchange-bundle-1727164800000.json"
Content-Type: application/json
Content-Length: 4852
```

#### Response Body (Nếu xuất nhiều hồ sơ: Bundle format)
```json
{
  "bundleId": "f1e2d3c4-b5a6-7890-abcd-ef1234567890",
  "exchangeVersion": "1.0",
  "exportedAt": "2026-09-24T08:30:00Z",
  "exportedBy": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "totalRecords": 2,
  "records": [
    {
      "exchangeVersion": "1.0",
      "generatedAt": "2026-09-24T08:30:00Z",
      "facility": {
        "clinicName": "Phòng khám Đa khoa Tiêu chuẩn",
        "address": "123 Đường Y Tế, Quận 1, TP. HCM",
        "phone": "0281234567",
        "email": null
      },
      "patient": {
        "patientId": "e7b1f0e4-2b8e-4c5a-9f6d-3d3e8a2b1c0a",
        "patientCode": "BN-2026-0001",
        "fullName": "Nguyễn Văn A",
        "dateOfBirth": "1985-05-15",
        "gender": "MALE",
        "phone": "0901234567",
        "identityNumber": "079085000123",
        "insuranceNumber": "GD4790000000123",
        "address": "Số 45 Lê Lợi, Quận 1, TP. HCM"
      },
      "encounter": {
        "visitId": "f8c2e5d0-1a3b-4c6d-8e9f-2a4b6c8d0e1f",
        "visitCode": "VS-2026-0001",
        "visitAt": "2026-09-24T08:15:00Z",
        "startedAt": "2026-09-24T08:30:00Z",
        "completedAt": "2026-09-24T09:15:00Z",
        "visitType": "WALK_IN",
        "reason": "Đau đầu, chóng mặt",
        "doctorId": "d5a1c3e2-4f6a-4b8c-9d0e-1a2b3c4d5e6f",
        "doctorName": "BS. Trần Văn B"
      },
      "clinicalRecord": {
        "medicalRecordId": "9b2d4f6e-8a0c-4e2f-9b3d-5f7a9c1e3d5b",
        "status": "SIGNED",
        "signedAt": "2026-09-24T09:10:00Z",
        "signedByDoctorId": "d5a1c3e2-4f6a-4b8c-9d0e-1a2b3c4d5e6f",
        "signedByDoctorName": "BS. Trần Văn B",
        "signatureData": "SIMULATED_SIGNATURE:...",
        "chiefComplaint": "Đau nửa đầu phải",
        "symptoms": "Đau giật từng cơn",
        "medicalHistory": "Tiền sử tăng huyết áp",
        "physicalExamination": "Mạch 78, HA 135/85 mmHg",
        "clinicalProgress": "Tiếp xúc tốt, tỉnh táo",
        "treatmentPlan": "Nghỉ ngơi, điều trị nội khoa",
        "doctorInstructions": "Uống thuốc theo đơn",
        "conclusion": "Đau nửa đầu do tăng huyết áp",
        "revisitDate": "2026-10-01"
      },
      "diagnoses": [
        {
          "diagnosisType": "PRIMARY",
          "diagnosisCode": "G43",
          "diagnosisName": "Migraine (Đau nửa đầu)",
          "note": "Chẩn đoán chính",
          "diagnosedAt": "2026-09-24T08:50:00Z"
        }
      ],
      "clinicalOrders": [
        {
          "orderId": "a1b2c3d4-0000-0000-0000-000000000001",
          "orderCode": "ORD-2026-0001",
          "orderedAt": "2026-09-24T08:35:00Z",
          "services": [
            {
              "itemId": "e1f2g3h4-0000-0000-0000-000000000001",
              "serviceCode": "XN-CTM",
              "serviceName": "Tổng phân tích tế bào máu ngoại vi",
              "serviceType": null,
              "instruction": "Lấy máu lúc đói",
              "status": "COMPLETED"
            }
          ]
        }
      ],
      "clinicalResults": [
        {
          "resultId": "b1c2d3e4-0000-0000-0000-000000000001",
          "serviceCode": null,
          "serviceName": null,
          "resultType": "NUMERIC",
          "numericValue": 142.0,
          "textValue": null,
          "unit": "g/L",
          "referenceRange": "130.0 - 170.0",
          "abnormalFlag": "NORMAL",
          "conclusion": "Bình thường",
          "status": "FINALIZED"
        }
      ],
      "prescriptions": [
        {
          "prescriptionId": "c1d2e3f4-0000-0000-0000-000000000001",
          "prescriptionCode": "RX-2026-0001",
          "status": "DISPENSED",
          "prescribedAt": "2026-09-24T09:05:00Z",
          "interconnectionReceiptCode": null,
          "note": "Uống sau ăn",
          "medications": [
            {
              "medicineCode": "MED-001",
              "medicineName": "Paracetamol 500mg",
              "activeIngredient": "Paracetamol",
              "dosageForm": null,
              "quantity": 10,
              "unit": "Viên",
              "usageInstructions": "Uống khi đau",
              "morningDose": null,
              "noonDose": null,
              "afternoonDose": null,
              "eveningDose": null,
              "daysSupply": 5
            }
          ]
        }
      ]
    }
  ]
}
```

---

### 3.2. GET /medical-records/{medicalRecordId}/export

Xuất nhanh một hồ sơ bệnh án đơn lẻ theo ID.

#### Request Headers
```http
Authorization: Bearer <jwt-token>
```

#### Response Headers (200 OK)
```http
Content-Disposition: attachment; filename="emr-exchange-BN-2026-0001-VS-2026-0001.json"
Content-Type: application/json
```

#### Response Body
Trả về đối tượng JSON `MedicalRecordExchangeDocument` trực tiếp.

---

## 4. Xử Lý Lỗi (Error Responses)

| HTTP Code | Mã lỗi (DomainErrorCode) | Nguyên nhân & Mô tả |
|---|---|---|
| `400 Bad Request` | `MEDICAL_RECORD_NOT_SIGNED` | Có hồ sơ chưa ở trạng thái đã ký (`DRAFT`/`OPEN`) theo quy tắc `QTN-41`. Thông báo: *"Medical record with ID ... must be signed before it can be exported."* |
| `400 Bad Request` | `VALIDATION_ERROR` | Hồ sơ thiếu chẩn đoán chính hoặc chẩn đoán chưa gắn mã bệnh ICD-10 theo `QTN-22`, hoặc danh sách ID rỗng, hoặc vượt quá 100 hồ sơ. |
| `401 Unauthorized` | `UNAUTHORIZED` | Không có JWT token hoặc token hết hạn/không hợp lệ. |
| `403 Forbidden` | `ACCESS_DENIED` | Người dùng không có quyền `MEDICAL_RECORD_EXPORT` (ví dụ tài khoản DOCTOR, PHARMACIST, RECEPTIONIST, PATIENT). |
| `404 Not Found` | `MEDICAL_RECORD_NOT_FOUND` | Không tìm thấy hồ sơ bệnh án theo ID yêu cầu. |

---

## 5. Nhật Ký Kiểm Toán (Audit Logging - TC-04)

Mỗi lần xuất thành công, hệ thống tự động ghi nhật ký vào 2 nơi:

1. **`audit_logs` (Nhật ký thao tác hệ thống):**
   - `action_type`: `EXPORT`
   - `resource_type`: `MEDICAL_RECORD`
   - `resource_id`: ID hồ sơ (nếu xuất đơn) hoặc `null` (nếu xuất bundle nhiều hồ sơ)
   - `detail`: JSON ghi nhận `exportedBy`, `recordCount`, `medicalRecordIds`, `visitIds`, `format`, `exportedAt`
2. **`medical_record_access_logs` (Nhật ký truy cập hồ sơ bệnh án):**
   - `patient_id`: ID bệnh nhân
   - `visit_id`: ID lượt khám
   - `medical_record_id`: ID hồ sơ bệnh án
   - `accessed_by`: ID người thực hiện xuất
   - `action`: `EXPORT`
   - `detail`: *"Medical record exported according to standard data exchange structure (NCL-11-CN-007)"*
   - `accessed_at`: Thời điểm xuất
