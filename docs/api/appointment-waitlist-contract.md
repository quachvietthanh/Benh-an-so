# NCL-03-CN-012 — Danh sách chờ khi hết khung giờ (Appointment Waitlist Contract)

## 1. Scope & Business Rules

User Story: **NCL-03-CN-012 — Danh sách chờ khi hết khung giờ** (Epic NCL-03 - Lịch hẹn và hàng đợi khám).

### Phân quyền & Vai trò
- **Vai trò được phép:** Lễ tân (`RECEPTIONIST`) hoặc Quản trị viên (`ADMIN`).
- **Quyền yêu cầu (Permissions):**
  - Thêm vào danh sách chờ: `APPOINTMENT_CREATE`
  - Xem danh sách chờ & Lấy gợi ý: `APPOINTMENT_READ`
  - Hủy mục chờ: `APPOINTMENT_UPDATE`
- Các vai trò khác (`DOCTOR`, `PHARMACIST`) hoặc người dùng chưa xác thực sẽ bị từ chối với `403 Forbidden` / `401 Unauthorized`.

### Quy tắc nghiệp vụ (Business Rules)
1. **Điều kiện tiên quyết — Bác sĩ kín lịch (`NCL-03-CN-012-TC-01`):**
   - Chỉ cho phép thêm bệnh nhân vào danh sách chờ khi bác sĩ không còn bất kỳ khung giờ khám trống nào khả dụng trong ngày mong muốn (hoặc trong ca mong muốn: Sáng / Chiều).
   - Nếu bác sĩ vẫn còn khung giờ trống, hệ thống từ chối (`400 Bad Request`, mã lỗi `DOCTOR_HAS_AVAILABLE_SLOTS`) và yêu cầu đặt lịch trực tiếp.
   - Bác sĩ phải có lịch làm việc trong ngày mong muốn (`QTN-30`) và đang ở trạng thái hoạt động (`ACTIVE`).
2. **Thứ tự ưu tiên đăng ký — FIFO (`NCL-03-CN-012-TC-01`):**
   - Danh sách chờ được sắp xếp ưu tiên tuyệt đối theo thứ tự đăng ký trước sau (`created_at ASC`). Bệnh nhân đăng ký trước sẽ luôn đứng đầu danh sách để được gợi ý trước.
3. **Chống trùng lặp danh sách chờ:**
   - Một bệnh nhân không được phép đăng ký chờ nhiều hơn một lần cho cùng một bác sĩ trong cùng một ngày nếu bản ghi cũ vẫn đang ở trạng thái `WAITING` (`409 Conflict`, mã lỗi `PATIENT_ALREADY_IN_WAITLIST`).
4. **Gợi ý tự động khi lịch hẹn bị hủy (`NCL-03-CN-012-TC-02`):**
   - Khi bất kỳ lịch hẹn nào cùng ngày của bác sĩ bị hủy (tại quầy hoặc trực tuyến), hệ thống tự động tìm kiếm người chờ đầu tiên (FIFO) và đính kèm vào response của API hủy lịch qua trường `suggestedWaitlistEntry`.
   - Ngoài ra, lễ tân có thể chủ động tra cứu gợi ý qua endpoint `GET /appointments/waitlist/suggest?doctorId=...&date=...`.
5. **Tự động giải phóng khỏi danh sách chờ khi đặt lịch (`NCL-03-CN-012-TC-03`):**
   - Khi lễ tân đặt lịch khám thành công cho bệnh nhân qua `POST /appointments`, hệ thống tự động chuyển trạng thái bản ghi chờ của bệnh nhân đó sang `SCHEDULED` và liên kết với mã cuộc hẹn vừa tạo (`booked_appointment_id`).
   - Khi tra cứu danh sách chờ active, bệnh nhân đã đặt lịch không còn xuất hiện trong danh sách.
6. **Hủy đăng ký danh sách chờ:**
   - Lễ tân có thể hủy mục chờ khi bệnh nhân báo không còn nhu cầu khám. Bắt buộc nhập lý do hủy, chuyển trạng thái sang `CANCELLED`.
7. **Kiểm toán hệ thống (Audit Log):**
   - Toàn bộ thao tác thêm mới và hủy danh sách chờ đều được ghi nhận vào bảng `audit_logs` với tài nguyên `APPOINTMENT_WAITLIST`.

---

## 2. API Specification

Base path: `/appointments/waitlist`

### 2.1. `POST /appointments/waitlist`
Thêm bệnh nhân vào danh sách chờ khi bác sĩ kín lịch.

