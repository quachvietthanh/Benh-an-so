# API Contract: Quản lý tiền sử bệnh mạn tính & tiền sử gia đình (Patient Chronic Disease & Family History)

> **Module:** NCL-02 - Quản lý hồ sơ bệnh nhân
>
> **User Story:** NCL-02-CN-009 - Ghi tiền sử bệnh mạn tính và tiền sử gia đình
>
> **Business Rules:** QTN-01 (*Phân quyền truy cập theo vai trò*), QTN-11 (*Chỉ bác sĩ ghi chuyên môn*)
>
> **Base URL:** `/patients/{patientId}/chronic-diseases` và `/patients/{patientId}/family-history`
>
> **Auth:** Bearer Token (JWT)
>
> **Date:** 2026-09-15

---

## 1. Tổng quan nghiệp vụ & Phân quyền

1. **Ghi tiền sử bệnh mạn tính**: Bác sĩ ghi nhận mã bệnh (mã chẩn đoán ICD-10 từ `diagnosis_catalog`), năm phát hiện và ghi chú.
   - Quyền yêu cầu: `PATIENT_CHRONIC_DISEASE_WRITE` (chỉ cấp cho `DOCTOR` và `ADMIN`).
2. **Ghi tiền sử gia đình**: Bác sĩ ghi nhận quan hệ gia đình (`relationship`), mã bệnh và ghi chú.
   - Quyền yêu cầu: `PATIENT_FAMILY_HISTORY_WRITE` (chỉ cấp cho `DOCTOR` và `ADMIN`).
3. **Kiểm tra mã bệnh hợp lệ**: Chỉ cho phép sử dụng mã bệnh đang **hoạt động** (`active = TRUE`).
   - Mã bệnh không tồn tại → HTTP `404 Not Found` (`DIAGNOSIS_CATALOG_NOT_FOUND`).
   - Mã bệnh đã ngừng hoạt động → HTTP `400 Bad Request` (`VALIDATION_FAILED`).
4. **Chống trùng lặp (TC-02)**: Mỗi bệnh nhân chỉ có duy nhất một bản ghi bệnh mạn tính **đang hoạt động** cho một mã bệnh.
   - Nếu trùng lặp, hệ thống từ chối lưu và trả về HTTP `409 Conflict` (`PATIENT_CHRONIC_DISEASE_ALREADY_EXISTS`).
   - Ràng buộc được bảo vệ ở tầng cơ sở dữ liệu (unique index trên cột sinh `active_diagnosis_catalog_id`).
5. **Xóa mềm**: Thao tác xóa chỉ chuyển trạng thái `active = false`; bản ghi vẫn được giữ lại và cho phép ghi nhận lại cùng mã bệnh sau này.
6. **Lưu vết kiểm toán**: Mỗi thao tác tạo/xóa đều ghi `audit_logs` với `resourceType = PATIENT_CHRONIC_DISEASE` hoặc `PATIENT_FAMILY_HISTORY`. Nếu có `visitId`, mã lượt khám được đưa vào chi tiết (`detail`) để giữ bối cảnh lâm sàng.

> **Lưu ý:** Tiền sử bệnh mạn tính & tiền sử gia đình là dữ liệu ở cấp **bệnh nhân**. `visitId` chỉ là bối cảnh lâm sàng tùy chọn (được kiểm tra hợp lệ nếu cung cấp), không được lưu thành cột riêng trong bảng.

---

## 2. Danh sách Endpoints

### 2.1. Thêm tiền sử bệnh mạn tính
- **Method:** `POST`
- **Path:** `/patients/{patientId}/chronic-diseases`
- **Permission:** `PATIENT_CHRONIC_DISEASE_WRITE` (Bác sĩ, Admin)

#### Request Body
```json
{
  "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000006",
  "yearDetected": 2018,
  "notes": "Đang dùng thuốc duy trì",
  "visitId": "d0000000-0000-0000-0000-000000000001"
}
```

*Ghi chú trường:*
- `diagnosisCatalogId`: Bắt buộc (UUID mã bệnh/chẩn đoán).
- `yearDetected`: Tùy chọn, số nguyên từ `1900` đến năm hiện tại.
- `notes`: Tùy chọn.
- `visitId`: Tùy chọn (UUID lượt khám đang thực hiện nếu có).

#### Response (201 Created)
```json
{
  "id": "b1000000-0000-0000-0000-000000000001",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000006",
  "diagnosisCode": "E11.9",
  "diagnosisName": "Đái tháo đường type 2",
  "yearDetected": 2018,
  "notes": "Đang dùng thuốc duy trì",
  "active": true,
  "createdBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
  "createdAt": "2026-09-15T07:30:00Z",
  "updatedBy": null,
  "updatedAt": "2026-09-15T07:30:00Z"
}
```

