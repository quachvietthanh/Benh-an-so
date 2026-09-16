# Overdue Medical Record Signing & Reminder API Contract

> Module: **NCL-11 - Hồ sơ bệnh án điện tử và Quản lý phòng khám**
>
> User Story: **NCL-11-CN-006 - Theo dõi và nhắc ký bệnh án quá hạn**
>
> Base URL: `http://localhost:8080/medical-records` & `http://localhost:8080/clinic-configuration`
>
> Auth: Bearer Token (JWT)
>
> Date: **2026-09-16**
>
> Status: **Completed Contract & Implementation**

---

## 1. Scope & Business Requirements

Tài liệu này đặc tả API contract cho tính năng theo dõi và nhắc ký hồ sơ bệnh án quá hạn (`NCL-11-CN-006`).

### 1.1 Mục tiêu nghiệp vụ
- Giúp người quản lý phòng khám (Admin/Manager) và bác sĩ theo dõi được các hồ sơ bệnh án chưa được ký số (`DRAFT`, `OPEN`) sau khi lượt khám đã hoàn tất (`COMPLETED` với `completed_at`).
- Thời hạn ký được cấu hình linh hoạt theo từng phòng khám (`signingDeadlineHours` trong `ClinicConfiguration`, mặc định 24 giờ, tối thiểu 1 giờ - theo QTN-29).
- Cung cấp tính năng gửi nhắc nhở ký bệnh án đến bác sĩ phụ trách, lưu lại lịch sử các lần nhắc (thời gian, người nhắc, kênh, ghi chú) và ghi vết `AuditLog`.
- Bệnh án sau khi được ký số (`SIGNED` hoặc `LOCKED`) sẽ tự động không còn xuất hiện trong danh sách quá hạn (AC-03, QTN-17).
- Kiểm soát phân quyền nghiêm ngặt: Lễ tân (Receptionist) hoặc người không có quyền sẽ bị từ chối truy cập (HTTP 403) và hệ thống tự động ghi nhật ký vi phạm bảo mật (`ACCESS_DENIED`).

---

## 2. Roles & Permissions

| Role | Quyền hạn | Ghi chú |
| :--- | :--- | :--- |
| **ADMIN** | `MEDICAL_RECORD_OVERDUE_READ`, `MEDICAL_RECORD_REMIND_SIGN`, `CLINIC_CONFIG_UPDATE` | Xem toàn bộ bệnh án quá hạn của phòng khám, gửi nhắc nhở, cấu hình thời hạn ký. |
| **MANAGER** | `MEDICAL_RECORD_OVERDUE_READ`, `MEDICAL_RECORD_REMIND_SIGN`, `CLINIC_CONFIG_UPDATE` | Xem danh sách vận hành quá hạn (không xem chi tiết khám nếu không có `MEDICAL_RECORD_READ`), gửi nhắc nhở bác sĩ. |
| **DOCTOR** | `MEDICAL_RECORD_OVERDUE_READ` | Chỉ xem danh sách các bệnh án quá hạn do chính mình phụ trách (`doctorId = currentUserId`). Không có quyền gửi nhắc nhở cho người khác. |
| **RECEPTIONIST** | *Không có quyền* | Bị cấm truy cập (HTTP 403). Ghi vết `ACCESS_DENIED` trong `audit_logs`. |

---

## 3. Endpoints Specification

### 3.1 Cấu hình thời hạn ký bệnh án

- **Endpoint**: `PUT /system/clinic`
- **Permission**: `CLINIC_CONFIGURATION_UPDATE`
- **Request Body**:
```json
{
  "clinicName": "Phòng khám Đa khoa Hoàn Mỹ",
  "address": "123 Hoàng Văn Thụ, Tân Bình, TP.HCM",
  "phone": "02838445566",
  "openingTime": "08:00:00",
  "closingTime": "17:00:00",
  "retentionYears": 10,
  "signingDeadlineHours": 24
}
```
- **Validation**:
  - `clinicName`: Bắt buộc, tối đa 150 ký tự.
  - `openingTime`, `closingTime`: Bắt buộc.
  - `retentionYears`: Tối thiểu 10 năm.
  - `signingDeadlineHours`: Kiểu số nguyên, giá trị tối thiểu là `1` (QTN-29).

- **Response (200 OK)**:
```json
{
  "id": 1,
  "clinicName": "Phòng khám Đa khoa Hoàn Mỹ",
  "address": "123 Hoàng Văn Thụ, Tân Bình, TP.HCM",
  "phone": "02838445566",
  "openingTime": "08:00:00",
  "closingTime": "17:00:00",
  "retentionYears": 10,
  "signingDeadlineHours": 24,
  "createdAt": "2026-09-01T00:00:00Z",
  "updatedAt": "2026-09-16T06:00:00Z"
}
```

---

### 3.2 Lấy danh sách bệnh án quá hạn ký

