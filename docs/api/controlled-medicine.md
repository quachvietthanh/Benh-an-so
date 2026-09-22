# Thuốc kiểm soát đặc biệt — Backend contract (NCL-06-CN-014)

## 1. Tổng quan

Nhóm thuốc kiểm soát đặc biệt (QTN-39) yêu cầu **xác nhận bổ sung khi kê và khi
cấp phát**, đồng thời ghi vào **sổ theo dõi riêng không xóa được** (append-only).

Vai trò liên quan: **Dược sĩ** (quản trị danh mục, cấp phát), **Bác sĩ** (kê đơn),
**Quản lý phòng khám** (mở sổ theo dõi).

## 2. Data model

### `medicines.controlled`
- `BOOLEAN NOT NULL DEFAULT FALSE` — đánh dấu thuốc thuộc nhóm kiểm soát đặc biệt.
- Được quản lý qua chính các API thuốc hiện có (không có endpoint riêng).

### `controlled_medicine_registers` (sổ theo dõi riêng)
| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | BINARY(16) | PK |
| `prescription_id` | BINARY(16) | Đơn thuốc gốc (FK `prescriptions`) |
| `prescription_item_id` | BINARY(16) | Dòng thuốc gốc (FK `prescription_items`) |
| `medicine_id` | BINARY(16) | Thuốc (FK `medicines`) |
| `medicine_name` | VARCHAR(150) | Snapshot tên thuốc |
| `patient_id` | BINARY(16) | Bệnh nhân (FK `patients`) |
| `prescribed_by` | BINARY(16) | Bác sĩ kê (FK `users`) |
| `dispensed_by` | BINARY(16) | Dược sĩ cấp phát (FK `users`) |
| `quantity` | INT | Số lượng cấp phát (> 0) |
| `dispensed_at` | TIMESTAMP | Thời điểm cấp phát |
| `created_at` | TIMESTAMP | Thời điểm tạo bản ghi |

Bảng chỉ được **append** (`INSERT`). Không có endpoint/service `DELETE` hay `UPDATE`.

## 3. Quy trình xác nhận (confirmation)

- **Kê đơn**: khi `POST /prescriptions` chứa ≥ 1 thuốc `controlled = true`, client
  phải gửi `"controlledMedicineConfirmed": true`. Thiếu → `409 CONTROLLED_MEDICINE_CONFIRMATION_REQUIRED`.
- **Sửa đơn**: khi `PATCH /prescriptions/{id}` đưa thuốc kiểm soát vào, yêu cầu tương tự.
- **Cấp phát**: khi `POST /prescriptions/{id}/dispense` hoặc
  `POST /prescriptions/{id}/partial-dispense` cấp phát thuốc kiểm soát, client phải
  gửi `"controlledMedicineConfirmed": true`. Thiếu → `409 CONTROLLED_MEDICINE_CONFIRMATION_REQUIRED`.
- Việc kiểm tra được thực thi ở **tầng service** (không chỉ controller), nên frontend
  không thể bỏ qua bằng cách gọi trực tiếp API.

## 4. API

### 4.1 Đánh dấu thuốc kiểm soát
Thuộc tính `controlled` được thêm vào body create/update thuốc hiện có:

- `POST /medicines` — `PHARMACY_CREATE`
- `PUT /medicines/{id}` — `PHARMACY_UPDATE`

Request bổ sung trường `"controlled": true|false`. Response trả thêm `controlled`.

### 4.2 Kê đơn — `POST /prescriptions`
- Permission: `PRESCRIPTION_CREATE`
- Body thêm trường tùy chọn `"controlledMedicineConfirmed": true`.

```json
{
  "medicalRecordId": "…",
  "note": "Uống sau ăn",
  "controlledMedicineConfirmed": true,
  "items": [ { "medicineId": "…", "dosage": "1 viên", "frequency": 2,
    "route": "ORAL", "durationDays": 5, "quantity": 10 } ]
}
```

