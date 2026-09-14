# NCL-03-CN-008 — Xác nhận lịch hẹn (Appointment Confirmation Contract)

## 1. Scope & Business Rules

User Story: **NCL-03-CN-008 — Xác nhận lịch hẹn** (Epic NCL-03 - Lịch hẹn và hàng đợi khám).

### Phân quyền & Vai trò
- **Xác nhận tại quầy / qua điện thoại:**
  - Vai trò được phép: Lễ tân (`RECEPTIONIST`) hoặc Quản trị viên (`ADMIN`).
  - Quyền yêu cầu (Permission): `APPOINTMENT_UPDATE`.
- **Tra cứu lịch chưa xác nhận:**
  - Vai trò được phép: Lễ tân (`RECEPTIONIST`) hoặc Quản trị viên (`ADMIN`).
  - Quyền yêu cầu (Permission): `APPOINTMENT_READ`.
- **Bệnh nhân tự xác nhận qua Cổng thông tin (Patient Portal):**
  - Vai trò được phép: Bệnh nhân (`PATIENT`).
  - Giới hạn phạm vi: Chỉ được xác nhận cuộc hẹn gắn với chính hồ sơ bệnh nhân của mình (`QTN-23`).
- Các vai trò khác (`DOCTOR`, `PHARMACIST`) không có quyền thực hiện xác nhận lịch hẹn.

