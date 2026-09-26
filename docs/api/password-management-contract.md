# Hợp đồng API: Quản lý đổi và đặt lại mật khẩu nhân viên (NCL-01-CN-005)

Tài liệu này mô tả chi tiết hợp đồng API backend phục vụ User Story **NCL-01-CN-005: Đổi mật khẩu và đặt lại mật khẩu nhân viên** theo các quy tắc nghiệp vụ `QTN-28`, `QTN-01`, `QTN-31`.

---

## 1. Tự đổi mật khẩu (Self-service Change Password)

- **Endpoint**: `POST /api/v1/auth/change-password`
- **Xác thực**: Bắt buộc (Bearer JWT qua Header `Authorization: Bearer <token>`).
- **Vai trò áp dụng**: Mọi tài khoản nhân viên phòng khám đang hoạt động (`ADMIN`, `DOCTOR`, `RECEPTIONIST`, `PHARMACIST`, `MANAGER`).

### 1.1. Request Body
```json
{
  "oldPassword": "CurrentPassword123",
  "newPassword": "NewSecurePassword456"
}
```

### 1.2. Chính sách mật khẩu áp dụng (QTN-28 / TC-02)
- Độ dài từ **8 đến 50** ký tự.
- Chứa ít nhất **1 chữ cái in hoa** (`A-Z`).
- Chứa ít nhất **1 chữ cái viết thường** (`a-z`).
- Chứa ít nhất **1 chữ số** (`0-9`).
- Mật khẩu mới **không được trùng** với mật khẩu cũ.

### 1.3. Phản hồi thành công
- **HTTP Status**: `200 OK` (hoặc `204 No Content`)
- **Tác động hệ thống**:
  - Mật khẩu mới được băm và lưu vào cơ sở dữ liệu.
  - Cờ `must_change_password` được đưa về `false`.
  - Toàn bộ phiên làm việc (`UserSession`) hiện tại và cũ của tài khoản bị thu hồi ngay lập tức (`revokedAt = now`).
  - Ghi bản ghi `AuditLog` với `actionType: "CHANGE_PASSWORD"`.

### 1.4. Lỗi có thể xảy ra
- `400 Bad Request` (`INVALID_OLD_PASSWORD`): Mật khẩu hiện tại không chính xác.
- `400 Bad Request` (`SAME_PASSWORD_NOT_ALLOWED`): Mật khẩu mới trùng với mật khẩu cũ.
- `400 Bad Request` (`WEAK_PASSWORD`): Mật khẩu không đạt độ mạnh tối thiểu kèm danh sách `details.violations`.
- `401 Unauthorized`: Token không hợp lệ hoặc phiên đã hết hạn.

---

## 2. Quản trị viên đặt lại mật khẩu nhân viên (Admin Reset Password)

- **Endpoint**: `POST /api/v1/users/{id}/reset-password`
- **Xác thực**: Bắt buộc (Bearer JWT).
- **Phân quyền**: Yêu cầu quyền `USER_RESET_PASSWORD` hoặc `USER_UPDATE` (chỉ cấp cho vai trò `ADMIN`).
- **Quy tắc QTN-01 & TC-04**: Các vai trò khác (như Lễ tân, Bác sĩ, Dược sĩ) khi cố tình gọi API này sẽ nhận `403 Forbidden` và hệ thống tự động ghi nhật ký truy cập trái phép (`ACCESS_DENIED`).

### 2.1. Request Body (Tùy chọn)
Có thể để trống Body (hoặc `{}`) để hệ thống tự sinh mật khẩu ngẫu nhiên an toàn, hoặc truyền mật khẩu tạm tự chọn:
```json
{
  "temporaryPassword": "CustomTempPass123"
}
```

### 2.2. Phản hồi thành công (TC-03)
- **HTTP Status**: `200 OK`
```json
{
  "userId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2",
  "username": "doctor1",
  "temporaryPassword": "GeneratedTemp123",
  "resetAt": "2026-09-07T10:00:00Z"
}
```
- **Tác động hệ thống**:
  - Mật khẩu tạm được băm và cập nhật cho tài khoản mục tiêu.
  - Cột `must_change_password` của tài khoản được đặt thành `true`.
  - Toàn bộ phiên làm việc của tài khoản mục tiêu bị thu hồi ngay lập tức.
  - Ghi nhật ký quản trị `AuditLog` với `actionType: "RESET_PASSWORD"`, lưu rõ Admin thực hiện, tài khoản bị tác động và thời điểm (QTN-31 / TC-05).
  - Khi tài khoản đăng nhập với mật khẩu tạm, API `/auth/login` trả về `mustChangePassword: true` buộc người dùng đổi mật khẩu.

---

## 3. Mã lỗi nghiệp vụ đăng ký (Domain Error Codes)

| Mã lỗi | HTTP Status | Ý nghĩa |
|---|---|---|
| `INVALID_OLD_PASSWORD` | `400 Bad Request` | Mật khẩu hiện tại nhập sai |
| `SAME_PASSWORD_NOT_ALLOWED` | `400 Bad Request` | Mật khẩu mới trùng với mật khẩu hiện tại |
| `WEAK_PASSWORD` | `400 Bad Request` | Mật khẩu không thỏa mãn chính sách độ mạnh tối thiểu |
| `USER_NOT_FOUND` | `404 Not Found` | Tài khoản nhân viên không tồn tại |
| `ACCOUNT_DISABLED` | `403 Forbidden` | Tài khoản nhân viên đang bị khóa hoặc vô hiệu hóa |
| `MUST_CHANGE_PASSWORD` | `403 Forbidden` | Tài khoản đang yêu cầu đổi mật khẩu trước khi tiếp tục thao tác nghiệp vụ khác |
