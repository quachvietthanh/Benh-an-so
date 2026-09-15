# NCL-02-CN-006 — Hợp đồng backend chức năng Gộp hồ sơ bệnh nhân trùng

## 1. Phạm vi

Tài liệu này chốt toàn bộ hợp đồng API, phân quyền, quy tắc nghiệp vụ và ràng buộc cơ sở dữ liệu cho chức năng Gộp hồ sơ bệnh nhân trùng (User Story `NCL-02-CN-006`). Không can thiệp frontend.

## 2. Quy tắc nghiệp vụ & Quyền hạn

- **Phân quyền (QTN-01)**:
  - Chỉ các vai trò Tiếp đón (`RECEPTIONIST`), Quản lý (`MANAGER`), và Quản trị hệ thống (`ADMIN`) có quyền thực hiện gộp hồ sơ (`PERMISSION_PATIENT_MERGE`).
  - Bác sĩ (`DOCTOR`), Dược sĩ (`PHARMACIST`) bị từ chối truy cập với mã lỗi `403 Forbidden` và hệ thống tự động ghi nhật ký `ACCESS_DENIED` vào bảng `audit_logs` (theo `NCL-02-CN-006-TC-04`).
- **Ràng buộc định danh & Khóa dữ liệu (QTN-33)**:
  - Không xóa vật lý bất kỳ hồ sơ nào. Hồ sơ nguồn sau khi gộp sẽ chuyển sang trạng thái `status = 'MERGED'`, `active = false`, trỏ `merged_into_patient_id` sang hồ sơ đích.
  - Hồ sơ nguồn trở thành **Chỉ đọc (Read-only)**: Mọi thao tác cập nhật (`PUT /patients/{id}`) trên hồ sơ đã gộp sẽ bị từ chối với lỗi `409 Conflict` kèm mã lỗi `PATIENT_ALREADY_MERGED` và liên kết đến hồ sơ đích (`NCL-02-CN-006-TC-03`).
  - Chặn gộp khi có xung đột định danh: Từ chối gộp nếu số CCCD/CMND khác nhau, hoặc thông tin nhân khẩu (ngày sinh, giới tính) khác nhau trong khi cả hai hồ sơ đều đã có bệnh án ký khóa chuyên môn.
- **Chuyển giao dữ liệu toàn diện (NCL-02-CN-006-TC-01)**:
  - Chuyển quyền sở hữu của tất cả các thực thể lâm sàng và tài chính sang hồ sơ đích: `visits`, `appointments`, `queue_items`, `clinical_orders`, `follow_up_reminders`, `post_care_logs`, `prescription_allergy_warning_logs`, `medical_record_access_logs`.
  - Khử trùng thông minh dị ứng thuốc (`patient_allergies`): Nếu chất gây dị ứng đã tồn tại trên hồ sơ đích (trùng `active_normalized_name`), bản ghi trùng trên hồ sơ nguồn sẽ bị deactivate thay vì báo lỗi vi phạm unique index.
- **Tra cứu lịch sử khám bệnh liền mạch (NCL-02-CN-006-TC-02)**:
  - Khi bác sĩ tra cứu lịch sử khám bệnh của hồ sơ đích (`GET /medical-history/patients/{targetPatientId}`), toàn bộ các lượt khám từ cả hồ sơ nguồn và hồ sơ đích sẽ hiển thị theo dòng thời gian thống nhất (`visitAt DESC`).
- **Kiểm toán & Truy vết (NCL-02-CN-006-TC-05)**:
  - Ghi `PatientChangeLog` (hành động `MERGE`) cho cả hai hồ sơ nguồn và đích.
  - Ghi bản ghi hệ thống `AuditLog` với `action = 'MERGE'`, `resource = 'PATIENT'`, `resourceId = targetPatientId`, trường `detail` lưu mã bệnh nhân nguồn, mã bệnh nhân đích, ID người thực hiện và mốc thời gian gộp.

## 3. API Contract

### 3.1 Gộp hồ sơ trùng
- **Endpoint**: `POST /patients/merge`
- **Quyền yêu cầu**: `@RequirePermission("PATIENT_MERGE")`
- **Request Body**:
```json
{
  "sourcePatientId": "4c9d57a2-1d54-4638-953e-8e4349386c63",
  "targetPatientId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "reason": "Hồ sơ trùng tiếp đón do bệnh nhân không mang CCCD lần đầu"
}
```
- **Response `200 OK`**:
```json
{
  "sourcePatientId": "4c9d57a2-1d54-4638-953e-8e4349386c63",
  "sourcePatientCode": "BN000015",
  "targetPatientId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "targetPatientCode": "BN000016",
  "transferredVisitsCount": 3,
  "mergedBy": "3b2cb612-4cfc-40ad-a035-71cb14b09b55",
  "reason": "Hồ sơ trùng tiếp đón do bệnh nhân không mang CCCD lần đầu",
  "mergedAt": "2026-09-15T11:00:00Z"
}
```

### 3.2 Tìm kiếm hồ sơ nghi trùng
- **Endpoint**: `GET /patients/duplicates`
- **Quyền yêu cầu**: `@RequirePermission("PATIENT_READ")`
- **Response `200 OK`**:
```json
[
  {
    "fullName": "Nguyễn Văn A",
    "dateOfBirth": "1990-01-01",
    "phone": "0901234567",
    "candidates": [
      {
        "id": "4c9d57a2-1d54-4638-953e-8e4349386c63",
        "patientCode": "BN000015",
        "fullName": "Nguyễn Văn A",
        "dateOfBirth": "1990-01-01",
        "gender": "MALE",
        "phone": "0901234567",
        "status": "ACTIVE",
        "isMerged": false
      },
      {
        "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        "patientCode": "BN000016",
        "fullName": "NGUYỄN VĂN A",
        "dateOfBirth": "1990-01-01",
        "gender": "MALE",
        "phone": "0901234567",
        "status": "ACTIVE",
        "isMerged": false
      }
    ]
  }
]
```

## 4. Bảng mã lỗi xử lý

| HTTP Status | Mã lỗi nghiệp vụ | Mô tả |
|---|---|---|
| `400 BAD REQUEST` | `CANNOT_MERGE_SAME_PATIENT` | Không thể gộp hồ sơ vào chính nó (`sourceId == targetId`). |
| `403 FORBIDDEN` | `ACCESS_DENIED` | Người dùng không có quyền `PATIENT_MERGE` (ví dụ: `DOCTOR`). |
| `404 NOT FOUND` | `PATIENT_NOT_FOUND` | Hồ sơ nguồn hoặc hồ sơ đích không tồn tại. |
| `409 CONFLICT` | `PATIENT_ALREADY_MERGED` | Hồ sơ nguồn hoặc hồ sơ đích đã ở trạng thái gộp trước đó. |
| `409 CONFLICT` | `PATIENT_IDENTITY_CONFLICT` | Xung đột thông tin định danh (CCCD hoặc ngày sinh/giới tính) đã ký khóa. |
