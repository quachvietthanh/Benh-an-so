# Security Review — NCL-14-CN-010 (Người giám hộ đặt lịch khám cho bệnh nhân phụ thuộc)

> **Phạm vi:** backend CV-02 (triển khai) + CV-04 (đánh giá bảo mật)
> **Liên quan:** `QTN-23` (cách ly dữ liệu bệnh nhân), `QTN-44` (người giám hộ), `NCL-14-CN-002`
> **Tài liệu hợp đồng:** `docs/api/patient-portal-family-appointment-contract.md`

---

## 1. Ranh giới tin cậy (trust boundary)

| Vùng | Đáng tin? | Ghi chú |
|---|---|---|
| JWT / `CurrentUserPort.getCurrentUserId()` | ✅ Tin cậy | Danh tính lấy từ token đã xác thực; đây là **nguồn thẩm quyền duy nhất** |
| `patients.guardian_user_id` (CSDL) | ✅ Tin cậy | Dữ liệu phía server đã được nhân viên có `PATIENT_UPDATE` thiết lập |
| `patients.user_id` (CSDL) | ✅ Tin cậy | Xác định hồ sơ nào thuộc tài khoản nào |
| `patientId` trong query string / request body | ❌ **KHÔNG tin cậy** | Chỉ là *yêu cầu phạm vi*, phải được uỷ quyền lại phía server |
| `guardianUserId` trong request body | ❌ **KHÔNG tin cậy** | Chỉ là *đề xuất gán*; chỉ nhân viên có `PATIENT_UPDATE` mới gọi được |
| `userId`, `accountId`, `role`, cờ “ownership” do client gửi | ❌ **KHÔNG tin cậy** | Hệ thống không đọc các trường này để uỷ quyền |

---

## 2. `patientId` không phải là bằng chứng thẩm quyền

Đây là quy tắc bất biến trung tâm của NCL-14-CN-010:

> Một tài khoản cổng đã xác thực **chỉ** được đặt lịch hoặc xem dữ liệu lịch hẹn của một bệnh nhân phụ thuộc khi `patients.guardian_user_id` của bệnh nhân đó **trỏ đúng** vào danh tính tài khoản đang đăng nhập.

Cơ chế thực thi nằm ở một chỗ duy nhất: `PatientAccessGuard.requirePatientAccess(...)`.

```java
Patient own = patientRepository.findByUserId(userId).orElse(null);
if (own != null && own.getId().equals(targetPatientId)) {
    return own;
}

// Tra cứu hồ sơ phụ thuộc CHỈ dựa trên danh tính phía server; mọi tuyên bố
// về quan hệ giám hộ do client gửi đều bị bỏ qua.
Patient dependent = patientRepository
        .findByGuardianUserIdAndId(userId, targetPatientId)
        .orElse(null);

if (dependent != null) {
    return dependent;
}

denyPatientAccess(targetPatientId, resourceType, resourceId);
throw new AccessDeniedException(FAMILY_SCOPE_DENIED_MESSAGE);
```

`findByGuardianUserIdAndId(userId, targetPatientId)` là một truy vấn có **hai** điều kiện: `guardian_user_id = <danh tính đã xác thực>` **và** `id = <bệnh nhân được yêu cầu>`. Vì tham số đầu tiên luôn đến từ `CurrentUserPort`, không có giá trị `patientId` nào do client chọn có thể mở rộng phạm vi.

---

## 3. Phân biệt “người thực hiện” và “bệnh nhân đích”

Trong luồng đặt lịch, hai khái niệm này được tách bạch rõ ràng:

| Khái niệm | Nguồn | Dùng để |
|---|---|---|
| Người thực hiện (`userId`) | `CurrentUserPort` | `AuditLog.userId`, trường audit `bookedByUserId`, `Appointment.createdBy` |
| Bệnh nhân đích (`patientId`) | Hồ sơ của chính mình hoặc hồ sơ phụ thuộc đã uỷ quyền | `Appointment.patientId` |
| Cờ hộ đặt | `!userId.equals(patient.getUserId())` (suy ra từ CSDL) | Audit `bookingOnBehalfOfDependent` |

Bệnh nhân đích **không bao giờ** được lấy từ `userId` do client gửi.

---

## 4. Uỷ quyền gán người giám hộ

### 4.1. Cơ chế

`guardianUserId` được thêm vào `PUT /patients/{patientId}` — endpoint đã được bảo vệ bằng `@RequirePermission("PATIENT_UPDATE")` (`PatientController.java`). Không thêm endpoint mới, không thêm quyền mới.

`RequirePermissionAspect` ghi audit `ACCESS_DENIED` / `PERMISSION` rồi trả 403 khi thiếu quyền.

