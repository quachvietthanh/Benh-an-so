# API Contract: Người giám hộ đặt lịch khám cho bệnh nhân phụ thuộc (Guardian Family Appointment)

> **User Story:** `NCL-14-CN-010` (Epic `NCL-14` — Cổng bệnh nhân)
> **Quy tắc nghiệp vụ:** `QTN-23` (bệnh nhân chỉ truy cập dữ liệu của chính mình), `QTN-44` (hồ sơ người chưa thành niên phải có người giám hộ)
> **Tiêu chí chấp nhận:** `NCL-14-CN-010-TC-01`, `NCL-14-CN-010-TC-02`, `NCL-14-CN-010-TC-03`
> **Quyền hạn:** `PATIENT_UPDATE` (gán người giám hộ — ADMIN / DOCTOR / RECEPTIONIST) · `ROLE_PATIENT` (cổng bệnh nhân)
> **Phạm vi:** Backend (CV-01, CV-02, CV-04). Frontend (CV-03) không thuộc thay đổi này.

---

## 1. Phân tích nghiệp vụ (CV-01)

### 1.1. Vấn đề nghiệp vụ

Trước `NCL-14-CN-010`, tài khoản cổng bệnh nhân chỉ thao tác được trên **hồ sơ bệnh nhân của chính mình** (`PatientAccessGuard.requirePatientOwnership` — `NCL-14-CN-002` / `QTN-23`). Phụ huynh / người giám hộ của bệnh nhân chưa thành niên không thể đặt lịch hoặc theo dõi lịch hẹn cho con của mình.

### 1.2. Nguồn thẩm quyền duy nhất (single source of truth)

`patients.guardian_user_id` (đã tồn tại từ `V57__add_guardian_fields_to_patients.sql`) là **nguồn duy nhất** xác định quan hệ giám hộ giữa một tài khoản cổng và một hồ sơ bệnh nhân phụ thuộc.

- Không tạo bảng quan hệ giám hộ mới.
- Không tạo migration mới, không thêm cột, không thêm index.
- Không tin bất kỳ trường nào do client gửi lên (`guardianUserId`, `userId`, `accountId`, `role`, cờ “ownership”).

### 1.3. Quy tắc nghiệp vụ

| # | Quy tắc |
|---|---|
| BR-01 | Một tài khoản cổng chỉ thao tác được trên: (a) hồ sơ của chính mình, hoặc (b) hồ sơ phụ thuộc có `guardian_user_id` = id tài khoản đang đăng nhập. |
| BR-02 | `patientId` do client gửi **không phải** bằng chứng thẩm quyền. Mọi yêu cầu phải được uỷ quyền lại phía server từ danh tính đã xác thực + trạng thái CSDL. |
| BR-03 | Gán `guardianUserId` chỉ do nhân viên có `PATIENT_UPDATE` thực hiện. Vai trò `PATIENT` có **0 quyền** (`V27__seed_patient_portal_role.sql`). |
| BR-04 | Bỏ trống `guardianUserId` khi cập nhật ⇒ **giữ nguyên** giá trị đang lưu (không thể vô tình xoá liên kết giám hộ). |
| BR-05 | `transitionToAdult = true` ⇒ xoá `guardianUserId` cùng toàn bộ bộ ba thông tin người giám hộ. |
| BR-06 | Chặn liên kết giám hộ vòng mức 1 (`A ↔ B`, tức chặng trực tiếp của `A → B → C`). |
| BR-07 | Huỷ / xác nhận / đổi lịch hẹn vẫn **chỉ dành cho chính bệnh nhân** — giới hạn sản phẩm có chủ đích, xem §8. |

### 1.4. Tái sử dụng chính sách tuổi

Không viết lại logic tuổi / chưa thành niên. Tái sử dụng `PatientMinorPolicy` (ngưỡng 18 tuổi, múi giờ `Asia/Ho_Chi_Minh`) qua `Patient.getAge()`, `Patient.isMinor()` và `Patient.requiresAdultTransition()`.

---

## 2. Ma trận phân quyền

