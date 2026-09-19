# Specialty and Room/Doctor Management API

> User Story: **NCL-09-CN-007 — Quản lý danh mục chuyên khoa và phòng khám bệnh**  
> Status: **Implemented in backend migration V72**  
> Base URL: `/api/v1` (hoặc `/` tùy cấu hình servlet path)  
> Auth: Bearer JWT  

---

## 1. Scope & Phân Quyền (Authorization)

Chức năng này cho phép Quản trị viên (`ADMIN`) quản lý danh mục chuyên khoa, gán bác sĩ phụ trách và phòng khám bệnh cho từng chuyên khoa.

* **Quyền quản trị (`SPECIALTY_MANAGE`)**:
  - `GET /system/specialties/{id}` (Chi tiết chuyên khoa kèm danh sách bác sĩ & phòng khám)
  - `POST /system/specialties` (Tạo mới chuyên khoa)
  - `PUT /system/specialties/{id}` (Cập nhật thông tin chuyên khoa)
  - `PATCH /system/specialties/{id}/deactivate` (Ngừng dùng chuyên khoa)
  - `PATCH /system/specialties/{id}/activate` (Kích hoạt lại chuyên khoa)
* **Quyền xem danh mục (`authenticated()`)**:
  - `GET /system/specialties` (Danh sách chuyên khoa, filter `active`, `keyword`)
  - `GET /users/doctors?specialtyId={id}` (Lọc danh sách bác sĩ theo chuyên khoa - yêu cầu `USER_READ`)

---

## 2. API Endpoints

### 2.1. Tra cứu danh sách chuyên khoa
* **Endpoint**: `GET /system/specialties`
* **Query Params**:
  - `keyword` (String, optional): Từ khóa tìm kiếm theo mã hoặc tên.
  - `active` (Boolean, optional): Lọc theo trạng thái hoạt động (`true`, `false`, hoặc bỏ trống để lấy tất cả).
* **Response `200 OK`**:
```json
[
  {
    "id": "f0000000-0000-0000-0000-000000000001",
    "code": "GENERAL",
    "name": "General",
    "description": "Khám tổng quát đa khoa",
    "active": true
  }
]
```

---

### 2.2. Xem chi tiết chuyên khoa
* **Endpoint**: `GET /system/specialties/{id}`
* **Permission**: `SPECIALTY_MANAGE`
* **Response `200 OK`**:
```json
{
  "id": "9f5b0c8a-4a29-4e0c-b63c-9cf1bc9d3f17",
  "code": "PEDIATRICS",
  "name": "Khoa Nhi",
  "description": "Khám và điều trị chuyên khoa nhi",
  "active": true,
  "doctors": [
    {
      "id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
      "username": "doctor_a",
      "fullName": "Nguyễn Văn A",
      "email": "doctor_a@clinic.com",
      "phone": "0901234567"
    }
  ],
  "rooms": [
    {
      "id": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1",
      "code": "P101",
      "name": "Phòng khám Nhi 1",
      "active": true,
      "createdAt": "2026-09-18T10:00:00Z",
      "updatedAt": null
    }
  ],
  "activeTemplateCount": 1,
  "createdAt": "2026-09-18T10:00:00Z",
  "updatedAt": "2026-09-18T10:00:00Z"
}
```

---

### 2.3. Tạo mới chuyên khoa (NCL-09-CN-007-TC-01, TC-03)
* **Endpoint**: `POST /system/specialties`
* **Permission**: `SPECIALTY_MANAGE`
* **Request Body**:
```json
{
  "code": "PEDIATRICS",
  "name": "Khoa Nhi",
  "description": "Khám và điều trị chuyên khoa nhi",
  "doctorIds": ["aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"],
  "roomIds": ["bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"]
}
```
* **Response `201 Created`**: Trả về `SpecialtyDetailResponse`.
* **Lỗi tiềm ẩn**:
  - `409 Conflict` (`SPECIALTY_NAME_ALREADY_EXISTS`): Tên chuyên khoa đã tồn tại (không phân biệt hoa thường).
  - `409 Conflict` (`SPECIALTY_CODE_ALREADY_EXISTS`): Mã chuyên khoa đã tồn tại.
  - `400 Bad Request` (`VALIDATION_FAILED`): Bác sĩ hoặc phòng khám không hợp lệ hoặc không hoạt động.

---

### 2.4. Cập nhật chuyên khoa
* **Endpoint**: `PUT /system/specialties/{id}`
* **Permission**: `SPECIALTY_MANAGE`
* **Request Body**:
```json
{
  "name": "Khoa Nhi Tổng Hợp",
  "description": "Mô tả mới",
  "doctorIds": ["aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2"],
  "roomIds": ["bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1"]
}
```
* **Response `200 OK`**: Trả về `SpecialtyDetailResponse`.

---

### 2.5. Ngừng dùng chuyên khoa (NCL-09-CN-007-TC-02)
* **Endpoint**: `PATCH /system/specialties/{id}/deactivate`
* **Permission**: `SPECIALTY_MANAGE`
* **Query Params**:
  - `confirm` (Boolean, default = `false`): Cờ xác nhận ngừng dùng khi có đối tượng đang liên kết.
* **Xử lý cảnh báo (Sai trạng thái - TC-02)**:
  - Nếu chuyên khoa đang có bác sĩ gán vào HOẶC có mẫu bệnh án đang active:
    - Khi `confirm=false`: Trả về `409 Conflict`:
      ```json
      {
        "code": "SPECIALTY_IN_USE",
        "message": "Chuyên khoa đang được gán cho 1 bác sĩ và 1 mẫu bệnh án đang hoạt động. Cần xác nhận trước khi ngừng dùng."
      }
      ```
    - Khi `confirm=true`: Thực hiện chuyển trạng thái sang `active = false` và trả về `200 OK`:
      ```json
      {
        "id": "9f5b0c8a-4a29-4e0c-b63c-9cf1bc9d3f17",
        "code": "PEDIATRICS",
        "name": "Khoa Nhi",
        "description": "...",
        "active": false
      }
      ```
* **Quy tắc bảo vệ `GENERAL`**:
  - Nếu gửi yêu cầu deactivate đối với chuyên khoa `GENERAL` (`f0000000-0000-0000-0000-000000000001`):
    - Trả về `400 Bad Request` (`CANNOT_DEACTIVATE_DEFAULT_SPECIALTY`): "Không thể ngừng dùng chuyên khoa mặc định hệ thống (GENERAL)."

---

### 2.6. Kích hoạt lại chuyên khoa
* **Endpoint**: `PATCH /system/specialties/{id}/activate`
* **Permission**: `SPECIALTY_MANAGE`
* **Response `200 OK`**: Trả về `SpecialtyResponse` với `active: true`.
