# API Contract: Cảnh Báo Dị Ứng Thuốc Khi Kê Đơn (Prescription Medication Allergy Warning)

> **Module:** NCL-05 — Kê đơn thuốc và cảnh báo tương tác  
> **User Story:** NCL-05-CN-004 — Cảnh báo dị ứng thuốc khi kê đơn  
> **Business Rules:** QTN-26 (*Cảnh báo dị ứng thuốc khi kê đơn*), QTN-05 (*Cảnh báo tương tác thuốc*), QTN-02 (*Ghi nhật ký truy cập bệnh án*), QTN-11 (*Chỉ bác sĩ ghi chuyên môn*)  
> **Acceptance Criteria:** NCL-05-CN-004-TC-01, TC-02, TC-03, TC-04, TC-05  
> **Base URL:** `/prescriptions`  
> **Auth:** Bearer Token (JWT)  
> **Date:** 2026-09-08  

---

## 1. Tổng Quan Nghiệp Vụ & Phân Quyền

1. **Cảnh báo chặn dị ứng thuốc (TC-01, QTN-26)**:
   - Khi bác sĩ kê đơn (`POST /prescriptions`) hoặc sửa đơn (`PATCH /prescriptions/{id}`), hệ thống tự động đối chiếu hoạt chất của các thuốc kê với danh sách dị ứng đang active của bệnh nhân.
   - Nếu phát hiện thuốc trùng với hoạt chất dị ứng mà bác sĩ chưa cung cấp lý do bỏ qua, hệ thống trả về mã lỗi HTTP `409 Conflict` (`ALLERGY_CONFIRMATION_REQUIRED`) kèm danh sách chi tiết các cảnh báo dị ứng phát hiện được.
2. **Xác nhận bỏ qua cảnh báo dị ứng kèm lý do (TC-02, QTN-26)**:
   - Nếu bác sĩ quyết định tiếp tục dùng thuốc do yêu cầu lâm sàng bắt buộc, bác sĩ gửi kèm danh sách `allergyOverrides` gồm `allergyId`, `medicineId` và `overrideReason` (chuỗi văn bản giải trình).
   - Hệ thống cho phép lưu đơn, giữ nguyên thuốc trong đơn và tự động ghi một bản ghi vào bảng `prescription_allergy_warning_logs` để lưu vết bất biến.
3. **Bệnh nhân không có dị ứng (TC-03)**:
   - Nếu bệnh nhân không có tiền sử dị ứng nào hoặc thuốc kê không trùng hoạt chất dị ứng, hệ thống cho phép hoàn tất đơn bình thường mà không hiển thị cảnh báo.
4. **Bảo vệ tính toàn vẹn trạng thái đơn (TC-04)**:
   - Nếu đơn thuốc đã ở trạng thái đã cấp phát (`DISPENSED`) hoặc đã hủy (`CANCELLED`), hệ thống từ chối cho phép mở lại hoặc sửa đổi thuốc (`409 Conflict` - `PRESCRIPTION_INVALID_STATUS`).
5. **Tra cứu nhật ký bỏ qua cảnh báo dị ứng dành cho Quản trị viên (TC-05)**:
   - Quản trị viên (`ADMIN`) có quyền tra cứu danh sách các lần bác sĩ đã bỏ qua cảnh báo dị ứng thuốc trong hệ thống thông qua endpoint `GET /prescriptions/allergy-warning-logs`.

---

## 2. Danh Sách Endpoints

### 2.1. Kiểm tra cảnh báo dị ứng thời gian thực (Pre-check)
- **Method:** `POST`
- **Path:** `/prescriptions/check-allergy-warnings`
- **Permission:** `PRESCRIPTION_CREATE` (hoặc `PRESCRIPTION_UPDATE`)
- **Clinical Scope & Ownership:** Người gọi bắt buộc phải là Bác sĩ đang phụ trách lượt khám (`visit.doctorId == currentUserId`), lượt khám đang diễn ra (`IN_PROGRESS`) và hồ sơ bệnh án chưa bị khóa. Quản trị viên (`ADMIN`) hoặc bác sĩ không phụ trách sẽ bị từ chối với `403 Forbidden`.

