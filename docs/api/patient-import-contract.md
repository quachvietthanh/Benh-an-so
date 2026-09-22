# API Contract: Nhập hồ sơ bệnh nhân từ tệp bảng tính (Patient Import from Spreadsheet)

> **User Story:** NCL-02-CN-010  
> **Epic:** NCL-02 (Quản lý hồ sơ bệnh nhân)  
> **Business Rules:** QTN-10 (*Mã bệnh nhân duy nhất*), QTN-03 (*Chỉ dùng dữ liệu mô phỏng*), QTN-31 (*Ghi nhật ký thao tác quản trị*), QTN-44 (*Hồ sơ người chưa thành niên phải có người giám hộ*), QTN-24 (*Phiếu đồng ý xử lý dữ liệu cá nhân*)  
> **Acceptance Criteria:** NCL-02-CN-010-TC-01, NCL-02-CN-010-TC-02, NCL-02-CN-010-TC-03, NCL-02-CN-010-TC-04  

---

## 1. Tổng quan nghiệp vụ

User Story `NCL-02-CN-010` phục vụ số hóa và chuyển đổi dữ liệu hồ sơ bệnh nhân cũ từ sổ sách hoặc bảng tính Excel sang hệ thống phòng khám số mà không phải nhập thủ công từng hồ sơ.

1. **Tải tệp mẫu (Precondition)**:
   - Cung cấp tệp mẫu chuẩn `.xlsx` có tiêu đề rõ ràng và dữ liệu mô phỏng cho người lớn và người chưa thành niên (<18 tuổi có người giám hộ theo `QTN-44`).
2. **Xem trước kiểm tra tệp (Preview - TC-02 & TC-03)**:
   - Kiểm tra từng dòng: họ tên, ngày sinh, giới tính, định dạng SĐT Việt Nam, số CCCD/CMND (9 hoặc 12 số), email, nhóm máu, người liên hệ khẩn cấp và người giám hộ cho trẻ em.
   - Phát hiện nghi trùng: Trùng nội bộ trong tệp hoặc trùng với cơ sở dữ liệu (qua CCCD hoặc Họ tên + Ngày sinh + SĐT).
   - Trả về thống kê số dòng hợp lệ, số dòng lỗi định dạng, số dòng nghi trùng kèm chi tiết từng dòng.
3. **Thực hiện nhập dữ liệu (Import - TC-01, TC-02, TC-03)**:
   - Nhập các dòng hợp lệ, cấp mã bệnh nhân duy nhất (`QTN-10`).
   - Tùy chọn `skipDuplicates=true` (mặc định): Bỏ qua các dòng nghi trùng, ghi nhận vào danh sách lỗi/cảnh báo của tệp nhập để người dùng đối chiếu tra cứu.
   - Lưu vết thay đổi `PatientChangeLog` cho từng bệnh nhân được tạo.
4. **Lưu lịch sử & Kiểm toán (TC-04 & QTN-31)**:
   - Lưu `PatientImportLog` và danh sách chi tiết lỗi từng dòng `PatientImportRowError`.
   - Ghi bản ghi kiểm toán quản trị `AuditLog` với `ActionType = IMPORT` và `ResourceType = PATIENT_IMPORT`.
   - Cung cấp API tra cứu lịch sử các lần nhập phân trang và xem chi tiết theo mã lần nhập.

---

## 2. Phân quyền truy cập (RBAC)

| Vai trò | Quyền hạn | Quyền truy cập API Import |
| :--- | :--- | :---: |
| **Quản trị viên (ADMIN)** | Toàn quyền quản trị hệ thống và cấu hình dữ liệu ban đầu | **Cho phép** (`PATIENT_IMPORT`) |
| **Lễ tân (RECEPTIONIST)** | Đón tiếp, đăng ký và cập nhật hồ sơ bệnh nhân hành chính | **Cho phép** (`PATIENT_IMPORT`) |
| **Bác sĩ (DOCTOR)** | Khám chữa bệnh chuyên môn | **Chặn (403 Forbidden)** |
| **Dược sĩ (PHARMACIST)** | Quản lý kho dược và cấp phát thuốc | **Chặn (403 Forbidden)** |