| Endpoint | Method | ADMIN | DOCTOR | RECEPTIONIST | PATIENT |
|---|---|---|---|---|---|
| `/patients/{patientId}` (gán `guardianUserId`) | PUT | ✅ `PATIENT_UPDATE` | ✅ | ✅ | ❌ 403 + audit `ACCESS_DENIED` |
| `/patient-portal/patients/linked` | GET | ❌ | ❌ | ❌ | ✅ `ROLE_PATIENT` |
| `/patient-portal/appointments?patientId=` | GET | ❌ | ❌ | ❌ | ✅ |
| `/patient-portal/appointments` (body `patientId`) | POST | ❌ | ❌ | ❌ | ✅ |
| `/patient-portal/appointments/{id}?patientId=` | GET | ❌ | ❌ | ❌ | ✅ |
| `/patient-portal/appointments/{id}/cancel` | PATCH | ❌ | ❌ | ❌ | ✅ (chỉ hồ sơ của chính mình) |
| `/patient-portal/appointments/{id}/confirm` | PATCH | ❌ | ❌ | ❌ | ✅ (chỉ hồ sơ của chính mình) |
| `/patient-portal/appointments/{id}/reschedule` | PUT | ❌ | ❌ | ❌ | ✅ (chỉ hồ sơ của chính mình) |

`/patient-portal/**` được bảo vệ bằng `hasRole("PATIENT")` trong `SecurityConfig`; thẩm quyền theo từng hồ sơ bệnh nhân do `PatientAccessGuard` thực thi.

---## 3. API chi tiết

### 3.1. GET /patient-portal/patients/linked

Trả về danh sách hồ sơ bệnh nhân mà tài khoản đang đăng nhập được phép thao tác: **hồ sơ của chính mình trước**, sau đó là các hồ sơ phụ thuộc sắp xếp theo `fullName` tăng dần (`findAllByGuardianUserIdOrderByFullNameAsc`).

**Response 200**

```json
[
  {
    "patientId": "e4b2d19f-1111-4567-8901-abcdef123456",
    "patientCode": "BN000001",
    "fullName": "Nguyễn Văn A",
    "dateOfBirth": "1995-05-10",
    "age": 31,
    "isMinor": false,
    "relationship": "SELF",
    "self": true,
    "requiresGuardianLinkReview": false
  },
  {
    "patientId": "7c1a0b88-2222-4567-8901-abcdef123456",
    "patientCode": "BN000002",
    "fullName": "Nguyễn Văn Con",
    "dateOfBirth": "2015-05-10",
    "age": 11,
    "isMinor": true,
    "relationship": "Bo",
    "self": false,
    "requiresGuardianLinkReview": false
  }
]
```

| Trường | Nguồn dữ liệu |
|---|---|
| `patientId`, `patientCode`, `fullName`, `dateOfBirth` | `Patient` |
| `age` | `Patient.getAge()` → `PatientMinorPolicy.calculateAge` |
| `isMinor` | `Patient.isMinor()` → `PatientMinorPolicy.isMinor` |
| `relationship` | `SELF` cho hồ sơ của chính mình; `guardianRelationship` đang lưu cho hồ sơ phụ thuộc |
| `self` | `true` khi `patients.user_id` = id tài khoản đang đăng nhập |
| `requiresGuardianLinkReview` | `Patient.requiresAdultTransition()` (TC-03) |

**KHÔNG trả về:** `allergies`, `chronicDiseases`, `medicalHistory`, `identityNumber`, `insuranceNumber`, `phone`, `address`, `email`, `guardianUserId`.

**Lỗi**

| HTTP | code | Nguyên nhân |
|---|---|---|
| 401 | `AUTHENTICATION_FAILED` | Chưa đăng nhập |
| 403 | `ACCESS_DENIED` | Không phải `ROLE_PATIENT` |

Tài khoản là người giám hộ nhưng không có hồ sơ bệnh nhân cho chính mình ⇒ vẫn `200` và chỉ trả về các hồ sơ phụ thuộc (không ném lỗi).

---

### 3.2. GET /patient-portal/appointments

