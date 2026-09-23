# NCL-14-CN-008 — Thông báo và nhắc lịch trên cổng bệnh nhân (Backend)

> Base URL: `/patient-portal/notifications`
>
> Auth: Bearer Token (JWT) — Vai trò bắt buộc: `ROLE_PATIENT`
>
> Quy tắc liên quan: `QTN-23`, dependency `NCL-14-CN-002`, `NCL-03-CN-005`

## 1. Business rules (từ workbook)

User story (NCL-14-CN-008):

> Là bệnh nhân, tôi muốn nhận thông báo nhắc lịch hẹn và kết quả trên cổng, để
> không bỏ lỡ lịch khám và biết khi nào có kết quả.

Danh sách thông báo trên cổng chỉ gồm đúng ba loại do workbook nêu rõ:

| Loại | Kích hoạt | Nội dung |
| :--- | :--- | :--- |
| `APPOINTMENT_REMINDER` | Nhắc lịch trước giờ hẹn (NCL-03-CN-005) | Bác sĩ + khung giờ (TC-01) |
| `APPOINTMENT_CHANGED` | Lịch hẹn bị dời tại quầy (TC-02) | Khung giờ cũ + khung giờ mới |
| `LAB_RESULT_AVAILABLE` | Có kết quả cận lâm sàng mới | Thông báo chung (không kèm chi tiết lâm sàng) |

Điều kiện (precondition): bệnh nhân có tài khoản cổng và có lịch hẹn/kết quả liên quan.

Hậu điều kiện (postcondition): thông báo chỉ hiển thị cho đúng bệnh nhân, đánh dấu đã đọc được, không lộ dữ liệu người khác (QTN-23).

## 2. Supported notification types

Enum `PatientPortalNotificationType`:

- `APPOINTMENT_REMINDER`
- `APPOINTMENT_CHANGED`
- `LAB_RESULT_AVAILABLE`

Không thêm kênh/hạng mục ngoài lề (SMS, email, push, WhatsApp…) vì workbook không yêu cầu.

## 3. Data model

Bảng `patient_portal_notifications` (Flyway `V87__create_patient_portal_notifications.sql`):

| Cột | Kiểu | Ghi chú |
| :--- | :--- | :--- |
| `id` | BINARY(16) PK | UUID |
| `patient_id` | BINARY(16) NOT NULL FK→patients | Chủ sở hữu (QTN-23) |
| `type` | VARCHAR(40) NOT NULL | 3 giá trị enum |
| `title` | VARCHAR(255) NOT NULL | Tiêu đề an toàn |
| `message` | TEXT NOT NULL | Nội dung snapshot |
| `read_at` | TIMESTAMP NULL | NULL = chưa đọc |
| `created_at` | TIMESTAMP NOT NULL | |
| `appointment_id` | BINARY(16) NULL FK→appointments | reminder + changed |

## 4. Ownership & security (QTN-23)

- Mọi endpoint yêu cầu `ROLE_PATIENT` (do `SecurityConfig` chặn `/patient-portal/**`).
- Ownership luôn suy từ security context: `patientRepository.findByUserId(currentUserPort.getCurrentUserId())` — **không** tin `patientId`/`userId` do client gửi.
- Với detail/mark-read, `PatientAccessGuard.requirePatientOwnership(notification.patientId, ResourceType.PATIENT_PORTAL, notification.id)` ghi audit `ACCESS_DENIED` (REQUIRES_NEW) và ném 403 khi truy cập chéo bệnh nhân.
- Không có IDOR, không leak sự tồn tại của thông báo: id không tồn tại và id của bệnh nhân khác đều được che thành 404 / 403 tương ứng.

## 5. API contract

### `GET /patient-portal/notifications?limit=50`

- Trả về danh sách thông báo của chính bệnh nhân, mới nhất trước (`created_at DESC`).
- `limit` mặc định `50`, tối đa `100`.

### `GET /patient-portal/notifications/{id}`

- Chi tiết một thông báo (ownership-checked).

### `PATCH /patient-portal/notifications/{id}/read`

- Đánh dấu đã đọc. Idempotent: `read_at` chỉ được ghi lần đầu.

