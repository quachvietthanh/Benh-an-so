# NCL-03-CN-006 — Quản lý lịch làm việc và thời gian nghỉ của bác sĩ

## Phạm vi và Mục tiêu

Tài liệu này đặc tả API và quy tắc nghiệp vụ quản lý lịch làm việc định kỳ (weekly schedule template) và các khoảng thời gian nghỉ/phép đột xuất (doctor time-off) của bác sĩ theo User Story **NCL-03-CN-006**.

### Quy tắc nghiệp vụ (Business Rules)
1. **QTN-30**: Bệnh nhân hoặc nhân viên tiếp đón chỉ được đặt lịch hẹn trong khung giờ làm việc của bác sĩ và ngoài các khoảng thời gian nghỉ đã đăng ký.
   - Nếu vi phạm, hệ thống từ chối với thông báo lỗi: `"Bác sĩ không làm việc trong khung giờ này."`.
2. **QTN-04**: Không cho phép đặt trùng lịch hẹn cho cùng một bác sĩ trong cùng khung giờ.
3. **Tiền điều kiện (Precondition)**: Khung giờ làm việc định kỳ của bác sĩ phải nằm trọn trong thời gian hoạt động của phòng khám (`ClinicConfiguration.openingTime` - `ClinicConfiguration.closingTime`).

---

## API Endpoints

Base path: `/api/v1`

### 1. Cấu hình lịch làm việc định kỳ theo tuần (Weekly Schedule)

#### `PUT /doctor-schedules/weekly`
Cập nhật hoặc ghi đè toàn bộ lịch làm việc định kỳ theo tuần của bác sĩ.
- **Quyền yêu cầu**: `DOCTOR_SCHEDULE_UPDATE` (`ADMIN`, `MANAGER`).

##### Request Body
```json
{
  "doctorId": "11111111-1111-1111-1111-111111111111",
  "items": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00:00",
      "endTime": "12:00:00",
      "active": true
    },
    {
      "dayOfWeek": "MONDAY",
      "startTime": "13:30:00",
      "endTime": "17:00:00",
      "active": true
    },
    {
      "dayOfWeek": "WEDNESDAY",
      "startTime": "08:00:00",
      "endTime": "17:00:00",
      "active": true
    }
  ]
}
```

##### Response `200 OK`
```json
{
  "doctorId": "11111111-1111-1111-1111-111111111111",
  "items": [
    {
      "id": "a1b2c3d4-...",
      "doctorId": "11111111-1111-1111-1111-111111111111",
      "dayOfWeek": "MONDAY",
      "startTime": "08:00:00",
      "endTime": "12:00:00",
      "active": true
    }
  ]
}
```

---

#### `GET /doctor-schedules/weekly`
Lấy lịch làm việc định kỳ trong tuần của một bác sĩ.
- **Quyền yêu cầu**: `DOCTOR_SCHEDULE_READ` (`ADMIN`, `MANAGER`, `RECEPTIONIST`, `DOCTOR`).
- **Query params**: `doctorId` (UUID, bắt buộc).

##### Response `200 OK`
Trả về danh sách các ca làm việc trong tuần của bác sĩ.

---

### 2. Quản lý thời gian nghỉ / nghỉ phép của bác sĩ (Doctor Time-off)

#### `POST /doctor-time-offs`
Đăng ký thời gian nghỉ/phép của bác sĩ. Nếu khoảng thời gian nghỉ này trùng với các lịch hẹn đã được đặt trước đó, hệ thống sẽ tự động phát hiện và trả về danh sách các lịch hẹn bị ảnh hưởng (`affectedAppointments`) để nhân viên tiếp đón có thể liên hệ bệnh nhân đổi lịch hoặc hủy hẹn.
- **Quyền yêu cầu**: `DOCTOR_TIME_OFF_CREATE` (`ADMIN`, `MANAGER`).

##### Request Body
```json
{
  "doctorId": "11111111-1111-1111-1111-111111111111",
  "startTime": "2026-09-15T08:00:00Z",
  "endTime": "2026-09-15T12:00:00Z",
  "reason": "Bác sĩ tham gia hội nghị chuyên môn"
}
```

