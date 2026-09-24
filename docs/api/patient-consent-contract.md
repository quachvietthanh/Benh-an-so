# API Contract: Phiếu đồng ý xử lý dữ liệu cá nhân (Patient Data Processing Consent)

> **User Story:** NCL-15-CN-001, NCL-15-CN-005 (*Rút lại và cập nhật phiếu đồng ý xử lý dữ liệu*)
> **Business Rule:** QTN-24 (*Phải có phiếu đồng ý trước khi xử lý dữ liệu cá nhân*), QTN-19 (*Bảo quản và lưu trữ hồ sơ bệnh án tối thiểu 10 năm*)
> **Acceptance Criteria:** AC-01 (Thu hẹp phạm vi / rút lại đồng ý), AC-02 (Tra cứu lịch sử phiên bản), AC-03 (Ngoại lệ từ chối xóa hồ sơ bệnh án theo QTN-19)

---

## 1. Tổng quan nghiệp vụ

Quy tắc `QTN-24` quy định: Mọi hồ sơ bệnh nhân mới được tạo lập phải ghi nhận sự đồng ý của người bệnh về việc xử lý dữ liệu cá nhân.

1. **Ghi nhận đồng ý khi lập hồ sơ mới (TC-01)**:
   - Endpoint: `POST /api/v1/patients` (Quầy tiếp đón) hoặc `POST /api/v1/auth/patient/register` (Cổng bệnh nhân).
   - Quyền quầy tiếp đón: `PATIENT_CREATE` (chỉ cấp cho `RECEPTIONIST`, `ADMIN`).
   - Payload bắt buộc phải có `consentAgreed: true`. `consentVersion` là tùy chọn (mặc định là `v1.0`).
2. **Chặn lưu khi thiếu đồng ý (TC-02)**:
   - Nếu `consentAgreed` là `false`, `null` hoặc không gửi, hệ thống trả về mã lỗi `400 Bad Request` (`VALIDATION_FAILED` hoặc `PATIENT_CONSENT_REQUIRED`).
3. **Rút lại sự đồng ý (TC-03)**:
   - Endpoint: `PUT /api/v1/patients/{patientId}/consent` hoặc `PUT /api/v1/patients/{patientId}`
   - Quyền: `PATIENT_CONSENT_UPDATE` (chỉ dành riêng cho `RECEPTIONIST`, `ADMIN`). Vai trò Bác sĩ (`DOCTOR`) không được phép can thiệp vào consent hành chính (HTTP 403 Forbidden).
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
- **Path**: `/api/v1/auth/patient/register`
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

### 3.3. Cập nhật hồ sơ bệnh nhân kèm trạng thái đồng ý
- **Method**: `PUT`
- **Path**: `/api/v1/patients/{patientId}`
- **Permission**: `PATIENT_UPDATE` (và `PATIENT_CONSENT_UPDATE` nếu có sửa đổi consent)

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

---

### 3.4. Cập nhật trạng thái Consent qua endpoint chuyên biệt
- **Method**: `PUT`
- **Path**: `/api/v1/patients/{patientId}/consent`
- **Permission**: `PATIENT_CONSENT_UPDATE` (RECEPTIONIST, ADMIN)

