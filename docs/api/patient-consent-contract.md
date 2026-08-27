# API Contract: Phiếu đồng ý xử lý dữ liệu cá nhân (Patient Data Processing Consent)

> **User Story:** NCL-15-CN-001
> **Business Rule:** QTN-24 (*Phải có phiếu đồng ý trước khi xử lý dữ liệu cá nhân*)
> **Acceptance Criteria:** NCL-15-CN-001-TC-01, NCL-15-CN-001-TC-02, NCL-15-CN-001-TC-03, NCL-15-CN-001-TC-04

---

## 1. Tổng quan nghiệp vụ

Quy tắc `QTN-24` quy định: Mọi hồ sơ bệnh nhân mới được tạo lập phải ghi nhận sự đồng ý của người bệnh về việc xử lý dữ liệu cá nhân.

1. **Ghi nhận đồng ý khi lập hồ sơ mới (TC-01)**:
   - Endpoint: `POST /api/v1/patients` (Quầy tiếp đón) hoặc `POST /api/v1/auth/patients/register` (Cổng bệnh nhân).
   - Quyền quầy tiếp đón: `PATIENT_CREATE` (chỉ cấp cho `RECEPTIONIST`, `ADMIN`).
   - Payload bắt buộc phải có `consentAgreed: true`.
2. **Chặn lưu khi thiếu đồng ý (TC-02)**:
   - Nếu `consentAgreed` là `false`, `null` hoặc không gửi, hệ thống trả về mã lỗi `400 Bad Request` (`VALIDATION_FAILED` hoặc `PATIENT_CONSENT_REQUIRED`).
3. **Rút lại sự đồng ý (TC-03)**:
   - Endpoint: `PUT /api/v1/patients/{patientId}`
   - Quyền: `PATIENT_CONSENT_UPDATE` (chỉ dành riêng cho `RECEPTIONIST`, `ADMIN`). Vai trò Bác sĩ (`DOCTOR`) không được phép can thiệp vào consent hành chính (HTTP 403).
   - Khi `consentWithdrawn: true`, hệ thống tự động cập nhật:
     - `consent_withdrawn = true`
     - `consent_withdrawn_at = <thời điểm>`
     - `consent_withdrawn_reason = "<lý do>"`
     - `non_medical_use_restricted = true` (ngừng dùng cho mục đích ngoài khám chữa bệnh, bảo đảm tính liên tục khám chữa bệnh với `active = true`).
4. **Lưu lịch sử & Audit Trail (TC-04)**:
   - Mọi thao tác tạo mới hoặc cập nhật trạng thái đồng ý đều được ghi nhận vào `patient_change_logs` và `audit_logs` kèm định danh người thực hiện (`changedBy`), thời điểm (`timestamp`), và chi tiết thay đổi.

---

## 2. Nội dung Phiếu đồng ý mẫu (Mã phiên bản v1.0 - CV-01)

- **Mã phiên bản**: `v1.0`
- **Tên điều khoản**: Phiếu Đồng Ý Xử Lý Dữ Liệu Cá Nhân Trong Hoạt Động Khám Chữa Bệnh
- **Nội dung tóm tắt**:
  1. Tôi đồng ý cung cấp thông tin cá nhân và dữ liệu sức khỏe phục vụ công tác quản lý hồ sơ y tế, chẩn đoán, điều trị và chăm sóc sức khỏe.
  2. Dữ liệu y tế phục vụ khám chữa bệnh sẽ được lưu trữ và bảo mật theo quy định của Luật Khám bệnh, chữa bệnh.
  3. Tôi hiểu rằng có quyền rút lại sự đồng ý đối với các mục đích phi y tế (nhận thông báo tiếp thị, khảo sát dịch vụ, nghiên cứu thống kê không bắt buộc) bất kỳ lúc nào tại quầy tiếp đón.

---

## 3. API Endpoints

### 3.1. Đăng ký bệnh nhân mới tại quầy tiếp đón
- **Method**: `POST`
- **Path**: `/api/v1/patients`
- **Permission**: `PATIENT_CREATE` (RECEPTIONIST, ADMIN)