##### Response `201 Created`
```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "doctorId": "11111111-1111-1111-1111-111111111111",
  "startTime": "2026-09-15T08:00:00Z",
  "endTime": "2026-09-15T12:00:00Z",
  "reason": "Bác sĩ tham gia hội nghị chuyên môn",
  "status": "ACTIVE",
  "createdBy": "44444444-4444-4444-4444-444444444444",
  "createdAt": "2026-09-08T08:00:00Z",
  "updatedAt": "2026-09-08T08:00:00Z",
  "affectedAppointments": [
    {
      "appointmentId": "33333333-3333-3333-3333-333333333333",
      "appointmentCode": "AP123456",
      "patientId": "55555555-5555-5555-5555-555555555555",
      "patientName": "Nguyễn Văn A",
      "patientPhone": "0901234567",
      "startTime": "2026-09-15T08:30:00Z",
      "endTime": "2026-09-15T09:00:00Z",
      "status": "SCHEDULED",
      "reason": "Khám tổng quát định kỳ"
    }
  ]
}
```

---

#### `GET /doctor-time-offs`
Tra cứu danh sách thời gian nghỉ của bác sĩ theo bộ lọc.
- **Quyền yêu cầu**: `DOCTOR_TIME_OFF_READ` (`ADMIN`, `MANAGER`, `RECEPTIONIST`, `DOCTOR`).
- **Query params** (tùy chọn):
  - `doctorId`: UUID
  - `status`: `ACTIVE` | `CANCELLED`
  - `fromTime`: Instant (ISO-8601)
  - `toTime`: Instant (ISO-8601)

##### Response `200 OK`
Danh sách các khoảng nghỉ thỏa mãn điều kiện lọc.

---

#### `DELETE /doctor-time-offs/{id}`
Hủy một khoảng nghỉ của bác sĩ (chuyển trạng thái từ `ACTIVE` sang `CANCELLED`).
- **Quyền yêu cầu**: `DOCTOR_TIME_OFF_DELETE` (`ADMIN`, `MANAGER`).

##### Response `200 OK`
Thông tin chi tiết khoảng nghỉ với `status: "CANCELLED"`.

---

#### `GET /doctor-time-offs/{id}/affected-appointments`
Xem danh sách các lịch hẹn bị ảnh hưởng bởi một khoảng nghỉ cụ thể.
- **Quyền yêu cầu**: `DOCTOR_TIME_OFF_READ` (`ADMIN`, `MANAGER`, `RECEPTIONIST`, `DOCTOR`).

##### Response `200 OK`
Danh sách `AffectedAppointmentResponse`.

---

## Ma trận Phân quyền & Mã lỗi

| Điều kiện | Kết quả HTTP | Lỗi nghiệp vụ / Ghi chú |
| --- | --- | --- |
| Không có JWT / Không xác thực | `401 Unauthorized` | Cần đăng nhập |
| Người dùng không có quyền (VD: `PHARMACIST`) | `403 Forbidden` | Tự động ghi nhận log `ACCESS_DENIED` vào `audit_logs` |
| Bác sĩ không tồn tại | `404 Not Found` | `DOCTOR_NOT_FOUND` |
| Bác sĩ không hoạt động | `400 Bad Request` | `DOCTOR_INACTIVE` |
| Người dùng không phải vai trò bác sĩ | `400 Bad Request` | `INVALID_DOCTOR_ROLE` |
| Lịch làm việc vượt quá giờ mở cửa phòng khám | `400 Bad Request` | `VALIDATION_ERROR` (Giờ làm việc phải nằm trong giờ mở cửa phòng khám) |
| Đặt lịch hoặc đổi lịch trong giờ nghỉ của bác sĩ | `400 Bad Request` | `DOCTOR_NOT_WORKING`: `"Bác sĩ không làm việc trong khung giờ này."` |
| Khoảng nghỉ không tồn tại | `404 Not Found` | `DOCTOR_TIME_OFF_NOT_FOUND` |

---

## Nhật ký kiểm toán (Audit Trail)

Mọi thao tác thay đổi lịch làm việc và đăng ký thời gian nghỉ đều được lưu vết tự động vào bảng `audit_logs`:
- `ActionType.UPDATE` với `ResourceType.DOCTOR_SCHEDULE` khi cập nhật lịch tuần.
- `ActionType.CREATE` với `ResourceType.DOCTOR_TIME_OFF` khi đăng ký thời gian nghỉ.
- `ActionType.DELETE` với `ResourceType.DOCTOR_TIME_OFF` khi hủy thời gian nghỉ.
- `ActionType.ACCESS_DENIED` với `ResourceType.PERMISSION` khi bị từ chối quyền truy cập (TC-04).