---

## 3. Cấu trúc các cột trong Tệp bảng tính mẫu (.xlsx)

| STT | Cột tiêu đề | Bắt buộc | Định dạng / Quy tắc |
| :---: | :--- | :---: | :--- |
| 0 | Họ và tên (*) | Có | Chuỗi tối đa 100 ký tự |
| 1 | Ngày sinh (*) (dd/MM/yyyy) | Có | Định dạng `dd/MM/yyyy` hoặc `yyyy-MM-dd`, không ở tương lai |
| 2 | Giới tính (*) (Nam/Nữ) | Có | `Nam`, `Nữ`, `Khác` |
| 3 | Số điện thoại | Không | Đầu số di động Việt Nam (03x, 05x, 07x, 08x, 09x) |
| 4 | Số CCCD/CMND | Không | Gồm 9 hoặc 12 chữ số |
| 5 | Số thẻ BHYT | Không | Chuỗi mã BHYT |
| 6 | Địa chỉ | Không | Địa chỉ cư trú |
| 7 | Email | Không | Định dạng email hợp lệ |
| 8 | Nhóm máu (A/B/AB/O) | Không | A, B, AB, O, A+, B+, etc. |
| 9 | Người liên hệ khẩn cấp | Không* | Bắt buộc đủ cả 3 thông tin nếu khai báo (Tên, Quan hệ, SĐT) |
| 10 | Quan hệ người liên hệ | Không* | Mối quan hệ |
| 11 | SĐT người liên hệ | Không* | SĐT di động hợp lệ |
| 12 | Tên người giám hộ (<18t bắt buộc) | Điều kiện | **Bắt buộc nếu bệnh nhân < 18 tuổi** (`QTN-44`) |
| 13 | Quan hệ người giám hộ (<18t bắt buộc) | Điều kiện | **Bắt buộc nếu bệnh nhân < 18 tuổi** (`QTN-44`) |
| 14 | SĐT người giám hộ (<18t bắt buộc) | Điều kiện | **Bắt buộc nếu bệnh nhân < 18 tuổi** (`QTN-44`) |

---

## 4. Đặc tả API Endpoints

### 4.1. Tải tệp bảng tính mẫu
* **Method**: `GET`
* **Path**: `/api/v1/patients/import/template`
* **Permission**: `PATIENT_IMPORT`
* **Response**: Binary Excel file (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)
* **Headers**: `Content-Disposition: attachment; filename="mau_danh_sach_benh_nhan.xlsx"`

---

### 4.2. Xem trước kiểm tra tệp (Preview)
* **Method**: `POST`
* **Path**: `/api/v1/patients/import/preview`
* **Permission**: `PATIENT_IMPORT`
* **Content-Type**: `multipart/form-data`
* **Request Params**:
  * `file` (MultipartFile): Tệp bảng tính `.xlsx` hoặc `.xls` (tối đa 10MB, tối đa 5000 dòng).

#### Response (200 OK)
```json
{
  "fileName": "danh_sach_benh_nhan_cu.xlsx",
  "totalRows": 15,
  "validCount": 12,
  "errorCount": 2,
  "duplicateCount": 1,
  "errors": [
    {
      "rowNumber": 4,
      "errorField": "Họ và tên",
      "errorMessage": "Họ và tên không được để trống.",
      "rawData": "RawPatientRowDto(rowNumber=4, ...)"
    },
    {
      "rowNumber": 9,
      "errorField": "Tên người giám hộ",
      "errorMessage": "Bệnh nhân dưới 18 tuổi bắt buộc phải khai báo tên người giám hộ (QTN-44).",
      "rawData": "RawPatientRowDto(rowNumber=9, ...)"
    }
  ],
  "suspectedDuplicates": [
    {
      "rowNumber": 7,
      "fullName": "Trần Văn An",
      "dateOfBirth": "1988-05-15",
      "phone": "0901234567",
      "identityNumber": "001088012345",
      "matchedExistingPatientId": "c1000000-0000-0000-0000-000000000001",
      "matchedExistingPatientCode": "BN-000045",
      "matchedExistingFullName": "Trần Văn An",
      "duplicateReason": "Hồ sơ trùng khớp Họ tên, Ngày sinh và Số điện thoại với bệnh nhân BN-000045 đã có trong hệ thống."
    }
  ]
}
```

