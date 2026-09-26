# API Contract: Quản lý tiền sử dị ứng thuốc của bệnh nhân (Patient Medication Allergy Management)

> **Module:** NCL-02 - Quản lý hồ sơ bệnh nhân
>
> **User Story:** NCL-02-CN-005 - Quản lý tiền sử dị ứng thuốc của bệnh nhân
>
> **Business Rules:** QTN-26 (*Cảnh báo dị ứng thuốc khi kê đơn*), QTN-02 (*Ghi nhật ký truy cập bệnh án*), QTN-01 (*Phân quyền truy cập theo vai trò*), QTN-11 (*Chỉ bác sĩ ghi chuyên môn*)
>
> **Acceptance Criteria:** NCL-02-CN-005-TC-01, NCL-02-CN-005-TC-02, NCL-02-CN-005-TC-03, NCL-02-CN-005-TC-04
>
> **Base URL:** `/patients/{patientId}/allergies`
>
> **Auth:** Bearer Token (JWT)
>
> **Date:** 2026-09-07

---

## 1. Tổng quan nghiệp vụ & Phân quyền

1. **Ghi nhận dị ứng thuốc (TC-01)**:
   - Bác sĩ mở hồ sơ bệnh nhân trong hoặc ngoài lượt khám và ghi nhận hoạt chất hoặc nhóm thuốc gây dị ứng, mức độ phản ứng (`MILD`, `MODERATE`, `SEVERE`, `ANAPHYLAXIS`), biểu hiện phản ứng và ghi chú.
   - Quyền yêu cầu: `PATIENT_ALLERGY_WRITE` (chỉ cấp cho `DOCTOR` và `ADMIN`).
2. **Kiểm tra trùng lặp hoạt chất (TC-02)**:
   - Hệ thống chuẩn hóa tên hoạt chất (`normalized_allergen_name` = `lower(trim(allergenName))`).
   - Nếu bệnh nhân đã có một mục dị ứng active với hoạt chất này, hệ thống từ chối lưu và trả về mã lỗi HTTP `409 Conflict` (`PATIENT_ALLERGY_ALREADY_EXISTS`).
3. **Từ chối và lưu vết truy cập trái quyền (TC-03)**:
   - Nếu người dùng có vai trò không được phép (ví dụ Lễ tân `RECEPTIONIST`) cố gắng thực hiện các thao tác ghi nhận dị ứng, hệ thống chặn lại với HTTP `403 Forbidden` (`FORBIDDEN`) và tự động ghi nhật ký vi phạm (`ActionType.ACCESS_DENIED`, `ResourceType.PERMISSION`) vào `audit_logs`.
4. **Lưu lịch sử thay đổi dị ứng (TC-04 & QTN-02)**:
   - Khi Bác sĩ cập nhật hoặc xóa một mục dị ứng, hệ thống tự động ghi lại snapshot trạng thái trước khi thay đổi (`before_data`) và sau khi thay đổi (`after_data`), định danh người thực hiện (`changedBy`), và thời điểm vào bảng `patient_allergy_change_logs`.
   - Đồng thời, hệ thống ghi nhật ký kiểm toán vào `audit_logs` với `resourceType = ResourceType.PATIENT_ALLERGY`, `resourceId = allergyId`, và snapshot chi tiết chứa `patientId`.

---

## 2. Danh sách Endpoints

### 2.1. Thêm mục dị ứng mới
- **Method:** `POST`
- **Path:** `/patients/{patientId}/allergies`
- **Permission:** `PATIENT_ALLERGY_WRITE` (Bác sĩ, Admin)

#### Request Body
```json
{
  "allergenType": "MEDICATION",
  "allergenName": "Penicillin",
  "severity": "SEVERE",
  "reaction": "Nổi mề đay toàn thân, khó thở nhẹ",
  "notes": "Xảy ra năm 2022 sau khi tiêm thuốc",
  "visitId": "123e4567-e89b-12d3-a456-426614174000"
}
```

*Ghi chú trường:*
- `allergenType`: Tùy chọn, mặc định `"MEDICATION"`.
- `allergenName`: Bắt buộc, chuỗi từ 1-255 ký tự.
- `severity`: Bắt buộc, một trong các giá trị: `MILD`, `MODERATE`, `SEVERE`, `ANAPHYLAXIS`.
- `reaction`: Tùy chọn, tối đa 255 ký tự.
- `notes`: Tùy chọn.
- `visitId`: Tùy chọn (UUID lượt khám đang thực hiện nếu có).