#### Request Body
| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `patientId` | `UUID` | Có | Định danh bệnh nhân |
| `doctorId` | `UUID` | Có | Định danh bác sĩ |
| `desiredDate` | `LocalDate` (YYYY-MM-DD) | Có | Ngày mong muốn khám (không được ở quá khứ) |
| `timePreference` | `String` | Không | Ca mong muốn: `ANYTIME` (mặc định), `MORNING`, `AFTERNOON` |
| `note` | `String` | Không | Ghi chú nhu cầu khám (tối đa 500 ký tự) |

#### Successful Response (`201 Created`)
```json
{
  "id": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "patientName": "Nguyễn Văn An",
  "patientPhone": "0901234567",
  "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
  "doctorName": "BS. Trần Thị B",
  "desiredDate": "2026-10-15",
  "timePreference": "ANYTIME",
  "status": "WAITING",
  "note": "Bệnh nhân muốn khám tiêu hóa",
  "cancelReason": null,
  "bookedAppointmentId": null,
  "createdBy": "uuuuuuuu-uuuu-uuuu-uuuu-uuuuuuuuuuu1",
  "createdByName": "Lễ tân Quầy 1",
  "createdAt": "2026-10-01T08:30:00Z",
  "updatedAt": null
}
```

---

### 2.2. `GET /appointments/waitlist`
Tra cứu danh sách chờ theo bộ lọc.

#### Query Parameters
| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `doctorId` | `UUID` | Không | Lọc theo bác sĩ |
| `date` | `LocalDate` | Không | Lọc theo ngày mong muốn khám |
| `status` | `String` | Không | Lọc theo trạng thái (`WAITING` mặc định, `SCHEDULED`, `CANCELLED`, `EXPIRED`) |

#### Successful Response (`200 OK`)
```json
[
  {
    "id": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
    "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
    "patientName": "Nguyễn Văn An",
    "patientPhone": "0901234567",
    "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
    "doctorName": "BS. Trần Thị B",
    "desiredDate": "2026-10-15",
    "timePreference": "ANYTIME",
    "status": "WAITING",
    "note": "Bệnh nhân muốn khám tiêu hóa",
    "createdAt": "2026-10-01T08:30:00Z"
  }
]
```

---

### 2.3. `GET /appointments/waitlist/suggest`
Lấy thông tin người chờ đầu tiên (FIFO) của một bác sĩ trong ngày để liên hệ khi có slot trống.

#### Query Parameters
| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `doctorId` | `UUID` | Có | Định danh bác sĩ |
| `date` | `LocalDate` | Có | Ngày khám cần tìm người chờ |

#### Successful Response
- `200 OK`: Có người đang chờ (trả về người đăng ký sớm nhất):
```json
{
  "waitlistId": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "patientName": "Nguyễn Văn An",
  "patientPhone": "0901234567",
  "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
  "doctorName": "BS. Trần Thị B",
  "desiredDate": "2026-10-15",
  "timePreference": "ANYTIME",
  "note": "Bệnh nhân muốn khám tiêu hóa",
  "createdAt": "2026-10-01T08:30:00Z"
}
```
- `204 No Content`: Không có ai trong danh sách chờ ngày đó.

---

### 2.4. `PATCH /appointments/waitlist/{id}/cancel`
Hủy đăng ký chờ khi bệnh nhân báo bận hoặc không muốn chờ nữa.

#### Request Body
```json
{
  "reason": "Bệnh nhân đã khám ở cơ sở y tế khác"
}
```

#### Successful Response (`200 OK`)
```json
{
  "id": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
  "status": "CANCELLED",
  "cancelReason": "Bệnh nhân đã khám ở cơ sở y tế khác",
  "updatedAt": "2026-10-02T09:15:00Z"
}
```

---

### 2.5. Gợi ý đính kèm khi Hủy lịch hẹn (`PATCH /appointments/{id}/cancel`)
Khi hủy một lịch hẹn bất kỳ, response của cuộc hẹn bị hủy sẽ tự động đính kèm thông tin người chờ đầu tiên (nếu có):
```json
{
  "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "appointmentCode": "APT000100",
  "status": "CANCELLED",
  "cancelReason": "Bệnh nhân bận việc đột xuất",
  "suggestedWaitlistEntry": {
    "waitlistId": "e1f2a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b",
    "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
    "patientName": "Nguyễn Văn An",
    "patientPhone": "0901234567",
    "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
    "doctorName": "BS. Trần Thị B",
    "desiredDate": "2026-10-15",
    "timePreference": "ANYTIME",
    "note": "Bệnh nhân muốn khám tiêu hóa",
    "createdAt": "2026-10-01T08:30:00Z"
  }
}
```
*Nếu không có ai trong danh sách chờ, trường `suggestedWaitlistEntry` trả về `null`.*