| Tham số | Bắt buộc | Mô tả |
|---|---|---|
| `status` | không | Lọc theo trạng thái; bỏ trống ⇒ `SCHEDULED`, `CONFIRMED` |
| `patientId` | không | `null` ⇒ hồ sơ của chính mình (hành vi cũ, không đổi). Có giá trị ⇒ hồ sơ phụ thuộc được uỷ quyền qua `PatientAccessGuard.requirePatientAccess` |

```http
GET /patient-portal/appointments?patientId=7c1a0b88-2222-4567-8901-abcdef123456
Authorization: Bearer <token>
```

Response là mảng `PatientAppointmentResponse` giống hệt trước đây (`id`, `appointmentCode`, `patientId`, `doctorId`, `startTime`, `endTime`, `status`, `reason`, `bookingChannel`, `createdAt`, `confirmedAt`).

### 3.3. GET /patient-portal/appointments/{id}

| Tham số | Bắt buộc | Mô tả |
|---|---|---|
| `patientId` | không | `null` ⇒ chỉ hồ sơ của chính mình (`requirePatientOwnership`). Có giá trị ⇒ phạm vi tương ứng, **và** lịch hẹn bắt buộc phải thuộc đúng phạm vi đó (chống IDOR) |

| HTTP | code | Nguyên nhân |
|---|---|---|
| 403 | `ACCESS_DENIED` | `patientId` ngoài phạm vi, hoặc lịch hẹn không thuộc phạm vi yêu cầu |
| 404 | `APPOINTMENT_NOT_FOUND` | Không tồn tại lịch hẹn |

---

### 3.4. POST /patient-portal/appointments (đặt lịch hộ người phụ thuộc)

**Request body** — thêm trường tuỳ chọn `patientId`:

```json
{
  "doctorId": "9f2c44aa-3333-4567-8901-abcdef123456",
  "appointmentDate": "2099-08-10",
  "startTime": "09:00",
  "reason": "Khám nhi",
  "patientId": "7c1a0b88-2222-4567-8901-abcdef123456"
}
```

| Trường | Bắt buộc | Ghi chú |
|---|---|---|
| `doctorId` | ✅ | |
| `appointmentDate` | ✅ | `yyyy-MM-dd` |
| `startTime` | ✅ | `HH:mm`, phải theo mốc 30 phút |
| `reason` | không | Tối đa 500 ký tự; mặc định `Đặt lịch hẹn trực tuyến` |
| `patientId` | không | Tương thích ngược tuyệt đối: bỏ trống ⇒ đặt cho chính mình (hành vi cũ) |

**Response 201** — `PatientAppointmentResponse`; `patientId` là **hồ sơ đích** (bệnh nhân phụ thuộc), không phải tài khoản người đặt.

Sau khi xác định hồ sơ đích, toàn bộ luồng đặt lịch hiện có được tái sử dụng nguyên vẹn: khoá bác sĩ, lịch tuần / lịch ngày, canh mốc 30 phút, chặn quá khứ, chặn thời gian nghỉ, chống trùng slot, xử lý waitlist, tạo `Appointment`, transaction và audit.

**Audit `CREATE` / `APPOINTMENT`:**

```json
{
  "patientId": "7c1a0b88-2222-4567-8901-abcdef123456",
  "doctorId": "9f2c44aa-3333-4567-8901-abcdef123456",
  "appointmentTime": "2099-08-10T02:00:00Z",
  "channel": "ONLINE_PORTAL",
  "bookedAt": "2026-09-25T02:00:00Z",
  "bookedByUserId": "a1b2c3d4-4444-4567-8901-abcdef123456",
  "bookingOnBehalfOfDependent": true
}
```

| Trường audit | Ý nghĩa |
|---|---|
| `bookedByUserId` | Danh tính đã xác thực thực hiện hành động |
| `bookingOnBehalfOfDependent` | `true` khi bệnh nhân đích không phải chủ tài khoản (`!actorId.equals(patient.userId)`) |

---

### 3.5. PUT /patients/{patientId} — gán người giám hộ (nhân viên)

**Permission:** `PATIENT_UPDATE` (ADMIN / DOCTOR / RECEPTIONIST).

