# Two-Factor Authentication for High-Privilege Accounts (NCL-01-CN-006)

## 1. Business rules

- Quản trị viên (Admin) có thể bật/tắt yêu cầu xác thực hai lớp theo vai trò.
- Các vai trò được hỗ trợ: `ADMIN`, `DOCTOR`.
- Người dùng thuộc vai trò bắt buộc phải nhập thêm "mã xác thực mô phỏng" sau khi
  nhập đúng mật khẩu.
- Vai trò không bật 2FA (ví dụ `RECEPTIONIST`) đăng nhập bình thường chỉ bằng mật khẩu.
- Mọi lần xác thực lớp hai thất bại đều được ghi nhật ký (audit log).

## 2. Authentication flow

### Role without 2FA

```
username/password -> password valid -> normal session + JWT
```

### Role with 2FA

```
username/password -> password valid
        -> create challenge + issue simulated code (NO session/JWT yet)
        -> client submits challengeToken + code
        -> code valid -> consume challenge -> normal session + JWT
```

Điểm then chốt: **nhập đúng mật khẩu KHÔNG tạo session/JWT** cho vai trò bắt buộc 2FA.
JWT truy cập thông thường chỉ được cấp sau khi mã lớp hai được xác minh thành công.

## 3. Challenge lifecycle

- Mỗi lần đăng nhập (vai trò bắt buộc 2FA) tạo một `TwoFactorChallenge`:
  - `id` = challenge token trả về cho client (UUID ngẫu nhiên, không thể đoán).
  - `userId` lấy từ tài khoản server-side (KHÔNG tin vào client).
  - `codeHash` = BCrypt hash của mã xác thực (không lưu plaintext).
  - `expiresAt` = thời hạn hiệu lực của mã (300 giây).
  - `consumedAt` = thời điểm đã dùng (chống replay).
- Xác minh thành công sẽ tiêu thụ challenge một cách **nguyên tử** (UPDATE có điều kiện
  `consumed_at IS NULL`) nên không thể dùng lại / xác thực đồng thời hai lần.

## 4. Code expiration / request new code

- Mã hết hạn -> xác minh bị từ chối (`VERIFICATION_CODE_EXPIRED`).
- Client gọi lại `POST /auth/2fa/resend` với cùng `twoFactorToken` để nhận mã mới:
  - Mã cũ bị thay bằng mã mới (rotate), `expiresAt` mới, số lần thử được reset.

## 5. Audit events

| Sự kiện | ActionType | ResourceType | Ghi chú |
|---|---|---|---|
| Nhập sai mã lớp hai | `LOGIN_FAILED` | `USER_SESSION` | reason = `INVALID_CODE` |
| Mã lớp hai hết hạn | `LOGIN_FAILED` | `USER_SESSION` | reason = `EXPIRED_CODE` |
| Challenge đã dùng / không hợp lệ | `LOGIN_FAILED` | `USER_SESSION` | reason = `CHALLENGE_ALREADY_CONSUMED` |
| Xác minh lớp hai thành công | `LOGIN` | `USER_SESSION` | detail `twoFactor: true` |
| Bật/tắt 2FA theo vai trò | `UPDATE` | `ROLE` | before/after `twoFactorRequired` |

Không ghi: mật khẩu, mã xác thực dạng plaintext.

## 6. Security bypass protections

1. Không cấp JWT sau bước mật khẩu cho vai trò 2FA.
2. Principal chỉ được tạo từ JWT thật (không có JWT trung gian cho trạng thái chưa 2FA).
3. Refresh token không thể nâng cấp trạng thái chưa 2FA (2FA hoàn tất mới tạo session).
4. Không tin các trường client gửi (`twoFactorVerified`, `userId`, `role`) — mọi thứ lấy từ
   challenge server-side.
5. Challenge đã tiêu thụ không dùng lại (atomic consume).
6. Challenge hết hạn luôn thất bại.
7. Challenge của user A không dùng cho user B (userId lấy từ challenge).
8. Vai trò lấy từ tài khoản server-side, không từ request body.
9. Logout/refresh tương tác như luồng hiện tại (session bị revoke khi đăng nhập mới).
10. Tiêu thụ challenge là nguyên tử -> chặn xác thực đồng thời trùng lặp.

## 7. API contract

### Login (2FA required)

`POST /auth/login` `{ "username": "...", "password": "..." }`

Với vai trò bắt buộc 2FA trả về `200`:

```json
{
  "userId": "...",
  "username": "doctor1",
  "accessToken": null,
  "refreshToken": null,
  "role": "DOCTOR",
  "expiredAt": null,
  "mustChangePassword": false,
  "twoFactorRequired": true,
  "twoFactorToken": "<challengeId>",
  "twoFactorExpiresAt": "2026-09-22T10:05:00Z"
}
```

Với vai trò không bắt buộc 2FA trả về cấu trúc cũ (`accessToken`, `refreshToken`, ...,
`twoFactorRequired: false`, `twoFactorToken: null`).

### Verify second factor

`POST /auth/2fa/verify` `{ "twoFactorToken": "...", "code": "123456" }`

- `200` — thành công, trả về `LoginResponse` đầy đủ (`accessToken`, `refreshToken`, ...).
- `400 INVALID_VERIFICATION_CODE` — sai mã.
- `400 VERIFICATION_CODE_EXPIRED` — mã hết hạn.
- `401 TWO_FACTOR_CHALLENGE_INVALID` — challenge không tồn tại / đã dùng.

### Resend code

`POST /auth/2fa/resend` `{ "twoFactorToken": "..." }`

- `200` — `{ "twoFactorToken": "...", "expiresAt": "..." }`.

### Configure 2FA by role (admin only)

`PATCH /admin/two-factor-auth/roles/{roleName}` `{ "enabled": true }`

- `roleName` phải là `ADMIN` hoặc `DOCTOR` (không phân biệt hoa thường).
- Yêu cầu quyền `TWO_FACTOR_AUTH_MANAGE` (seed cho ADMIN).
- `200` — `{ "roleName": "DOCTOR", "twoFactorRequired": true }`.

## 8. Assumptions (mã xác thực mô phỏng)

Workbook chỉ yêu cầu "mã xác thực mô phỏng", không chỉ định SMS/email/TOTP. Backend dùng
`VerificationCodeGeneratorPort` (mã 6 chữ số) và `TwoFactorCodeDeliveryPort` với mock adapter
(`MockTwoFactorCodeDeliveryAdapter`). Mã được giữ trong bộ nhớ của mock để phục vụ test và
KHÔNG được ghi vào log ứng dụng thông thường. Frontend tương lai chỉ cần gọi các endpoint
trên; không cần sửa backend.

## 9. Database

- `V85__add_two_factor_authentication.sql`:
  - Thêm cột `roles.two_factor_required BOOLEAN NOT NULL DEFAULT FALSE`.
  - Tạo bảng `two_factor_challenges`.
  - Seed permission `TWO_FACTOR_AUTH_MANAGE` và gán cho `ADMIN`.