### 4.2. Vì sao tài khoản cổng không thể tự gán người giám hộ

| Bằng chứng | Nội dung |
|---|---|
| `V27__seed_patient_portal_role.sql` | Vai trò `PATIENT` (id `77777777-…`) có **0 permission grant** |
| `V2__seed_auth_data.sql` | `PATIENT_UPDATE` chỉ thuộc ADMIN / DOCTOR / RECEPTIONIST |
| `SecurityConfig` | `/patients/**` = `authenticated()`, nhưng uỷ quyền thật nằm ở method security |

⇒ `guardianUserId` **không thể** bị gán bởi tài khoản cổng: quyền cần thiết không tồn tại ở vai trò đó. Đây là bảo vệ theo cấu trúc, không phải theo kiểm tra tên trường.

### 4.3. Kiểm tra phía service (defense in depth)

Kể cả khi một actor nội bộ gọi service, `UpdatePatientService.validateGuardianUser(...)` vẫn chặn:

1. Tự gán chính mình (`guardianUserId` = `patients.user_id` của bệnh nhân).
2. Tài khoản không tồn tại.
3. Tài khoản bị vô hiệu hoá.
4. Tài khoản không thuộc vai trò `PATIENT`.
5. Liên kết giám hộ vòng mức 1.
6. Bỏ trống `guardianUserId` ⇒ **giữ nguyên** giá trị cũ.
7. `transitionToAdult = true` ⇒ xoá liên kết.

---

## 5. Bảo vệ IDOR

### 5.1. Trên danh sách và đặt lịch

Phạm vi được xác định bằng `requirePatientAccess`, trước mọi truy vấn nghiệp vụ. Không có nhánh nào đọc dữ liệu lịch hẹn trước khi uỷ quyền.

### 5.2. Trên chi tiết lịch hẹn — hai lớp

`GetPatientPortalAppointmentDetailService` không chỉ uỷ quyền cho `patientId` được yêu cầu mà còn **bắt buộc** lịch hẹn phải thuộc đúng phạm vi đó:

```java
Patient authorised = patientId == null
        ? patientAccessGuard.requirePatientOwnership(
                appointment.getPatientId(), ResourceType.APPOINTMENT, appointment.getId())
        : patientAccessGuard.requirePatientAccess(
                patientId, ResourceType.APPOINTMENT, appointment.getId());

if (!appointment.getPatientId().equals(authorised.getId())) {
    patientAccessGuard.denyPatientAccess(
            appointment.getPatientId(), ResourceType.APPOINTMENT, appointment.getId());
    throw new AccessDeniedException(SCOPE_MISMATCH_MESSAGE);
}
```

Nhờ lớp thứ hai, việc trộn phạm vi (gửi `patientId` của người phụ thuộc nhưng `appointmentId` của bệnh nhân thứ ba, hoặc ngược lại) **luôn** bị từ chối và ghi audit.

### 5.3. `requirePatientOwnership` không bị thay đổi

Phương thức cũ được giữ **nguyên văn**. Mọi tài nguyên cổng hiện có vẫn chỉ-dành-cho-chính-mình. Phạm vi gia đình là API **bổ sung** (`requirePatientAccess`), không phải nới lỏng API cũ. Test `requirePatientOwnershipStillDeniesALinkedDependent` chứng minh điều này.

---

## 6. Audit

| Sự kiện | Action | Resource | Chi tiết |
|---|---|---|---|
| Đặt lịch cho người phụ thuộc | `CREATE` | `APPOINTMENT` | `patientId`, `doctorId`, `appointmentTime`, `channel`, `bookedAt`, `bookedByUserId`, `bookingOnBehalfOfDependent` |
| Truy cập ngoài phạm vi | `ACCESS_DENIED` | `PATIENT` / `APPOINTMENT` | `action`, `targetPatientId`, `deniedAt` |
| Gán / xoá `guardianUserId` | `UPDATE` | `PATIENT` | Trường `guardianUserId` trong danh sách thay đổi |
| Thiếu `PATIENT_UPDATE` | `ACCESS_DENIED` | — | Do `RequirePermissionAspect` ghi |

`PatientAccessDeniedAuditWriter` ghi bằng giao dịch **`REQUIRES_NEW`**, nên bản ghi từ chối **tồn tại độc lập** với giao dịch bị rollback do `AccessDeniedException`.

---

## 7. Quy tắc chống liên kết vòng

**Quy tắc:** từ chối khi hồ sơ của người giám hộ tiềm năng có `guardian_user_id` trỏ ngược lại chính bệnh nhân đang cập nhật.