```json
{
  "fullName": "Nguyễn Văn Con",
  "dateOfBirth": "2015-05-10",
  "gender": "MALE",
  "active": true,
  "guardianName": "Nguyễn Văn Cha",
  "guardianRelationship": "Bo",
  "guardianPhone": "0912345678",
  "guardianUserId": "a1b2c3d4-4444-4567-8901-abcdef123456"
}
```

**Quy tắc kiểm tra `guardianUserId`**

| # | Điều kiện | Kết quả khi vi phạm |
|---|---|---|
| 1 | Bỏ trống | Giữ nguyên `guardianUserId` đang lưu |
| 2 | Khác `patients.user_id` của chính bệnh nhân | `400 VALIDATION_FAILED` (`guardianUserId`) |
| 3 | Tài khoản phải tồn tại | `400 VALIDATION_FAILED` |
| 4 | Tài khoản phải đang hoạt động (`active = true`) | `400 VALIDATION_FAILED` |
| 5 | Tài khoản phải thuộc vai trò `PATIENT` | `400 VALIDATION_FAILED` |
| 6 | Không tạo liên kết vòng mức 1 | `400 VALIDATION_FAILED` |
| 7 | `transitionToAdult = true` | Xoá `guardianUserId` |

`guardianUserId` được ghi nhận vào audit thay đổi hồ sơ qua `PatientChangeDetailBuilder.forUpdate`.

---## 4. Truy vết tiêu chí chấp nhận

### 4.1. TC-01 — Tài khoản cổng thấy được các hồ sơ mình được phép thao tác

| Hạng mục | Chi tiết |
|---|---|
| Triển khai | `GET /patient-portal/patients/linked` → `GetPatientLinkedProfilesService` (usecase `GetPatientLinkedProfilesUseCase`) |
| Nguồn phạm vi | `patientRepository.findByUserId(currentUserId)` + `patientRepository.findAllByGuardianUserIdOrderByFullNameAsc(currentUserId)` |
| Bằng chứng | `GetPatientLinkedProfilesServiceTest` (7 ca), `PatientPortalPatientControllerTest` (5 ca) |

### 4.2. TC-02 — Người giám hộ đặt lịch và theo dõi lịch hẹn cho người phụ thuộc

| Hạng mục | Chi tiết |
|---|---|
| Đặt lịch | `POST /patient-portal/appointments` + trường tuỳ chọn `patientId` |
| Danh sách | `GET /patient-portal/appointments?patientId=` |
| Chi tiết | `GET /patient-portal/appointments/{id}?patientId=` |
| Bằng chứng | `PatientBookAppointmentServiceTest.booksAppointmentForLinkedDependentWithOnBehalfAuditContext`, `…dependentBookingStillEnforcesSlotCollisionRules`, `…rejectsBookingWhenDependentScopeIsNotAuthorised`; `GetPatientPortalAppointmentsServiceTest.listsAppointmentsOfLinkedDependentWhenPatientIdSupplied`; `GetPatientPortalAppointmentDetailServiceTest.returnsDependentAppointmentDetailWhenScopeAuthorised`; `PatientPortalAppointmentControllerTest` |
| Truy vết actor | Audit `bookedByUserId` + `bookingOnBehalfOfDependent` |

### 4.3. TC-03 — Rà soát / chuyển tiếp khi người phụ thuộc đủ 18 tuổi

**Đã triển khai**

- Cờ `requiresGuardianLinkReview` trên `GET /patient-portal/patients/linked`, tính bằng `Patient.requiresAdultTransition()` → `PatientMinorPolicy.requiresAdultTransition(dateOfBirth, guardianName)`.
- Việc xoá liên kết giám hộ khi chuyển tiếp thành niên đã tồn tại (`Patient.transitionToAdult()` đặt `guardianUserId = null`) và được **giữ nguyên**, có test hồi quy.
- Bằng chứng: `GetPatientLinkedProfilesServiceTest.requiresGuardianLinkReviewFlaggedWhenAnAdultStillHasAGuardian`, `PatientPortalPatientControllerTest.exposesRequiresGuardianLinkReviewFlagForTc03`, `UpdatePatientServiceTest.transitionToAdultClearsGuardianUserId`.