- **Endpoint**: `GET /medical-records/overdue-signing`
- **Permission**: `MEDICAL_RECORD_OVERDUE_READ`
- **Query Parameters**:
  - `doctorId` (optional, UUID): Lọc theo bác sĩ phụ trách (đối với Admin/Manager). Nếu user là Doctor, hệ thống tự động khóa theo id của bác sĩ đó.
  - `page` (optional, int, default 0): Số trang.
  - `size` (optional, int, default 20): Kích thước trang.

- **Response (200 OK)**:
```json
{
  "content": [
    {
      "medicalRecordId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "status": "OPEN",
      "visitId": "b2c3d4e5-f6a7-8b9c-0d1e-2f3a4b5c6d7e",
      "visitCode": "KB-20260914-0012",
      "visitCompletedAt": "2026-09-14T10:00:00Z",
      "patientId": "c3d4e5f6-a7b8-9c0d-1e2f-3a4b5c6d7e8f",
      "patientCode": "BN-2026-0089",
      "patientFullName": "Nguyễn Văn A",
      "doctorId": "d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a",
      "doctorFullName": "BS. Trần Thị B",
      "doctorEmail": "bs.tranb@clinic.vn",
      "doctorPhone": "0912345678",
      "signingDeadlineHours": 24,
      "deadlineAt": "2026-09-15T10:00:00Z",
      "overdueHours": 20,
      "reminderCount": 1,
      "lastRemindedAt": "2026-09-15T14:30:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### 3.3 Gửi nhắc nhở ký bệnh án cho bác sĩ

- **Endpoint**: `POST /medical-records/{medicalRecordId}/signing-reminders`
- **Permission**: `MEDICAL_RECORD_REMIND_SIGN`
- **Path Parameters**:
  - `medicalRecordId` (UUID, required): ID của hồ sơ bệnh án cần nhắc ký.
- **Request Body** (optional):
```json
{
  "channel": "SYSTEM",
  "notes": "Vui lòng ký bệnh án trước 17h hôm nay để hoàn thiện hồ sơ BHYT."
}
```

- **Response (201 Created)**:
```json
{
  "id": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
  "medicalRecordId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "doctorId": "d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a",
  "doctorFullName": "BS. Trần Thị B",
  "remindedBy": "5f6a7b8c-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
  "remindedByName": "Quản lý phòng khám",
  "remindedAt": "2026-09-16T06:40:00Z",
  "overdueHours": 20,
  "channel": "SYSTEM",
  "notes": "Vui lòng ký bệnh án trước 17h hôm nay để hoàn thiện hồ sơ BHYT.",
  "status": "SENT"
}
```

- **Side Effects**:
  - Gửi thông báo đến bác sĩ qua `SigningReminderNotificationPort`.
  - Ghi bản ghi nhắc ký vào bảng `medical_record_signing_reminders`.
  - Ghi `AuditLog` với `action: ActionType.SEND`, `resourceType: ResourceType.MEDICAL_RECORD`, `resourceId: medicalRecordId`.

---

### 3.4 Xem lịch sử các lần nhắc nhở của bệnh án

- **Endpoint**: `GET /medical-records/{medicalRecordId}/signing-reminders`
- **Permission**: `MEDICAL_RECORD_OVERDUE_READ`
- **Path Parameters**:
  - `medicalRecordId` (UUID, required): ID hồ sơ bệnh án.

- **Response (200 OK)**:
```json
[
  {
    "id": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
    "medicalRecordId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "doctorId": "d4e5f6a7-b8c9-0d1e-2f3a-4b5c6d7e8f9a",
    "doctorFullName": "BS. Trần Thị B",
    "remindedBy": "5f6a7b8c-9d0e-1f2a-3b4c-5d6e7f8a9b0c",
    "remindedByName": "Quản lý phòng khám",
    "remindedAt": "2026-09-16T06:40:00Z",
    "overdueHours": 20,
    "channel": "SYSTEM",
    "notes": "Vui lòng ký bệnh án trước 17h hôm nay để hoàn thiện hồ sơ BHYT.",
    "status": "SENT"
  }
]
```

---

## 4. Error Responses

| Status Code | Error Code | Description |
| :--- | :--- | :--- |
| `400 Bad Request` | `MEDICAL_RECORD_NOT_OVERDUE` | Bệnh án chưa quá thời hạn ký hoặc đã được ký/khóa trước đó. |
| `400 Bad Request` | `VALIDATION_ERROR` | Request body không hợp lệ (ví dụ: `signingDeadlineHours < 1`, `notes > 500 chars`). |
| `401 Unauthorized`| `UNAUTHORIZED` | Token không hợp lệ hoặc đã hết hạn. |
| `403 Forbidden`   | `ACCESS_DENIED` | Người dùng không có quyền truy cập (ví dụ Lễ tân gọi các endpoint trên). Ghi vết bảo mật vào `audit_logs`. |
| `404 Not Found`   | `MEDICAL_RECORD_NOT_FOUND` | Không tìm thấy bệnh án tương ứng với ID được truyền vào. |