Response schema (mỗi thông báo):

```json
{
  "id": "uuid",
  "type": "APPOINTMENT_REMINDER",
  "title": "Nhắc lịch hẹn",
  "message": "Bạn có lịch hẹn lúc 09:00 ngày 01/10/2026. Mã lịch hẹn: APT000123. Bác sĩ: ...",
  "read": false,
  "readAt": null,
  "createdAt": "2026-09-30T01:00:00Z"
}
```

Errors: `401` (thiếu JWT), `403` (không liên kết hồ sơ / truy cập chéo), `404` (không tồn tại — thông điệp chung).

## 6. Reminder behavior

Tái sử dụng hạ tầng nhắc lịch hiện có của NCL-03-CN-005:

- `AppointmentReminderScheduler` (`@Scheduled`, `@ConditionalOnProperty(appointment.reminder.enabled)`) → `SendDueAppointmentRemindersService` → `ProcessAppointmentReminderService`.
- Mốc nhắc: `appointment.reminder.advance-hours=24` (không có mốc mới trong workbook — **giữ nguyên quy ước 24h sẵn có**).
- Không tạo reminder cho lịch đã hủy/không còn hợp lệ (kiểm tra `SCHEDULED`/`CONFIRMED` + `startTime > now`).
- Chống trùng: `ProcessAppointmentReminderService` đã chặn bằng `existsSentReminderByAppointmentId`; thông báo portal được tạo ngay sau log gửi thành công, có unique index chặn trùng thêm.

## 7. Appointment-change behavior

- Tích hợp vào `RescheduleAppointmentService` (đổi lịch tại quầy, `APPOINTMENT_UPDATE`), sau khi ghi `appointment_reschedule_logs`.
- Thông báo gắn với đúng bệnh nhân của lịch hẹn; chống trùng theo `reschedule_log_id`.

## 8. Clinical/lab-result behavior

- Tích hợp vào `ClinicalResultService.finalizeResult(...)` — thời điểm kết quả chuyển `FINAL` (thời điểm "có kết quả" mà cổng bệnh nhân hiển thị, khớp `LookupPortalResultService`).
- Kết quả chưa xác nhận (DRAFT/CORRECTED) **không** tạo thông báo.
- Nội dung an toàn: "Bạn có kết quả cận lâm sàng mới." — không đưa chi tiết lâm sàng vào tiêu đề/nội dung.
- Chống trùng theo `clinical_result_id`.

## 9. Read/unread

- Chỉ có `mark one read`; không có unread-count hay mark-all-read (workbook không yêu cầu).
- `read_at` được lưu; đánh dấu lại là an toàn (idempotent).

## 10. Assumptions (workbook im lặng → ghi rõ giả định)

1. **"Lịch bị đổi" chỉ gồm đổi lịch tại quầy** (TC-02 "dời tại quầy"). Đổi/hủy do bệnh nhân tự thao tác trên cổng (NCL-14-CN-004) và hủy lịch **nằm ngoài phạm vi** (workbook chỉ liệt kê 3 loại, không có loại "hủy").
2. **Mốc nhắc 24h** được tái sử dụng từ cấu hình `appointment.reminder.advance-hours` hiện có — không phát minh mốc mới.
3. **Snapshot** nội dung thông báo, không phát sinh lại từ dữ liệu sống.
4. Không có endpoint unread-count / mark-all-read.

## 11. Known limitations

- Kiểm thử persistence chạy trên H2 (Flyway); kiểm chứng runtime MySQL/Flyway/Testcontainers chưa thực hiện do môi trường không có Docker.

| `reschedule_log_id` | BINARY(16) NULL FK→appointment_reschedule_logs | changed |
| `clinical_result_id` | BINARY(16) NULL FK→clinical_results | lab result |

**Snapshot semantics:** title/message được ghi tại thời điểm tạo, không phát sinh lại từ dữ liệu sống.

**Idempotency (database-level):** 3 unique index một phần (NULL phân biệt):

- `uk_ppn_reminder (patient_id, type, appointment_id)`
- `uk_ppn_changed (patient_id, type, reschedule_log_id)`
- `uk_ppn_lab_result (patient_id, type, clinical_result_id)`