#### Request Body
```json
{
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

---

### 3.5. Tra cứu lịch sử phiên bản phiếu đồng ý (Quầy tiếp đón / Nhân viên y tế)
- **Method**: `GET`
- **Path**: `/api/v1/patients/{patientId}/consent-history`
- **Permission**: `PATIENT_READ` hoặc `PATIENT_CONSENT_UPDATE` (RECEPTIONIST, DOCTOR, ADMIN)

#### Response (200 OK)
```json
[
  {
    "id": "f1a2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c",
    "patientId": "e4b2d19f-1234-4567-8901-abcdef123456",
    "versionNumber": 1,
    "versionCode": "v1.0",
    "status": "AGREED",
    "scopes": ["TREATMENT", "COMMUNICATION", "RESEARCH"],
    "consentAgreed": true,
    "consentAgreedAt": "2026-08-27T08:30:00Z",
    "consentWithdrawn": false,
    "consentWithdrawnAt": null,
    "consentWithdrawnReason": null,
    "nonMedicalUseRestricted": false,
    "signerName": "Nguyễn Văn A",
    "createdBy": "00000000-0000-0000-0000-000000000001",
    "createdAt": "2026-08-27T08:30:00Z"
  },
  {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "patientId": "e4b2d19f-1234-4567-8901-abcdef123456",
    "versionNumber": 2,
    "versionCode": "v1.0",
    "status": "PARTIALLY_WITHDRAWN",
    "scopes": ["TREATMENT"],
    "consentAgreed": true,
    "consentAgreedAt": "2026-08-27T08:30:00Z",
    "consentWithdrawn": false,
    "consentWithdrawnAt": null,
    "consentWithdrawnReason": null,
    "nonMedicalUseRestricted": true,
    "signerName": "Nguyễn Văn A",
    "createdBy": "00000000-0000-0000-0000-000000000002",
    "createdAt": "2026-09-15T09:00:00Z"
  }
]
```

---

### 3.6. Yêu cầu xóa dữ liệu cá nhân theo QTN-19 (Quầy tiếp đón)
- **Method**: `POST`
- **Path**: `/api/v1/patients/{patientId}/data-erasure-request`
- **Permission**: `PATIENT_CONSENT_UPDATE` (RECEPTIONIST, ADMIN)
- **Quy tắc nghiệp vụ (QTN-19)**: Hồ sơ bệnh án chuyên môn **không thể xóa** trước hạn do quy định lưu trữ tối thiểu 10 năm theo Luật Khám bệnh, chữa bệnh. Hệ thống tự động thu hồi sự đồng ý và hạn chế sử dụng đối với các mục đích ngoài khám chữa bệnh.

#### Request Body
```json
{
  "reason": "Người bệnh yêu cầu xóa toàn bộ dữ liệu cá nhân"
}
```

#### Response (200 OK)
```json
{
  "patientId": "e4b2d19f-1234-4567-8901-abcdef123456",
  "consentWithdrawn": true,
  "nonMedicalUseRestricted": true,
  "medicalRecordsRetained": true,
  "retentionYears": 10,
  "message": "Hồ sơ bệnh án được lưu trữ tối thiểu 10 năm theo Luật Khám bệnh, chữa bệnh (QTN-19) và không thể xóa trước hạn. Hệ thống đã thu hồi sự đồng ý đối với các mục đích xử lý ngoài khám chữa bệnh."
}
```

---

### 3.7. Cổng bệnh nhân: Tra cứu lịch sử phiếu đồng ý cá nhân
- **Method**: `GET`
- **Path**: `/api/v1/patient-portal/consent/history`
- **Permission**: Người dùng đăng nhập có liên kết hồ sơ bệnh nhân (`ROLE_PATIENT`)

#### Response (200 OK)
Trả về danh sách `PatientConsentHistoryResponse` tương tự endpoint 3.5.

---

### 3.8. Cổng bệnh nhân: Cập nhật phạm vi / rút lại đồng ý
- **Method**: `PUT`
- **Path**: `/api/v1/patient-portal/consent`
- **Permission**: Người dùng đăng nhập có liên kết hồ sơ bệnh nhân (`ROLE_PATIENT`)

#### Request Body (Thu hẹp phạm vi - AC-01)
```json
{
  "scopes": ["TREATMENT"]
}
```

#### Request Body (Rút lại toàn bộ - AC-01)
```json
{
  "consentWithdrawn": true,
  "consentWithdrawnReason": "Tôi không muốn tiếp tục xử lý thông tin"
}
```

#### Response (200 OK)
Trả về `PatientResponse` đã cập nhật.

---

### 3.9. Cổng bệnh nhân: Yêu cầu xóa dữ liệu cá nhân (Áp dụng QTN-19)
- **Method**: `POST`
- **Path**: `/api/v1/patient-portal/consent/data-erasure-request`
- **Permission**: Người dùng đăng nhập có liên kết hồ sơ bệnh nhân (`ROLE_PATIENT`)

#### Request Body
```json
{
  "reason": "Yêu cầu xóa toàn bộ dữ liệu tài khoản"
}
```

#### Response (200 OK)
Trả về `DataErasureResponse` tương tự endpoint 3.6.

---

## 4. Danh mục Giá trị Chuẩn hóa

### 4.1. Phạm vi xử lý dữ liệu (`ConsentScope`)
| Mã phạm vi | Tên hiển thị Tiếng Việt | Bắt buộc | Ghi chú |
| :--- | :--- | :---: | :--- |
| `TREATMENT` | Khám bệnh, chữa bệnh | **Có** | Bắt buộc phải có khi duy trì sự đồng ý. Không thể loại bỏ trừ khi rút lại toàn bộ. |
| `COMMUNICATION` | Thông báo, liên lạc y tế, khảo sát | Không | Có thể thu hẹp / rút bớt. |
| `RESEARCH` | Nghiên cứu khoa học, thống kê y học | Không | Có thể thu hẹp / rút bớt. |

### 4.2. Trạng thái lịch sử phiên bản (`ConsentHistoryStatus`)
| Mã trạng thái | Diễn giải |
| :--- | :--- |
| `AGREED` | Đồng ý toàn bộ các phạm vi tiêu chuẩn |
| `PARTIALLY_WITHDRAWN` | Thu hẹp phạm vi (rút một phần, chỉ duy trì khám chữa bệnh `TREATMENT`) |
| `WITHDRAWN` | Rút lại toàn bộ sự đồng ý |

---

## 5. Kiến trúc Concurrency & Cơ sở dữ liệu

### 5.1. Database Migration (`V90__create_patient_consent_history.sql`)
- Phiên bản migration chính thức sau rebase `origin/develop`: **`V90`** (tránh va chạm với `V87-V89` của develop).
- Bảng lưu trữ: `patient_consent_history`.
- Ràng buộc toàn vẹn: Unique constraint `uk_patient_consent_version (patient_id, version_number)`.
- Foreign key: `fk_patient_consent_patient` liên kết đến bảng `patients(id)` với `ON DELETE CASCADE`.

### 5.2. Concurrency Control (Race-Condition Protection)
- **Aggregate Root Pessimistic Locking**: Mọi use case sinh phiên bản consent mới (`UpdatePatientConsentService`, `RequestPatientDataErasureService`) bắt buộc phải lock bản ghi bệnh nhân thông qua:
  ```java
  Patient patient = patientRepository.findByIdForUpdate(patientId)
          .orElseThrow(() -> new PatientNotFoundException(patientId));
  ```
- **Serialization Guarantee**: Lệnh `SELECT ... FOR UPDATE` trên dòng `patients` tuần tự hóa các yêu cầu cập nhật đồng thời của cùng một bệnh nhân. Nhờ đó, việc tính toán `getNextVersionNumber(patientId)` (`max(version_number) + 1`) luôn an toàn, tránh `DataIntegrityViolationException` do vi phạm unique constraint.

### 5.3. Audit Log Isolation (`REQUIRES_NEW`)
- Thao tác ghi log từ chối xóa dữ liệu và thu hồi đồng ý (`PatientConsentErasureAuditWriter`) được thực thi với:
  ```java
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  ```
- **Rollback Survival Guarantee**: Bản ghi audit log được commit trong transaction riêng biệt độc lập với transaction nghiệp vụ chính. Ngay cả khi transaction nghiệp vụ bị rollback do lỗi hệ thống, bằng chứng yêu cầu và từ chối xóa dữ liệu theo `QTN-19` vẫn được bảo toàn nguyên vẹn trong hệ thống.
- **Malformed JSON Protection**: Mọi payload audit log được serialize chuẩn hóa bằng `ObjectMapper` thay vì nối chuỗi, đảm bảo an toàn tuyệt đối với các ký tự đặc biệt (`"`, `\`) do người dùng nhập.


