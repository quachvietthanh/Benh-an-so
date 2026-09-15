# API Contract: Hồ sơ bệnh nhân trẻ em gắn người giám hộ (Pediatric Patient Guardian Link)

> **User Story:** `NCL-02-CN-008` (Epic `NCL-02` - Quản lý hồ sơ bệnh nhân)  
> **Business Rule:** `QTN-44` (*Hồ sơ người chưa thành niên phải có người giám hộ*), `QTN-24` (*Phải có phiếu đồng ý trước khi xử lý dữ liệu cá nhân*), `QTN-10` (*Mã bệnh nhân duy nhất*)  
> **Acceptance Criteria:** `NCL-02-CN-008-TC-01`, `NCL-02-CN-008-TC-02`, `NCL-02-CN-008-TC-03`, `NCL-02-CN-008-TC-04`  
> **Quyền hạn áp dụng:** `PATIENT_CREATE`, `PATIENT_UPDATE`, `PATIENT_READ` (Lễ tân - `RECEPTIONIST`, Quản trị viên - `ADMIN`)

---

## 1. Tổng quan nghiệp vụ

1. **Ngưỡng tuổi chưa thành niên (Minor Age Threshold)**:
   - Áp dụng mốc **dưới 18 tuổi** theo quy định của Bộ luật Dân sự 2015 và Luật Khám bệnh, chữa bệnh 2023.
   - Bệnh nhân có `Period.between(dateOfBirth, today).getYears() < 18` được xem là chưa thành niên.
2. **Quy tắc bắt buộc Người giám hộ (QTN-44 / TC-01, TC-02)**:
   - Khi tạo mới hồ sơ (`POST /patients`) cho bệnh nhân dưới 18 tuổi, hệ thống bắt buộc phải khai báo đầy đủ bộ ba thông tin người giám hộ:
     - `guardianName`: Họ tên người giám hộ (không được để trống).
     - `guardianRelationship`: Quan hệ với bệnh nhân (không được để trống, tối đa 50 ký tự).
     - `guardianPhone`: Số điện thoại người giám hộ (không được để trống, đúng định dạng di động Việt Nam).
     - `guardianIdentityNumber`: Số CCCD/Định danh của người giám hộ (tùy chọn).
   - Nếu thiếu bất kỳ thông tin nào trong bộ ba trên, hệ thống từ chối lưu với mã lỗi `400 Bad Request` (`VALIDATION_FAILED`).
3. **Quy tắc Phiếu đồng ý đứng tên người giám hộ (TC-03)**:
   - Đối với bệnh nhân chưa thành niên, phiếu đồng ý xử lý dữ liệu cá nhân (`consentAgreed = true`) có hiệu lực pháp lý khi và chỉ khi do người giám hộ đại diện đứng tên.
   - Trường `consentSignerName` tự động ghi nhận theo họ tên của người giám hộ (`guardianName`).
4. **Quy tắc Nhắc nhở và Chuyển tiếp Trưởng thành (TC-04)**:
   - Khi xem hồ sơ (`GET /patients/{id}` hoặc `GET /patients/code/{code}`): Nếu bệnh nhân đã đủ 18 tuổi theo ngày sinh nhưng hồ sơ vẫn còn gắn người giám hộ, response trả về `requiresAdultTransitionPrompt: true`.
   - Khi cập nhật hồ sơ (`PUT /patients/{id}`): Lễ tân có thể gửi cờ `transitionToAdult: true` (hoặc gửi các trường `guardian_*` là null). Hệ thống gỡ bỏ liên kết người giám hộ và cập nhật phiếu đồng ý mới đứng tên chính bệnh nhân (`consentSignerName = fullName`).

---

## 2. API Endpoints

### 2.1. Đăng ký hồ sơ bệnh nhân mới
- **Method:** `POST`
- **Path:** `/patients`
- **Permission:** `PATIENT_CREATE`

#### Request Body - Bệnh nhân trẻ em (< 18 tuổi)
```json
{
  "fullName": "Nguyễn Văn Con",
  "dateOfBirth": "2020-05-10",
  "gender": "MALE",
  "address": "123 Đường ABC, Phường 1, Quận 1, TP.HCM",
  "bloodType": "O_POSITIVE",
  "guardianName": "Nguyễn Văn Cha",
  "guardianRelationship": "Bố",
  "guardianPhone": "0912345678",
  "guardianIdentityNumber": "079090001234",
  "consentAgreed": true,
  "consentVersion": "v1.0"
}
```