```java
Patient guardianProfile = patientRepository.findByUserId(guardianUserId).orElse(null);
if (guardianProfile != null
        && guardianProfile.getGuardianUserId() != null
        && guardianProfile.getGuardianUserId().equals(patient.getId())) {
    throw new ValidationException("guardianUserId",
            "Không thể tạo liên kết giám hộ vòng giữa hai hồ sơ.");
}
```

| Chuỗi | Kết quả |
|---|---|
| `A → B` (B là con của A) | Cho phép |
| `A ↔ B` | **Chặn** |
| `A → B → C` (chặng trực tiếp liên quan A và B) | **Chặn** |
| Vòng ≥ 3 nút (ví dụ `A → B → C → A`) | **Không** phát hiện đầy đủ — hạn chế có chủ đích, xem §9 |

Đánh giá rủi ro: mọi lượt ghi đều đi qua `PUT /patients/{patientId}` với `PATIENT_UPDATE`, là thao tác chỉ dành cho nhân viên; vòng sâu không tạo ra lỗ hổng truy cập dữ liệu (phạm vi vẫn chỉ mở rộng cho tài khoản trỏ trực tiếp), mà chỉ có thể tạo dữ liệu liên kết không mong muốn.

---
## 8. Rò rỉ sự tồn tại của bệnh nhân (existence oracle)

Thông báo từ chối của phạm vi gia đình là **hằng số**, không phụ thuộc việc bệnh nhân có tồn tại hay không:

```java
private static final String FAMILY_SCOPE_DENIED_MESSAGE =
        "Patient may only access their own data or the data of a patient they are guardian of.";
```

Vì vậy kẻ tấn công không thể phân biệt “bệnh nhân không tồn tại” với “bệnh nhân tồn tại nhưng không thuộc phạm vi”. `patients.guardian_user_id` chỉ được ghi trong audit phía server.

Lưu ý còn lại: `GET /patient-portal/appointments/{id}` trả `404 APPOINTMENT_NOT_FOUND` cho lịch hẹn không tồn tại và `403 ACCESS_DENIED` cho lịch hẹn ngoài phạm vi — nghĩa là một tài khoản cổng hợp lệ có thể phân biệt hai trường hợp này. Đây là hành vi **có sẵn từ trước** (`NCL-14-CN-004`) và không bị thay đổi bởi thay đổi này.

---

## 9. Rủi ro còn lại

| # | Mức | Rủi ro | Giảm thiểu hiện có / Đề xuất |
|---|---|---|---|
| R-01 | **P2** | Vòng giám hộ ≥ 3 nút không bị phát hiện đầy đủ | Chỉ nhân viên có `PATIENT_UPDATE` ghi được; vòng sâu không mở rộng phạm vi truy cập. Đề xuất: rà soát định kỳ / đệ quy giới hạn số bước. |
| R-02 | **P2** | Huỷ / xác nhận / đổi lịch hẹn **không** hỗ trợ người giám hộ | Giới hạn sản phẩm có chủ đích; đã ghi tài liệu. Cần user story riêng nếu muốn mở. |
| R-03 | **P2** | Liên kết giám hộ cũ vẫn còn sau khi bệnh nhân đủ 18 tuổi | Cờ `requiresGuardianLinkReview` + quy trình `transitionToAdult` đã có; phần **thông báo chủ động chưa triển khai** (khoảng trống nghiệp vụ). |
| R-04 | **P3** | Phân biệt 404/403 trên chi tiết lịch hẹn | Hành vi có sẵn từ `NCL-14-CN-004`; không đổi trong thay đổi này. |
| R-05 | **P3** | Không giới hạn số hồ sơ phụ thuộc trả về trong `/linked` | Dữ liệu tối thiểu, không PHI; có thể bổ sung phân trang nếu số hộ gia đình lớn. |
| R-06 | **P3** | Bệnh nhân phụ thuộc có vai trò `PATIENT` nhưng chưa từng có hồ sơ hợp lệ | Không ảnh hưởng uỷ quyền: phạm vi vẫn dựa trên `guardian_user_id`. |

---

## 10. Kết luận

- Nguồn thẩm quyền duy nhất: danh tính đã xác thực + `patients.guardian_user_id`.
- `patientId` do client gửi **không bao giờ** đủ để uỷ quyền.
- `requirePatientOwnership` không thay đổi ⇒ không có hồi quy trên các tài nguyên cổng hiện có.
- Mọi từ chối đều ghi audit `ACCESS_DENIED` trong giao dịch độc lập.
- Gán người giám hộ chỉ khả thi với `PATIENT_UPDATE`; vai trò `PATIENT` không có quyền này.
- Khoảng trống đã biết: phần thông báo của TC-03 và giới hạn huỷ/xác nhận/đổi lịch hẹn.