### Quy tắc nghiệp vụ (Business Rules)
1. **Trạng thái hợp lệ (`NCL-03-CN-008-TC-01`, `TC-02`, `TC-03`):**
   - Chỉ cho phép xác nhận các cuộc hẹn đang ở trạng thái `SCHEDULED`.
   - Các trạng thái khác (`CONFIRMED`, `CANCELLED`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`, `NO_SHOW`) bị từ chối với `409 Conflict` (`AppointmentInvalidStatusException`).
2. **Khống chế thời gian xác nhận:**
   - Không cho phép xác nhận cuộc hẹn đã quá giờ khám (`startTime <= now`). Hệ thống trả về `AppointmentPastCutoffException` (`400 Bad Request`).
3. **Chống Race Condition & Double-Submit:**
   - Cả hai luồng xác nhận (Lễ tân và Bệnh nhân) đều áp dụng khóa bi quan (`PESSIMISTIC_WRITE`) thông qua `findByIdForUpdate(appointmentId)`.
4. **Kiểm toán hệ thống (Audit Log):**
   - **Xác nhận thành công:** Ghi nhật ký vào bảng `audit_logs` với `actionType: UPDATE`, `resourceType: APPOINTMENT`, kèm chi tiết `channel` (`RECEPTION_COUNTER` hoặc `ONLINE_PORTAL`).
   - **Từ chối quyền truy cập:** Ghi nhận độc lập qua `AppointmentAccessDeniedAuditWriter` với `@Transactional(propagation = Propagation.REQUIRES_NEW)` để bản ghi kiểm toán `ACCESS_DENIED` không bị rollback theo giao dịch chính.

---

## 2. API Specification

Base path: `/appointments` và `/patient-portal/appointments`

### 2.1. `PATCH /appointments/{id}/confirm`
Lễ tân hoặc Quản trị viên xác nhận lịch hẹn tại quầy hoặc qua điện thoại.

#### Request Headers
```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

#### Path Parameters
| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `id` | `UUID` | Có | ID cuộc hẹn cần xác nhận |

#### Request Body
*Không yêu cầu body (Empty body).*

#### Successful Response (`200 OK`)
```json
{
  "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "appointmentCode": "APT000100",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
  "startTime": "2026-09-20T09:00:00Z",
  "endTime": "2026-09-20T09:30:00Z",
  "status": "CONFIRMED",
  "reason": "Khám sức khỏe tổng quát",
  "cancelReason": null,
  "checkedInAt": null,
  "completedAt": null,
  "createdAt": "2026-09-14T08:00:00Z",
  "confirmedAt": "2026-09-14T09:15:00Z",
  "confirmedBy": "uuuuuuuu-uuuu-uuuu-uuuu-uuuuuuuuuuu1",
  "confirmedByName": "Lễ Tân Nguyễn Văn A",
  "rescheduleHistories": []
}
```

#### Error Responses
- `400 Bad Request`: Lịch hẹn đã quá giờ khám (`AppointmentPastCutoffException`).
- `401 Unauthorized`: Chưa xác thực token.
- `403 Forbidden`: Người dùng không có quyền `APPOINTMENT_UPDATE` hoặc không có role `RECEPTIONIST`/`ADMIN`.
- `404 Not Found`: Không tìm thấy lịch hẹn.
- `409 Conflict`: Lịch hẹn không ở trạng thái `SCHEDULED` (đã hủy, đã xác nhận trước đó, v.v.).

---

### 2.2. `GET /appointments/unconfirmed`
Tra cứu danh sách các cuộc hẹn chưa xác nhận theo ngày (được sắp xếp `startTime ASC`) để lễ tân gọi điện nhắc hẹn.

#### Request Headers
```http
Authorization: Bearer <access_token>
```

#### Query Parameters
| Tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả |
| :--- | :--- | :---: | :--- | :--- |
| `date` | `LocalDate` (`YYYY-MM-DD`) | Không | Ngày hiện tại | Ngày cần tra cứu |
| `page` | `int` | Không | `0` | Số thứ tự trang (>= 0) |
| `size` | `int` | Không | `20` | Số lượng bản ghi mỗi trang (1-100) |

> **Lưu ý nghiệp vụ:** Khi tra cứu ngày hiện tại (`today`), danh sách chỉ trả về các cuộc hẹn có `startTime > now`. Nếu truyền ngày trong quá khứ, API trả về danh sách rỗng.

#### Successful Response (`200 OK`)
```json
{
  "content": [
    {
      "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
      "appointmentCode": "APT000101",
      "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb002",
      "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
      "startTime": "2026-09-14T14:30:00Z",
      "endTime": "2026-09-14T15:00:00Z",
      "status": "SCHEDULED",
      "reason": "Khám tim mạch",
      "cancelReason": null,
      "checkedInAt": null,
      "completedAt": null,
      "createdAt": "2026-09-13T08:00:00Z",
      "confirmedAt": null,
      "confirmedBy": null,
      "confirmedByName": null,
      "rescheduleHistories": []
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

#### Error Responses
- `400 Bad Request`: `page < 0` hoặc `size` ngoài khoảng `1-100`.
- `401 Unauthorized`: Chưa xác thực token.
- `403 Forbidden`: Người dùng không có quyền `APPOINTMENT_READ`.

---

### 2.3. `PATCH /patient-portal/appointments/{id}/confirm`
Bệnh nhân tự xác nhận cuộc hẹn sắp tới thông qua Cổng thông tin bệnh nhân.

#### Request Headers
```http
Authorization: Bearer <patient_token>
Content-Type: application/json
```

#### Path Parameters
| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `id` | `UUID` | Có | ID cuộc hẹn của chính bệnh nhân |

#### Request Body
*Không yêu cầu body (Empty body).*

#### Successful Response (`200 OK`)
```json
{
  "id": "c1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
  "appointmentCode": "APT000100",
  "patientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
  "doctorId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3",
  "startTime": "2026-09-20T09:00:00Z",
  "endTime": "2026-09-20T09:30:00Z",
  "status": "CONFIRMED",
  "reason": "Khám sức khỏe tổng quát",
  "bookingChannel": "ONLINE_PORTAL",
  "createdAt": "2026-09-14T08:00:00Z",
  "confirmedAt": "2026-09-14T09:20:00Z"
}
```

#### Error Responses
- `400 Bad Request`: Cuộc hẹn đã quá giờ khám (`AppointmentPastCutoffException`).
- `401 Unauthorized`: Chưa xác thực token.
- `403 Forbidden`: Tài khoản không có vai trò `PATIENT` hoặc cố gắng xác nhận cuộc hẹn của bệnh nhân khác (`QTN-23`).
- `404 Not Found`: Không tìm thấy cuộc hẹn.
- `409 Conflict`: Cuộc hẹn không ở trạng thái `SCHEDULED` (đã hủy, đã xác nhận trước đó).
