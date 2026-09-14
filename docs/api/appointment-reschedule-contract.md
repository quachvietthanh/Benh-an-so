# NCL-03-CN-007 — Đổi lịch hẹn tại quầy (Appointment Reschedule Contract)

## 1. Scope & Business Rules

User Story: **NCL-03-CN-007 — Đổi lịch hẹn tại quầy** (Epic NCL-03 - Tiếp đón và quản lý bệnh nhân).

### Phân quyền & Vai trò
- **Vai trò được phép:** Lễ tân (`RECEPTIONIST`) hoặc Quản trị viên (`ADMIN`).
- **Quyền yêu cầu (Permission):** `APPOINTMENT_UPDATE`.
- Các vai trò khác (`DOCTOR`, `PHARMACIST`) hoặc người dùng chưa xác thực sẽ bị từ chối với `403 Forbidden` / `401 Unauthorized`.

### Quy tắc nghiệp vụ (Business Rules)
1. **Trạng thái hợp lệ (`NCL-03-CN-007-TC-02` & `QTN-08`):**
   - Chỉ cho phép đổi lịch hẹn đang ở trạng thái `SCHEDULED` hoặc `CONFIRMED`.
   - Các trạng thái khác (`NO_SHOW`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) sẽ bị từ chối (`400 Bad Request` hoặc `AppointmentInvalidStatusTransitionException`).
2. **Khống chế thời gian đổi lịch (`NCL-03-CN-007-TC-02`):**
   - Không cho phép dời lịch đối với cuộc hẹn đã quá giờ khám (`startTime <= now`). Hệ thống trả về `AppointmentPastCutoffException` (`400 Bad Request`).
   - Thời gian khám mới bắt buộc phải ở thời điểm tương lai (`newStartTime > now`).
3. **Đổi cùng bác sĩ hoặc khác bác sĩ (`NCL-03-CN-007-TC-01` & `TC-01b`):**
   - Có thể giữ nguyên bác sĩ hoặc chỉ định `newDoctorId` sang bác sĩ khác đang hoạt động (`ACTIVE`).
4. **Kiểm tra ca làm việc và lịch nghỉ (`QTN-30`):**
   - Bác sĩ phải có lịch làm việc bao trọn khung giờ khám mới.
   - Bác sĩ không được trùng thời gian đăng ký nghỉ phép (`doctor_time_offs`).
5. **Chống trùng lịch (`QTN-04`):**
   - Bác sĩ không có cuộc hẹn khám nào khác trùng hoặc giao nhau với khung giờ khám mới (ngoại trừ chính lịch hẹn đang được thao tác dời).
6. **Lịch sử dời lịch (`NCL-03-CN-007-TC-04`):**
   - Mỗi lần dời lịch thành công, hệ thống tự động ghi nhận vào bảng `appointment_reschedule_logs` gồm: giờ cũ, giờ mới, bác sĩ cũ/mới, người dời, lý do và thời điểm dời.
   - Khi xem chi tiết lịch hẹn qua `GET /appointments/{id}`, danh sách `rescheduleHistories` được trả về đầy đủ.
7. **Kiểm toán hệ thống (Audit Log):**
   - Ghi lại hành động `APPOINTMENT_RESCHEDULE` vào bảng `audit_logs`.

---

## 2. API Specification

Base path: `/appointments` (hoặc `/api/v1/appointments`)

### `PATCH /appointments/{id}/reschedule`

Thực hiện dời lịch hẹn sang thời gian mới và/hoặc bác sĩ mới.

#### Request Headers
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

#### Path Parameters
| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `id` | `UUID` | Có | Định danh lịch hẹn cần dời |

#### Request Body
| Trường | Kiểu dữ liệu | Bắt buộc | Ràng buộc / Mô tả |
| --- | --- | --- | --- |
| `newDoctorId` | `UUID` | Không | ID bác sĩ mới. Nếu để trống hoặc null, giữ nguyên bác sĩ hiện tại. |
| `startTime` | `Instant` (ISO-8601) | Có | Thời gian bắt đầu khám mới. Phải ở tương lai (`> now`). |
| `endTime` | `Instant` (ISO-8601) | Có | Thời gian kết thúc khám mới. Phải sau `startTime`. |
| `reason` | `String` | Có | Lý do đổi lịch hẹn. Tối đa 500 ký tự, không được để trống. |