---

### 4.3. Thực hiện nhập hồ sơ từ tệp bảng tính (Import)
* **Method**: `POST`
* **Path**: `/api/v1/patients/import`
* **Permission**: `PATIENT_IMPORT`
* **Content-Type**: `multipart/form-data`
* **Request Params**:
  * `file` (MultipartFile): Tệp bảng tính Excel.
  * `skipDuplicates` (boolean, default `true`): `true` để bỏ qua các dòng nghi trùng; `false` để chặn và ghi lỗi cho các dòng trùng.

#### Response (200 OK)
```json
{
  "importLogId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "fileName": "danh_sach_benh_nhan_cu.xlsx",
  "totalRows": 15,
  "successCount": 12,
  "errorCount": 3,
  "duplicateCount": 1,
  "createdPatientCodes": [
    "BN-000101",
    "BN-000102",
    "BN-000103"
  ],
  "errors": [
    {
      "rowNumber": 4,
      "errorField": "Họ và tên",
      "errorMessage": "Họ và tên không được để trống.",
      "rawData": "RawPatientRowDto(rowNumber=4, ...)"
    },
    {
      "rowNumber": 7,
      "errorField": "Thông tin định danh",
      "errorMessage": "Dòng bị bỏ qua do trùng lặp: Hồ sơ trùng khớp Họ tên, Ngày sinh và Số điện thoại với bệnh nhân BN-000045 đã có trong hệ thống.",
      "rawData": "Họ tên: Trần Văn An, SĐT: 0901234567"
    }
  ]
}
```

#### Response (409 Conflict)
Khi hệ thống đang có một tiến trình nhập hồ sơ khác đang diễn ra:
```json
{
  "timestamp": "2026-09-22T08:50:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "CONCURRENT_IMPORT_IN_PROGRESS",
  "message": "Hệ thống đang thực hiện một tiến trình nhập hồ sơ khác, vui lòng thử lại sau."
}
```

---

### 4.4. Tra cứu danh sách lịch sử nhập liệu
* **Method**: `GET`
* **Path**: `/api/v1/patients/import-logs?page=0&size=20&sort=createdAt,desc`
* **Permission**: `PATIENT_IMPORT`

#### Response (200 OK)
```json
{
  "content": [
    {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "fileName": "danh_sach_benh_nhan_cu.xlsx",
      "fileSize": 18450,
      "totalRows": 15,
      "successRows": 12,
      "errorRows": 3,
      "duplicateRows": 1,
      "status": "PARTIAL",
      "importedBy": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "importedByName": "Quản trị viên Hệ thống",
      "createdAt": "2026-09-21T04:15:30Z",
      "errors": []
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

### 4.5. Xem chi tiết một lần nhập liệu
* **Method**: `GET`
* **Path**: `/api/v1/patients/import-logs/{id}`
* **Permission**: `PATIENT_IMPORT`

#### Response (200 OK)
Trả về chi tiết `PatientImportLogResponse` kèm đầy đủ mảng `errors` chứa chi tiết từng dòng lỗi của lần nhập đó.

#### Response (404 Not Found)
Khi ID lần nhập không tồn tại:
```json
{
  "timestamp": "2026-09-21T04:15:30Z",
  "status": 404,
  "error": "Not Found",
  "code": "PATIENT_IMPORT_LOG_NOT_FOUND",
  "message": "Không tìm thấy nhật ký nhập liệu với mã: <id>"
}
```