---

### 2.2. Lấy danh sách tiền sử bệnh mạn tính
- **Method:** `GET`
- **Path:** `/patients/{patientId}/chronic-diseases`
- **Permission:** `PATIENT_CHRONIC_DISEASE_READ` (Admin, Bác sĩ, Dược sĩ, Quản lý)

#### Response (200 OK)
Trả về mảng các bản ghi **đang hoạt động**, mỗi phần tử có cấu trúc như trên (gồm `diagnosisCode` và `diagnosisName` để client hiển thị trực tiếp, không cần truy vấn thêm).

---

### 2.3. Xóa (vô hiệu hóa) tiền sử bệnh mạn tính
- **Method:** `DELETE`
- **Path:** `/patients/{patientId}/chronic-diseases/{chronicDiseaseId}`
- **Permission:** `PATIENT_CHRONIC_DISEASE_WRITE` (Bác sĩ, Admin)
- **Query:** `reason` (tùy chọn lý do xóa).

#### Response (204 No Content)

---

### 2.4. Thêm tiền sử gia đình
- **Method:** `POST`
- **Path:** `/patients/{patientId}/family-history`
- **Permission:** `PATIENT_FAMILY_HISTORY_WRITE` (Bác sĩ, Admin)

#### Request Body
```json
{
  "relationship": "Bố",
  "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000006",
  "notes": "Đã mổ tim năm 2020",
  "visitId": "d0000000-0000-0000-0000-000000000001"
}
```

*Ghi chú trường:*
- `relationship`: Bắt buộc, chuỗi từ 1-100 ký tự.
- `diagnosisCatalogId`: Bắt buộc (UUID mã bệnh/chẩn đoán).
- `notes`: Tùy chọn.
- `visitId`: Tùy chọn.

#### Response (201 Created)
```json
{
  "id": "b2000000-0000-0000-0000-000000000001",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "relationship": "Bố",
  "diagnosisCatalogId": "a1000000-0000-0000-0000-000000000006",
  "diagnosisCode": "E11.9",
  "diagnosisName": "Đái tháo đường type 2",
  "notes": "Đã mổ tim năm 2020",
  "active": true,
  "createdBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
  "createdAt": "2026-09-15T07:30:00Z",
  "updatedBy": null,
  "updatedAt": "2026-09-15T07:30:00Z"
}
```

---

### 2.5. Lấy danh sách tiền sử gia đình
- **Method:** `GET`
- **Path:** `/patients/{patientId}/family-history`
- **Permission:** `PATIENT_FAMILY_HISTORY_READ` (Admin, Bác sĩ, Dược sĩ, Quản lý)

#### Response (200 OK)
Trả về mảng các bản ghi **đang hoạt động**, mỗi phần tử có cấu trúc như trên.

---

### 2.6. Xóa (vô hiệu hóa) tiền sử gia đình
- **Method:** `DELETE`
- **Path:** `/patients/{patientId}/family-history/{familyHistoryId}`
- **Permission:** `PATIENT_FAMILY_HISTORY_WRITE` (Bác sĩ, Admin)
- **Query:** `reason` (tùy chọn lý do xóa).

#### Response (204 No Content)

---

## 3. Error Contract

| HTTP Status | Error Code | Mô tả |
|---|---|---|
| `400 Bad Request` | `VALIDATION_FAILED` | Thiếu trường bắt buộc, giá trị sai (`yearDetected`, `relationship`), hoặc mã bệnh đã ngừng hoạt động. |
| `400 Bad Request` | `VALIDATION_FAILED` | `visitId` không thuộc bệnh nhân hoặc lượt khám đã kết thúc. |
| `403 Forbidden` | `FORBIDDEN` | Người dùng không có quyền ghi/đọc tương ứng. |
| `404 Not Found` | `PATIENT_NOT_FOUND` | Bệnh nhân không tồn tại. |
| `404 Not Found` | `DIAGNOSIS_CATALOG_NOT_FOUND` | Mã bệnh/chẩn đoán không tồn tại. |
| `404 Not Found` | `PATIENT_CHRONIC_DISEASE_NOT_FOUND` | Bản ghi bệnh mạn tính không tồn tại hoặc không thuộc bệnh nhân. |
| `404 Not Found` | `PATIENT_FAMILY_HISTORY_NOT_FOUND` | Bản ghi tiền sử gia đình không tồn tại hoặc không thuộc bệnh nhân. |
| `409 Conflict` | `PATIENT_CHRONIC_DISEASE_ALREADY_EXISTS` | Bệnh nhân đã ghi nhận bệnh mạn tính này từ trước (TC-02). |