#### Request Body
```json
{
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "phone": "0901234567",
  "email": "nguyenvana@example.com",
  "address": "123 Đường ABC, Phường 1, Quận 1, TP.HCM",
  "identityNumber": "079090001234",
  "insuranceNumber": "DN4790123456789",
  "bloodType": "O_POSITIVE",
  "emergencyContact": "Nguyễn Thị B",
  "emergencyPhone": "0909998877",
  "consentAgreed": true,
  "consentVersion": "v1.0"
}
```

#### Response (200 OK / 201 Created)
```json
{
  "id": "e4b2d19f-1234-4567-8901-abcdef123456",
  "patientCode": "BN000001",
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "phone": "0901234567",
  "email": "nguyenvana@example.com",
  "address": "123 Đường ABC, Phường 1, Quận 1, TP.HCM",
  "identityNumber": "079090001234",
  "insuranceNumber": "DN4790123456789",
  "bloodType": "O_POSITIVE",
  "emergencyContact": "Nguyễn Thị B",
  "emergencyPhone": "0909998877",
  "active": true,
  "createdAt": "2026-08-27T08:30:00Z",
  "updatedAt": "2026-08-27T08:30:00Z",
  "consentAgreed": true,
  "consentAgreedAt": "2026-08-27T08:30:00Z",
  "consentVersion": "v1.0",
  "consentWithdrawn": false,
  "consentWithdrawnAt": null,
  "consentWithdrawnReason": null,
  "nonMedicalUseRestricted": false
}
```

#### Error Response khi thiếu đồng ý (400 Bad Request)
```json
{
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Validation failed.",
  "path": "/api/v1/patients",
  "details": {
    "fields": {
      "consentAgreed": "Phải ghi nhận sự đồng ý của người bệnh trước khi lập hồ sơ mới (QTN-24)."
    }
  }
}
```

---

### 3.2. Đăng ký tài khoản qua Cổng bệnh nhân (Patient Portal)
- **Method**: `POST`
- **Path**: `/api/v1/auth/patients/register`
- **Permission**: Public

#### Request Body
```json
{
  "phone": "0901234567",
  "password": "Password123@",
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "identityNumber": "079090001234",
  "email": "nguyenvana@example.com",
  "consentAgreed": true,
  "consentVersion": "v1.0"
}
```

---

### 3.3. Rút lại sự đồng ý xử lý dữ liệu
- **Method**: `PUT`
- **Path**: `/api/v1/patients/{patientId}`
- **Permission**: `PATIENT_CONSENT_UPDATE` (RECEPTIONIST, ADMIN)

#### Request Body
```json
{
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "phone": "0901234567",
  "email": "nguyenvana@example.com",
  "address": "123 Đường ABC, Phường 1, Quận 1, TP.HCM",
  "identityNumber": "079090001234",
  "insuranceNumber": "DN4790123456789",
  "bloodType": "O_POSITIVE",
  "emergencyContact": "Nguyễn Thị B",
  "emergencyPhone": "0909998877",
  "active": true,
  "consentWithdrawn": true,
  "consentWithdrawnReason": "Người bệnh yêu cầu ngừng nhận thông báo tiếp thị và nghiên cứu"
}
```

#### Response (200 OK)
```json
{
  "id": "e4b2d19f-1234-4567-8901-abcdef123456",
  "patientCode": "BN000001",
  "fullName": "Nguyễn Văn A",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "phone": "0901234567",
  "email": "nguyenvana@example.com",
  "address": "123 Đường ABC, Phường 1, Quận 1, TP.HCM",
  "identityNumber": "079090001234",
  "insuranceNumber": "DN4790123456789",
  "bloodType": "O_POSITIVE",
  "emergencyContact": "Nguyễn Thị B",
  "emergencyPhone": "0909998877",
  "active": true,
  "createdAt": "2026-08-27T08:30:00Z",
  "updatedAt": "2026-08-27T08:35:00Z",
  "consentAgreed": true,
  "consentAgreedAt": "2026-08-27T08:30:00Z",
  "consentVersion": "v1.0",
  "consentWithdrawn": true,
  "consentWithdrawnAt": "2026-08-27T08:35:00Z",
  "consentWithdrawnReason": "Người bệnh yêu cầu ngừng nhận thông báo tiếp thị và nghiên cứu",
  "nonMedicalUseRestricted": true
}
```