#### Response thành công (200 OK / 201 Created)
```json
{
  "id": "e4b2d19f-1234-4567-8901-abcdef123456",
  "patientCode": "BN000001",
  "fullName": "Nguyễn Văn Con",
  "dateOfBirth": "2020-05-10",
  "gender": "MALE",
  "phone": null,
  "email": null,
  "address": "123 Đường ABC, Phường 1, Quận 1, TP.HCM",
  "identityNumber": null,
  "insuranceNumber": null,
  "bloodType": "O_POSITIVE",
  "emergencyContact": null,
  "emergencyRelationship": null,
  "emergencyPhone": null,
  "guardianName": "Nguyễn Văn Cha",
  "guardianRelationship": "Bố",
  "guardianPhone": "0912345678",
  "guardianIdentityNumber": "079090001234",
  "guardianUserId": null,
  "consentSignerName": "Nguyễn Văn Cha",
  "isMinor": true,
  "requiresAdultTransitionPrompt": false,
  "active": true,
  "createdAt": "2026-09-15T08:30:00Z",
  "updatedAt": "2026-09-15T08:30:00Z",
  "consentAgreed": true,
  "consentAgreedAt": "2026-09-15T08:30:00Z",
  "consentVersion": "v1.0",
  "consentWithdrawn": false,
  "consentWithdrawnAt": null,
  "consentWithdrawnReason": null,
  "nonMedicalUseRestricted": false
}
```

#### Error Response khi thiếu người giám hộ (400 Bad Request - TC-02)
```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "guardianName: Hồ sơ bệnh nhân dưới 18 tuổi bắt buộc phải khai báo người giám hộ (QTN-44).",
  "path": "/patients",
  "details": {
    "fields": {
      "guardianName": "Hồ sơ bệnh nhân dưới 18 tuổi bắt buộc phải khai báo người giám hộ (QTN-44)."
    }
  }
}
```

---

### 2.2. Cập nhật hồ sơ bệnh nhân / Chuyển tiếp thành niên
- **Method:** `PUT`
- **Path:** `/patients/{patientId}`
- **Permission:** `PATIENT_UPDATE`

#### Request Body - Chuyển đổi sang tự chịu trách nhiệm khi đủ 18 tuổi (TC-04)
```json
{
  "fullName": "Nguyễn Văn Con",
  "dateOfBirth": "2006-05-10",
  "gender": "MALE",
  "phone": "0901234567",
  "identityNumber": "079206001234",
  "active": true,
  "transitionToAdult": true,
  "consentAgreed": true,
  "consentVersion": "v1.0"
}
```

#### Response sau khi chuyển đổi thành công
```json
{
  "id": "e4b2d19f-1234-4567-8901-abcdef123456",
  "patientCode": "BN000001",
  "fullName": "Nguyễn Văn Con",
  "dateOfBirth": "2006-05-10",
  "gender": "MALE",
  "phone": "0901234567",
  "guardianName": null,
  "guardianRelationship": null,
  "guardianPhone": null,
  "guardianIdentityNumber": null,
  "guardianUserId": null,
  "consentSignerName": "Nguyễn Văn Con",
  "isMinor": false,
  "requiresAdultTransitionPrompt": false,
  "active": true
}
```

#### Error Response khi gỡ người giám hộ không kèm transitionToAdult (400 Bad Request - TC-04)
Đối với bệnh nhân đã đủ 18 tuổi đang có người giám hộ cũ, hệ thống chặn việc xóa bỏ người giám hộ âm thầm qua API update thông thường:
```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "transitionToAdult: Bệnh nhân đã đủ 18 tuổi. Việc gỡ bỏ người giám hộ yêu cầu kích hoạt quy trình chuyển tiếp thành niên (transitionToAdult = true) để ký gia hạn phiếu đồng ý mới (TC-04).",
  "path": "/patients/e4b2d19f-1234-4567-8901-abcdef123456",
  "details": {
    "fields": {
      "transitionToAdult": "Bệnh nhân đã đủ 18 tuổi. Việc gỡ bỏ người giám hộ yêu cầu kích hoạt quy trình chuyển tiếp thành niên (transitionToAdult = true) để ký gia hạn phiếu đồng ý mới (TC-04)."
    }
  }
}
```

---

### 2.3. Quy tắc Phiếu đồng ý & Ẩn danh Dữ liệu (Anonymization Mode)

1. **Ràng buộc Người ký phiếu đồng ý (`consentSignerName`)**:
   - Đối với bệnh nhân chưa thành niên, người ký phiếu đồng ý bắt buộc phải là người giám hộ.
   - Nếu client gửi `consentSignerName` khác với `guardianName`, hệ thống trả về `400 Bad Request` với trường vi phạm `consentSignerName`.
   - Nếu client không gửi `consentSignerName`, hệ thống tự động gán `consentSignerName = guardianName`.

2. **Cơ chế Ẩn danh trình diễn (NCL-15-CN-003)**:
   - Khi chế độ ẩn danh được bật (`/api/v1/anonymization/mode`), nhãn ẩn danh được chuẩn hóa riêng biệt:
     - `fullName`: `BỆNH NHÂN #<mã_bệnh_nhân>`
     - `guardianName`: `GIÁM HỘ #<mã_bệnh_nhân>`
     - `consentSignerName` (đối với trẻ em): `GIÁM HỘ #<mã_bệnh_nhân>`
     - `consentSignerName` (đối với người lớn): `BỆNH NHÂN #<mã_bệnh_nhân>`
   - Khi client cập nhật hồ sơ với dữ liệu đã bị mask (ví dụ `guardianName = "GIÁM HỘ #BN000001"`), hệ thống tự động nhận diện và giữ nguyên giá trị thật trong cơ sở dữ liệu, không ghi đè nhãn ẩn danh.

