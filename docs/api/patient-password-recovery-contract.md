# Hợp đồng API: Khôi phục mật khẩu cổng bệnh nhân (NCL-14-CN-006)

Tài liệu này mô tả chi tiết hợp đồng API backend phục vụ User Story **NCL-14-CN-006: Khôi phục mật khẩu cổng bệnh nhân** theo các tiêu chí chấp nhận `TC-01..TC-04` và các quy tắc nghiệp vụ `QTN-28`, `QTN-23`, `QTN-45`, `QTN-31`.

---

## 1. Yêu cầu mã xác thực khôi phục mật khẩu (Forgot Password Request)

- **Endpoint**: `POST /auth/patient/forgot-password`
- **Xác thực**: Không yêu cầu (Public).
- **Phân quyền áp dụng**: Cổng bệnh nhân (chỉ phát sinh và gửi mã tới tài khoản có vai trò `PATIENT`).

### 1.1. Request Body
```json
{
  "phone": "0901111222"
}
```

### 1.2. Quy tắc nghiệp vụ và Bảo mật (TC-01, TC-03)
1. **Chuẩn hóa số điện thoại**: Hỗ trợ định dạng số điện thoại Việt Nam (`0...` hoặc `+84...`).
2. **Bảo vệ chống rò rỉ thông tin người dùng (Anti-enumeration / Information leakage prevention - TC-03)**:
   - Nếu số điện thoại không tồn tại trong hệ thống, hoặc số điện thoại thuộc tài khoản nhân viên (không có role `PATIENT`), hoặc tài khoản bị vô hiệu hóa: Hệ thống **không** sinh mã, nhưng **vẫn trả về HTTP 200 OK** với thông báo thành công chung.
   - Kẻ tấn công không thể dựa vào phản hồi của API để suy đoán số điện thoại nào đã đăng ký tài khoản bệnh nhân.
3. **Chống spam gửi mã (Cooldown)**:
   - Mỗi lần yêu cầu mã cho cùng một số điện thoại phải cách nhau tối thiểu 60 giây. Nếu yêu cầu lại quá nhanh, trả về `429 Too Many Requests` (`VERIFICATION_CODE_COOLDOWN`).
4. **Thời hạn hiệu lực mã (TTL)**:
   - Mã xác thực gồm 6 chữ số ngẫu nhiên, có hiệu lực trong **300 giây (5 phút)**.
   - Mã được gửi mô phỏng tới số điện thoại của bệnh nhân.

### 1.3. Phản hồi thành công (HTTP 200 OK)
```json
{
  "message": "Nếu số điện thoại đã được đăng ký tài khoản bệnh nhân, mã xác thực sẽ được gửi tới số điện thoại của bạn.",
  "expiresInSeconds": 300
}
```

---

## 2. Kiểm tra mã xác thực (Verify Recovery Code - Tùy chọn cho UI)

- **Endpoint**: `POST /auth/patient/verify-recovery-code`
- **Xác thực**: Không yêu cầu (Public).

### 2.1. Request Body
```json
{
  "phone": "0901111222",
  "code": "123456"
}
```

### 2.2. Phản hồi
- **Thành công**: `200 OK`
  ```json
  {
    "valid": true,
    "message": "Mã xác thực hợp lệ."
  }
  ```
- **Mã hết hạn (TC-02)**: `400 Bad Request`
  ```json
  {
    "code": "VERIFICATION_CODE_EXPIRED",
    "message": "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới."
  }
  ```
- **Mã không chính xác**: `400 Bad Request`
  ```json
  {
    "code": "INVALID_VERIFICATION_CODE",
    "message": "Mã xác thực không chính xác hoặc không tồn tại."
  }
  ```

---

## 3. Đặt mật khẩu mới bằng mã xác thực (Reset Password Confirm)

- **Endpoint**: `POST /auth/patient/reset-password`
- **Xác thực**: Không yêu cầu (Public).

### 3.1. Request Body
```json
{
  "phone": "0901111222",
  "code": "123456",
  "newPassword": "NewPassword123"
}
```

### 3.2. Quy tắc nghiệp vụ (TC-04, QTN-28, QTN-45, QTN-31)
1. **Kiểm tra mã xác thực**:
   - Nếu mã đã quá hạn -> Báo lỗi `VERIFICATION_CODE_EXPIRED` (TC-02).
   - Nếu mã sai -> Tăng số lần thử sai `attempts`. Nếu số lần sai đạt tối đa (5 lần) -> vô hiệu hóa mã ngay lập tức và báo `INVALID_VERIFICATION_CODE`.
2. **Kiểm tra chính sách mật khẩu (QTN-28)**:
   - Mật khẩu phải từ 8 đến 50 ký tự, có ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số. Nếu vi phạm, trả về `400 Bad Request` (`WEAK_PASSWORD`).
3. **Cập nhật mật khẩu**:
   - Mật khẩu mới được băm BCrypt và lưu vào DB.
   - Cờ `mustChangePassword` được đặt thành `false` (vì đây là bệnh nhân tự đặt mật khẩu chính thức, không phải mật khẩu tạm của Admin).
   - Đánh dấu mã OTP đã sử dụng (`used_at = now`).
4. **Thu hồi phiên làm việc (QTN-45)**:
   - Toàn bộ phiên làm việc cũ của tài khoản bệnh nhân lập tức bị thu hồi (`userSessionRepository.revokeByUserId`).
5. **Ghi nhật ký kiểm toán (QTN-31)**:
   - Tạo bản ghi `AuditLog` với `actionType: "RESET_PASSWORD"`, `resourceType: "PATIENT_PORTAL"`, lưu `userId`, `patientId`, `phone`, `ipAddress` và thời điểm.

### 3.3. Phản hồi thành công (HTTP 200 OK)
```json
{
  "message": "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."
}
```

---

## 4. Bảng mã lỗi nghiệp vụ bổ sung (Domain Error Codes)

| Mã lỗi | HTTP Status | Ý nghĩa |
|---|---|---|
| `VERIFICATION_CODE_EXPIRED` | `400 Bad Request` | Mã xác thực đã quá thời hạn hiệu lực (TC-02) |
| `INVALID_VERIFICATION_CODE` | `400 Bad Request` | Mã xác thực sai hoặc đã bị hủy do nhập sai nhiều lần |
| `VERIFICATION_CODE_COOLDOWN` | `429 Too Many Requests` | Yêu cầu gửi mã quá nhanh, cần chờ hết thời gian giãn cách (60s) |
| `WEAK_PASSWORD` | `400 Bad Request` | Mật khẩu mới không thỏa mãn chính sách độ mạnh tối thiểu |
| `ACCOUNT_DISABLED` | `403 Forbidden` | Tài khoản bệnh nhân đang bị khóa hoặc vô hiệu hóa |