#### Request Body
```json
{
  "medicalRecordId": "e0000000-0000-0000-0000-000000000001",
  "medicineIds": [
    "m0000000-0000-0000-0000-000000000001",
    "m0000000-0000-0000-0000-000000000002"
  ]
}
```

#### Response (200 OK)
```json
[
  {
    "allergyId": "a0000000-0000-0000-0000-000000000001",
    "patientId": "70000000-0000-0000-0000-000000000001",
    "medicineId": "m0000000-0000-0000-0000-000000000001",
    "medicineName": "Augmentin 1g",
    "activeIngredient": "Amoxicillin 875mg, Acid Clavulanic 125mg",
    "allergenName": "Amoxicillin",
    "severity": "SEVERE",
    "reaction": "Nổi mề đay toàn thân, khó thở nhẹ"
  }
]
```

---

### 2.2. Kê đơn thuốc có override cảnh báo dị ứng
- **Method:** `POST`
- **Path:** `/prescriptions`
- **Permission:** `PRESCRIPTION_CREATE` (Bác sĩ)

#### Request Body
```json
{
  "medicalRecordId": "e0000000-0000-0000-0000-000000000001",
  "note": "Kê đơn điều trị viêm đường hô hấp",
  "items": [
    {
      "medicineId": "m0000000-0000-0000-0000-000000000001",
      "dosage": "1 viên",
      "frequency": 2,
      "route": "ORAL",
      "durationDays": 7,
      "quantity": 14,
      "instructions": "Uống sau ăn"
    }
  ],
  "allergyOverrides": [
    {
      "allergyId": "a0000000-0000-0000-0000-000000000001",
      "medicineId": "m0000000-0000-0000-0000-000000000001",
      "overrideReason": "Đã cho test lẩy da âm tính và dùng kèm kháng histamin giám sát nội trú"
    }
  ]
}
```

#### Response khi thiếu override (409 Conflict)
```json
{
  "code": "ALLERGY_CONFIRMATION_REQUIRED",
  "message": "All detected medication allergies must be confirmed with an override reason.",
  "path": "/api/v1/prescriptions",
  "details": {
    "warnings": [
      {
        "allergyId": "a0000000-0000-0000-0000-000000000001",
        "patientId": "70000000-0000-0000-0000-000000000001",
        "medicineId": "m0000000-0000-0000-0000-000000000001",
        "medicineName": "Augmentin 1g",
        "activeIngredient": "Amoxicillin 875mg, Acid Clavulanic 125mg",
        "allergenName": "Amoxicillin",
        "severity": "SEVERE",
        "reaction": "Nổi mề đay toàn thân, khó thở nhẹ"
      }
    ]
  }
}
```

---

### 2.3. Tra cứu nhật ký bỏ qua cảnh báo dị ứng (TC-05)
- **Method:** `GET`
- **Path:** `/prescriptions/allergy-warning-logs`
- **Permission:** `PRESCRIPTION_ALLERGY_WARNING_VIEW` (Quản trị viên / ADMIN)

#### Query Parameters
- `doctorId` (UUID, tùy chọn): Lọc theo ID bác sĩ đã override.
- `patientId` (UUID, tùy chọn): Lọc theo ID bệnh nhân.
- `from` (ISO-8601 Instant, tùy chọn): Thời điểm bắt đầu.
- `to` (ISO-8601 Instant, tùy chọn): Thời điểm kết thúc.
- `page` (int, default: 0): Số thứ tự trang.
- `size` (int, default: 20, max: 100): Kích thước trang.