**CHƯA triển khai — khoảng trống nghiệp vụ (không được coi là đã đạt)**

Phần “thông báo cho người dùng” của TC-03 **không** được triển khai. Để làm được cần cả 4 hạng mục sau, và yêu cầu nghiệp vụ chưa quy định rõ:

1. **Loại thông báo mới** trong `PatientPortalNotificationType` — enum hiện đang bị ràng buộc bởi CHECK `chk_ppn_type` trong `V87__create_patient_portal_notifications.sql` (chỉ 3 giá trị), nên cần **migration mới** để nới ràng buộc.
2. **Mô hình người nhận** — chưa xác định thông báo cho người giám hộ, cho bệnh nhân, hay cả hai.
3. **Sweep job / scheduler** — phát hiện sự kiện “đã đủ 18 tuổi nhưng còn liên kết giám hộ”.
4. **Nội dung + thời điểm** gửi.

Vì vậy TC-03 chỉ được coi là **đạt phần cờ rà soát**, không phải đạt toàn bộ.

### 4.4. QTN-23 — Cách ly phạm vi dữ liệu bệnh nhân

`PatientAccessGuard.requirePatientOwnership(...)` **giữ nguyên từng byte**. Phạm vi gia đình được thêm bằng phương thức **riêng** `requirePatientAccess(...)`, nên các tài nguyên cũ vẫn chỉ-dành-cho-chính-mình.

Bằng chứng: `PatientAccessGuardTest` — 4 ca gốc không đổi + 8 ca mới, trong đó `requirePatientOwnershipStillDeniesALinkedDependent` chứng minh hồ sơ phụ thuộc **vẫn bị từ chối** bởi API cũ.

### 4.5. QTN-44 — Hồ sơ người chưa thành niên phải có người giám hộ

Không thay đổi quy tắc bắt buộc `guardianName` / `guardianRelationship` / `guardianPhone` cho bệnh nhân dưới 18 tuổi; `guardianUserId` chỉ **bổ sung** liên kết tài khoản cổng.

---

## 5. Tương thích ngược

| Kịch bản | Hành vi |
|---|---|
| Client cũ không gửi `patientId` (đặt lịch, danh sách, chi tiết) | Không đổi — vẫn dùng hồ sơ của chính mình, không ghi audit từ chối |
| Client cũ gửi `PUT /patients/{id}` không có `guardianUserId` | `guardianUserId` đang lưu được giữ nguyên |
| Client cũ gửi `POST /patients` | Không bị ảnh hưởng (`RegisterPatientCommand` không có `guardianUserId`) |
| `PatientBookAppointmentCommand` 4 tham số | Vẫn biên dịch qua constructor tương thích |

---

## 6. Giới hạn có chủ đích: huỷ / xác nhận / đổi lịch hẹn

`PatientCancelAppointmentService`, `PatientConfirmAppointmentService` và `PatientRescheduleAppointmentService` **không** được sửa. Các thao tác này tiếp tục **chỉ cho phép trên lịch hẹn của chính bệnh nhân đang đăng nhập**.

Hệ quả: người giám hộ có thể **xem và đặt** lịch hẹn cho con nhưng **không thể huỷ / xác nhận / đổi lịch hộ** qua cổng. Đây là giới hạn sản phẩm đã biết, không phải lỗi.

---

## 7. Ghi chú tích hợp cho Frontend

1. Gọi `GET /patient-portal/patients/linked` một lần sau đăng nhập, lưu `patientId` + `self` để dựng bộ chọn hồ sơ. Mục `self = true` là hồ sơ mặc định.
2. Không gửi `patientId` khi người dùng chọn chính mình — giữ đúng hành vi cũ và tránh thêm một lần uỷ quyền.
3. Trường `requiresGuardianLinkReview = true` dùng để hiển thị nhắc nhở rà soát liên kết giám hộ (TC-03).
4. `403 ACCESS_DENIED` nghĩa là hồ sơ đích không nằm trong phạm vi cho phép; cần tải lại danh sách `linked` thay vì thử lại.
5. Không cần thêm quyền hay cấu hình bảo mật nào ở phía client.

---
