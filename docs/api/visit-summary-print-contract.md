# Visit Summary Printing API Contract

> Module: **NCL-04 - Khám bệnh và bệnh án điện tử**
>
> User Story: **NCL-04-CN-011 - In phiếu tóm tắt lượt khám cho bệnh nhân**
>
> Base URL: `http://localhost:8080/api/v1/visits`
>
> Auth: Bearer Token (JWT)
>
> Status: **Proposed Contract & Implementation**

---

## 1. Scope & Business Requirements

Tài liệu này đặc tả API contract cho tính năng dựng và in phiếu tóm tắt lượt khám cho bệnh nhân (`NCL-04-CN-011`).

### 1.1 Mục tiêu nghiệp vụ
- Người bệnh sau khi hoàn tất khám bệnh rời phòng khám với chứng từ chuyên môn chính thức tóm tắt chẩn đoán, các chỉ định cận lâm sàng đã thực hiện, lời dặn của bác sĩ và mốc hẹn tái khám.
- Dựng phiếu chuẩn A4 dạng PDF và cung cấp API xem trước dữ liệu (JSON).
- Ràng buộc trạng thái: Bệnh án của lượt khám **phải được ký** (`SIGNED`, `LOCKED`, hoặc `ARCHIVED`) mới được phép xem/in phiếu tóm tắt (`QTN-17`, `TC-02`).
- Mỗi lần in/kết xuất tệp PDF đều được ghi nhận vào nhật ký kiểm toán hệ thống (`QTN-02`, `TC-03`).
- Phân quyền: Cấp quyền cho Bác sĩ (`DOCTOR`), Lễ tân (`RECEPTIONIST`), Quản trị viên (`ADMIN`), Quản lý phòng khám (`MANAGER`).

---

## 2. Roles & Permissions

| Role | Quyền hạn | Ghi chú |
| :--- | :--- | :--- |
| **DOCTOR** | `VISIT_SUMMARY_PRINT` | Bác sĩ in phiếu tóm tắt trực tiếp tại phòng khám cho người bệnh. |
| **RECEPTIONIST** | `VISIT_SUMMARY_PRINT` | Lễ tân in phiếu tóm tắt tại quầy tiếp đón/thu ngân khi hoàn tất lượt khám đã ký. |
| **ADMIN** | `VISIT_SUMMARY_PRINT` | Toàn quyền xem và xuất tệp tóm tắt. |
| **MANAGER** | `VISIT_SUMMARY_PRINT` | Quản lý phòng khám xem và xuất tệp tóm tắt. |
| **PHARMACIST** | *Không có quyền* | Trả về HTTP 403 Forbidden. |
| **PATIENT** | *Không có quyền* | Trả về HTTP 403 Forbidden (bệnh nhân dùng cổng tra cứu riêng). |

---

## 3. Endpoints Specification

### 3.1 Xem trước thông tin phiếu tóm tắt lượt khám (Preview)

- **Endpoint**: `GET /api/v1/visits/{visitId}/summary`
- **Method**: `GET`
- **Permission**: `VISIT_SUMMARY_PRINT`
- **Path Parameter**:
  - `visitId` (UUID, required): Mã định danh duy nhất của lượt khám.
- **Success Response**: `200 OK`
```json
{
  "visitId": "d0000000-0000-0000-0000-000000000001",
  "visitCode": "KB-20260820-0001",
  "visitAt": "2026-08-20T08:30:00Z",
  "clinic": {
    "name": "Phòng khám Đa khoa Hoàn Mỹ",
    "address": "123 Hoàng Văn Thụ, Tân Bình, TP.HCM",
    "phone": "02838445566"
  },
  "patient": {
    "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb001",
    "patientCode": "BN-2026-0001",
    "fullName": "Nguyễn Văn A",
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "phone": "0901234567",
    "identityNumber": "079090001234"
  },
  "doctor": {
    "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
    "fullName": "BS. Trần Văn B"
  },
  "medicalRecord": {
    "id": "e0000000-0000-0000-0000-000000000001",
    "status": "SIGNED",
    "signedAt": "2026-08-20T09:15:00Z",
    "signedBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
    "signedByName": "BS. Trần Văn B"
  },
  "diagnoses": [
    {
      "code": "J00",
      "name": "Viêm mũi họng cấp (cảm thường)",
      "isPrimary": true
    }
  ],
  "clinicalOrders": [
    {
      "orderCode": "ORD-20260820-0001",
      "serviceCode": "XQ01",
      "serviceName": "Chụp X-quang phổi thẳng",
      "instruction": "Tư thế đứng thẳng",
      "status": "COMPLETED"
    }
  ],
  "doctorInstructions": "Nghỉ ngơi, uống nhiều nước ấm, tránh gió lạnh.",
  "treatmentPlan": "Điều trị ngoại trú 5 ngày.",
  "revisitDate": "2026-08-27",
  "printHistory": [
    {
      "printedBy": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
      "printedByName": "BS. Trần Văn B",
      "printedAt": "2026-08-20T09:20:00Z",
      "detail": "In phiếu tóm tắt lượt khám"
    }
  ]
}
```

- **Error Responses**:
  - `400 BAD_REQUEST`: Nếu bệnh án chưa được ký (`DomainErrorCode: MEDICAL_RECORD_NOT_SIGNED`).
  - `404 NOT_FOUND`: Nếu `visitId` không tồn tại hoặc chưa có bệnh án.
  - `403 FORBIDDEN`: Không có quyền `VISIT_SUMMARY_PRINT`.

---

### 3.2 In / Xuất phiếu tóm tắt lượt khám dạng PDF

- **Endpoint**: `GET /api/v1/visits/{visitId}/summary/print`
- **Method**: `GET`
- **Permission**: `VISIT_SUMMARY_PRINT`
- **Path Parameter**:
  - `visitId` (UUID, required): Mã định danh lượt khám.
- **Success Response**: `200 OK`
  - `Content-Type`: `application/pdf`
  - `Content-Disposition`: `attachment; filename="phieu-tom-tat-{visitCode}.pdf"`
  - `Body`: Binary file PDF chuẩn A4.
- **Audit Logging**:
  - Tự động ghi 1 bản ghi vào `audit_logs` (`ActionType.EXPORT`, `ResourceType.VISIT`).
  - Tự động ghi 1 bản ghi vào `medical_record_access_logs` (`MedicalRecordAccessAction.PRINT`, `detail: "In phiếu tóm tắt lượt khám"`).

- **Error Responses**:
  - `400 BAD_REQUEST`: Nếu bệnh án chưa được ký (`MEDICAL_RECORD_NOT_SIGNED`).
  - `404 NOT_FOUND`: Nếu không tìm thấy lượt khám hoặc bệnh án.
  - `403 FORBIDDEN`: Không có quyền `VISIT_SUMMARY_PRINT`.