#### Response (200 OK)
```json
{
  "content": [
    {
      "id": "w0000000-0000-0000-0000-000000000001",
      "prescriptionId": "p0000000-0000-0000-0000-000000000001",
      "prescriptionCode": "RX000042",
      "patientId": "70000000-0000-0000-0000-000000000001",
      "patientCode": "BN-00001",
      "patientName": "Nguyễn Văn A",
      "doctorId": "22222222-2222-2222-2222-222222222222",
      "doctorName": "Bác sĩ B",
      "medicineId": "m0000000-0000-0000-0000-000000000001",
      "medicineName": "Augmentin 1g",
      "activeIngredient": "Amoxicillin 875mg, Acid Clavulanic 125mg",
      "allergenName": "Amoxicillin",
      "severity": "SEVERE",
      "reaction": "Nổi mề đay toàn thân",
      "overrideReason": "Đã cho test lẩy da âm tính và dùng kèm kháng histamin giám sát nội trú",
      "handledAt": "2026-09-08T08:30:00Z"
    }
  ],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

## 3. Kiến Trúc Bảo Mật & Phân Tích Tương Tranh (TOCTOU Traceability)

### 3.1. Phạm vi phân quyền Pre-check (Clinical Scope & Ownership)
- Endpoint `POST /prescriptions/check-allergy-warnings` áp dụng cơ chế xác thực hai lớp:
  1. **Tầng API Gateway / Filter**: Yêu cầu người dùng đăng nhập có quyền `PRESCRIPTION_CREATE` hoặc `PRESCRIPTION_UPDATE`.
  2. **Tầng Domain / Application Service**: Yêu cầu người gọi bắt buộc phải là **Bác sĩ đang phụ trách lượt khám** (`visit.doctorId == currentUserId`), lượt khám đang diễn ra (`IN_PROGRESS`), và hồ sơ bệnh án chưa bị khóa (`ensureEditable()`).
- Theo quy định phân quyền hệ thống (VT-05 Quản trị viên: *"Không tham gia nghiệp vụ khám bệnh và không kê đơn thuốc"*), tài khoản Quản trị viên (`ADMIN`) hoặc bác sĩ không phụ trách ca khám nếu gọi endpoint này sẽ nhận mã lỗi **HTTP 403 Forbidden** (`AccessDeniedException`).

### 3.2. Phân tích rủi ro TOCTOU (Time-of-Check to Time-of-Use)
- **Bản chất của Pre-check**: Endpoint `check-allergy-warnings` được thiết kế như một công cụ tư vấn giao diện người dùng thời gian thực (real-time UI advisory assistant). Endpoint này không khóa bản ghi trong cơ sở dữ liệu để đảm bảo hiệu năng và tránh tranh chấp khóa không cần thiết khi bác sĩ đang thao tác trên màn hình.
- **Rủi ro tương tranh tiềm ẩn**: Tồn tại một khoảng thời gian trễ giữa lúc bác sĩ gọi pre-check và thời điểm bác sĩ gửi lệnh lưu đơn thuốc (`POST /prescriptions` hoặc `PATCH /prescriptions/{id}`). Trong khoảng thời gian này, có thể có dị ứng mới của bệnh nhân được ghi nhận thêm bởi nhân viên y tế khác.
- **Cơ chế bảo vệ toàn vẹn dữ liệu (Authoritative Enforcement)**:
  - Hệ thống không tin cậy hoàn toàn vào kết quả pre-check ở client.
  - Khi bác sĩ thực hiện lưu đơn (`CreatePrescriptionService` hoặc `AmendPrescriptionService`), nghiệp vụ luôn được bao bọc trong một Transaction với khóa bi quan (`PESSIMISTIC_WRITE` qua `findByIdForUpdate`).
  - Tại đây, hệ thống **tái kiểm tra toàn bộ hoạt chất đối chiếu với danh sách dị ứng đang active của bệnh nhân**.
  - Nếu phát hiện bất kỳ dị ứng nào chưa được cung cấp lý do bỏ qua (`allergyOverrides`), hệ thống lập tức rollback giao dịch và trả về mã lỗi **HTTP 409 Conflict** (`ALLERGY_CONFIRMATION_REQUIRED`) kèm danh sách dị ứng mới phát sinh.
  - Vì vậy, tính toàn vẹn dữ liệu và an toàn lâm sàng cho người bệnh luôn được đảm bảo tuyệt đối.