### 4.3 Sửa đơn — `PATCH /prescriptions/{id}`
- Permission: `PRESCRIPTION_UPDATE`
- Body thêm trường tùy chọn `"controlledMedicineConfirmed": true`.

### 4.4 Cấp phát — `POST /prescriptions/{id}/dispense`
- Permission: `PRESCRIPTION_UPDATE_STATUS`
- Body tùy chọn (trước đây không có body):

```json
{ "controlledMedicineConfirmed": true }
```

Khi cấp phát thành công một thuốc kiểm soát, hệ thống ghi 1 bản ghi vào sổ trong
cùng transaction (đảm bảo không thể trừ kho mà thiếu bản ghi sổ, và ngược lại).

### 4.5 Cấp phát một phần — `POST /prescriptions/{id}/partial-dispense`
- Permission: `PRESCRIPTION_UPDATE_STATUS`
- Body bổ sung `"controlledMedicineConfirmed": true` (cùng nguyên tắc).

### 4.6 Sổ theo dõi — `GET /controlled-medicines/register`
- Permission: `CONTROLLED_MEDICINE_REGISTER_READ` (seed cho `ADMIN`, `PHARMACIST`, `MANAGER`).
- Query params tùy chọn: `patientId`, `medicineId`, `from`, `to`, `page` (default 0), `size` (default 20).
- `from` inclusive, `to` exclusive (theo `dispensed_at`).

```json
{
  "content": [
    {
      "id": "…",
      "prescriptionId": "…",
      "prescriptionItemId": "…",
      "medicineId": "…",
      "medicineName": "Morphine 10mg",
      "patientId": "…",
      "patientCode": "BN-0001",
      "patientName": "Nguyễn Văn A",
      "prescribedBy": "…",
      "doctorName": "BS. B",
      "dispensedBy": "…",
      "pharmacistName": "DS. C",
      "quantity": 5,
      "dispensedAt": "2026-09-21T10:00:00Z"
    }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

## 5. Phân quyền & bảo mật

| Thao tác | Quyền / vai trò |
|---|---|
| Đánh dấu thuốc kiểm soát | `PHARMACY_CREATE` / `PHARMACY_UPDATE` (service: `PHARMACIST`) |
| Kê/sửa đơn có thuốc kiểm soát | `PRESCRIPTION_CREATE` / `PRESCRIPTION_UPDATE` (service: `DOCTOR`) |
| Cấp phát có thuốc kiểm soát | `PRESCRIPTION_UPDATE_STATUS` (service: `PHARMACIST`/`ADMIN`) |
| Xem sổ theo dõi | `CONTROLLED_MEDICINE_REGISTER_READ` (`ADMIN`, `PHARMACIST`, `MANAGER`) |
| Xóa/sửa bản ghi sổ | **Không có endpoint/service** (append-only) |

- `@RequirePermission` từ chối ghi `ACCESS_DENIED` audit tự động.
- Xác nhận thuốc kiểm soát không thể bị bỏ qua qua API trực tiếp (kiểm tra ở service).
- IDOR: kê đơn giữ nguyên kiểm tra quyền bác sĩ phụ trách; cấp phát giữ nguyên `findByIdForUpdate`.

## 6. Immutability (TC-04)

- Không có `DELETE`/`UPDATE` endpoint cho sổ theo dõi.
- Port `ControlledMedicineRegisterRepository` chỉ có `save`, `saveAll`, `search`.
- Kiểm thử phản chiếu khẳng định port không có phương thức `delete*`/`update*`.
- Bản ghi đã tạo giữ nguyên khi thử xóa (không có đường dẫn nào để xóa).

## 7. Lỗi

| HTTP | Code | Mô tả |
|---|---|---|
| 409 Conflict | `CONTROLLED_MEDICINE_CONFIRMATION_REQUIRED` | Kê/cấp phát thuốc kiểm soát chưa xác nhận |
| 403 Forbidden | `ACCESS_DENIED` | Thiếu quyền tương ứng |
| 401 Unauthorized | `AUTHENTICATION_FAILED` | Chưa đăng nhập |