#### Request Example
```json
{
  "newDoctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
  "startTime": "2026-09-20T09:00:00Z",
  "endTime": "2026-09-20T09:30:00Z",
  "reason": "Bệnh nhân bận việc đột xuất cần đổi sang sáng Chủ nhật"
}
```

#### Successful Response (`200 OK`)
```json
{
  "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "appointmentCode": "APT000100",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
  "startTime": "2026-09-20T09:00:00Z",
  "endTime": "2026-09-20T09:30:00Z",
  "status": "SCHEDULED",
  "reason": "Khám tổng quát",
  "cancelReason": null,
  "checkedInAt": null,
  "completedAt": null,
  "createdAt": "2026-09-14T08:00:00Z",
  "rescheduleHistories": [
    {
      "id": "e9f8a7b6-c5d4-3e2f-1a0b-9c8d7e6f5a4b",
      "appointmentId": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "oldDoctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
      "newDoctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
      "oldDoctorName": "BS. Nguyễn Văn A",
      "newDoctorName": "BS. Trần Văn B",
      "oldStartTime": "2026-09-15T09:00:00Z",
      "oldEndTime": "2026-09-15T09:30:00Z",
      "newStartTime": "2026-09-20T09:00:00Z",
      "newEndTime": "2026-09-20T09:30:00Z",
      "reason": "Bệnh nhân bận việc đột xuất cần đổi sang sáng Chủ nhật",
      "rescheduledBy": "uuuuuuuu-uuuu-uuuu-uuuu-uuuuuuuuuuu1",
      "rescheduledByName": "Lễ tân Quầy 1",
      "rescheduledAt": "2026-09-14T10:30:00Z"
    }
  ]
}
```

---

### `GET /appointments/{id}`

Truy vấn chi tiết một lịch hẹn kèm toàn bộ lịch sử các lần dời lịch (`rescheduleHistories`).

#### Successful Response (`200 OK`)
Cấu trúc trả về đồng nhất với response của `PATCH /appointments/{id}/reschedule`, trường `rescheduleHistories` được sắp xếp theo thời gian dời lịch mới nhất lên đầu (`rescheduledAt DESC`).

---

## 3. Error Responses & Status Codes

| Mã lỗi HTTP | Điều kiện phát sinh | Chi tiết phản hồi |
| --- | --- | --- |
| `400 Bad Request` | Thiếu trường bắt buộc (`startTime`, `endTime`, `reason`), `startTime >= endTime`, hoặc `startTime <= now` | `Validation failed.` hoặc `ValidationException` |
| `400 Bad Request` | Lịch hẹn đã quá giờ khám (`startTime <= now`) theo QTN-08 | `AppointmentPastCutoffException: Lịch hẹn đã quá giờ khám, vui lòng tạo lịch hẹn mới.` |
| `400 Bad Request` | Bác sĩ không có ca làm việc hoặc đang trong lịch nghỉ phép (QTN-30) | `DoctorNotWorkingException: Doctor is not on working schedule or on leave.` |
| `401 Unauthorized` | Không có Bearer token hợp lệ | `Unauthorized` |
| `403 Forbidden` | Người dùng không có vai trò `RECEPTIONIST` hoặc `ADMIN` (hoặc thiếu permission `APPOINTMENT_UPDATE`) | `Forbidden` |
| `404 Not Found` | Không tìm thấy cuộc hẹn (`appointmentId`) hoặc bác sĩ (`newDoctorId`) | `ResourceNotFoundException: Appointment not found` / `Doctor not found` |
| `409 Conflict` | Trạng thái cuộc hẹn không phải `SCHEDULED` hoặc `CONFIRMED` | `AppointmentInvalidStatusException: Chỉ có thể đổi lịch hẹn ở trạng thái SCHEDULED hoặc CONFIRMED.` / `AppointmentAlreadyCancelledException` / `AppointmentAlreadyCompletedException` |
| `409 Conflict` | Bác sĩ đã có lịch khám khác trong khung giờ này (QTN-04) | `AppointmentTimeConflictException` |