#### Response (201 Created)
```json
{
  "id": "a0000000-0000-0000-0000-000000000001",
  "patientId": "70000000-0000-0000-0000-000000000001",
  "allergenType": "MEDICATION",
  "allergenName": "Penicillin",
  "severity": "SEVERE",
  "reaction": "Nổi mề đay toàn thân, khó thở nhẹ",
  "notes": "Xảy ra năm 2022 sau khi tiêm thuốc",
  "active": true,
  "createdBy": "22222222-2222-2222-2222-222222222222",
  "createdAt": "2026-09-07T07:30:00Z",
  "updatedBy": null,
  "updatedAt": "2026-09-07T07:30:00Z"
}
```

---

### 2.2. Lấy danh sách dị ứng của bệnh nhân
- **Method:** `GET`
- **Path:** `/patients/{patientId}/allergies`
- **Permission:** `PATIENT_ALLERGY_READ` (Bác sĩ, Dược sĩ, Quản lý, Admin)

#### Response (200 OK)
```json
[
  {
    "id": "a0000000-0000-0000-0000-000000000001",
    "patientId": "70000000-0000-0000-0000-000000000001",
    "allergenType": "MEDICATION",
    "allergenName": "Penicillin",
    "severity": "SEVERE",
    "reaction": "Nổi mề đay toàn thân, khó thở nhẹ",
    "notes": "Xảy ra năm 2022 sau khi tiêm thuốc",
    "active": true,
    "createdBy": "22222222-2222-2222-2222-222222222222",
    "createdAt": "2026-09-07T07:30:00Z",
    "updatedBy": null,
    "updatedAt": "2026-09-07T07:30:00Z"
  }
]
```

---

### 2.3. Cập nhật mục dị ứng
- **Method:** `PUT`
- **Path:** `/patients/{patientId}/allergies/{allergyId}`
- **Permission:** `PATIENT_ALLERGY_WRITE` (Bác sĩ, Admin)

#### Request Body
```json
{
  "allergenName": "Penicillin",
  "severity": "ANAPHYLAXIS",
  "reaction": "Khó thở dữ dội, tụt huyết áp",
  "notes": "Đã xác định phản vệ mức độ nguy kịch",
  "changeReason": "Bổ sung biểu hiện nặng theo lời kể gia đình"
}
```

#### Response (200 OK)
Trả về thông tin `PatientAllergyResponse` đã được cập nhật (`updatedBy`, `updatedAt` mới).

---

### 2.4. Xóa (vô hiệu hóa) mục dị ứng
- **Method:** `DELETE`
- **Path:** `/patients/{patientId}/allergies/{allergyId}`
- **Permission:** `PATIENT_ALLERGY_WRITE` (Bác sĩ, Admin)

#### Request Query / Body
- `reason`: Tùy chọn lý do xóa.

#### Response (204 No Content)

---

### 2.5. Tra cứu lịch sử thay đổi của mục dị ứng (TC-04)
- **Method:** `GET`
- **Path:** `/patients/{patientId}/allergies/{allergyId}/history`
- **Permission:** `PATIENT_ALLERGY_WRITE` (Bác sĩ, Admin)

#### Response (200 OK)
```json
[
  {
    "id": "c0000000-0000-0000-0000-000000000001",
    "allergyId": "a0000000-0000-0000-0000-000000000001",
    "patientId": "70000000-0000-0000-0000-000000000001",
    "action": "UPDATE",
    "beforeData": {
      "allergenName": "Penicillin",
      "severity": "SEVERE",
      "reaction": "Nổi mề đay toàn thân, khó thở nhẹ",
      "notes": "Xảy ra năm 2022 sau khi tiêm thuốc"
    },
    "afterData": {
      "allergenName": "Penicillin",
      "severity": "ANAPHYLAXIS",
      "reaction": "Khó thở dữ dội, tụt huyết áp",
      "notes": "Đã xác định phản vệ mức độ nguy kịch"
    },
    "changeReason": "Bổ sung biểu hiện nặng theo lời kể gia đình",
    "changedBy": "22222222-2222-2222-2222-222222222222",
    "changedAt": "2026-09-07T08:00:00Z"
  }
]
```

---

## 3. Error Contract

| HTTP Status | Error Code | Mô tả |
|---|---|---|
| `400 Bad Request` | `VALIDATION_FAILED` | Thiếu trường bắt buộc (`allergenName`, `severity`), hoặc giá trị sai enum. |
| `403 Forbidden` | `FORBIDDEN` | Người dùng không có quyền `PATIENT_ALLERGY_WRITE` (ví dụ: Lễ tân - TC-03). |
| `404 Not Found` | `PATIENT_NOT_FOUND` | Bệnh nhân không tồn tại. |
| `404 Not Found` | `PATIENT_ALLERGY_NOT_FOUND` | Mục dị ứng không tồn tại hoặc không thuộc bệnh nhân này. |
| `409 Conflict` | `PATIENT_ALLERGY_ALREADY_EXISTS` | Bệnh nhân đã ghi nhận dị ứng hoạt chất này từ trước (TC-02). |
