# Account Lockout and Unlock API Contract

## 1. Overview
User Story: **NCL-01-CN-008** - Khóa tạm tài khoản khi đăng nhập sai nhiều lần.
Business Rules: `QTN-46`, `QTN-01`, `QTN-31`.
Acceptance Criteria: `TC-01`, `TC-02`, `TC-03`, `TC-04`.

Tài liệu này chuẩn hóa API contract cho:
1. Phản hồi khóa tạm thời khi đăng nhập sai vượt quá số lần quy định (HTTP 429).
2. API cho Quản trị viên mở khóa tài khoản người dùng trước thời hạn (Early Unlock).
3. API tra cứu lịch sử đăng nhập/khóa/mở khóa tài khoản (Login Audit Logs).

---

## 2. Authentication & Lockout Behavior

### 2.1. Login Endpoint
- **URL**: `/api/v1/auth/login`
- **Method**: `POST`
- **Headers**: `Content-Type: application/json`

#### Request Body
```json
{
  "username": "bacsi_an",
  "password": "WrongPassword123!"
}
```

#### Responses

##### Trường hợp 1: Đăng nhập sai từ lần 1 đến lần 4
- **HTTP Status**: `401 Unauthorized`
- **Response Body**:
```json
{
  "status": 401,
  "code": "INVALID_CREDENTIALS",
  "message": "Tên đăng nhập hoặc mật khẩu không chính xác.",
  "path": "/api/v1/auth/login",
  "timestamp": "2026-03-30T10:30:00Z"
}
```

##### Trường hợp 2: Đăng nhập sai lần thứ 5 (Kích hoạt khóa tạm thời - TC-01)
- **HTTP Status**: `429 Too Many Requests`
- **Response Headers**:
  - `Retry-After: 900`
- **Response Body**:
```json
{
  "status": 429,
  "code": "TOO_MANY_LOGIN_ATTEMPTS",
  "message": "Hệ thống tạm khóa đăng nhập. Vui lòng thử lại sau 900 giây.",
  "path": "/api/v1/auth/login",
  "timestamp": "2026-03-30T10:30:00Z",
  "details": {
    "retryAfterSeconds": 900,
    "blockedUntil": "2026-03-30T10:45:00Z"
  }
}
```
*Ghi chú*: Hệ thống đồng thời tự động ghi Audit Log với `actionType: LOCK`, `resourceType: USER`, `resourceId: <user_id>`.

##### Trường hợp 3: Đang trong thời gian khóa tạm thời (TC-02)
Dù người dùng nhập đúng mật khẩu hay sai mật khẩu, yêu cầu đều bị chặn ngay ở đầu luồng:
- **HTTP Status**: `429 Too Many Requests`
- **Response Headers**:
  - `Retry-After: <số giây còn lại>`
- **Response Body**:
```json
{
  "status": 429,
  "code": "TOO_MANY_LOGIN_ATTEMPTS",
  "message": "Hệ thống tạm khóa đăng nhập. Vui lòng thử lại sau 750 giây.",
  "path": "/api/v1/auth/login",
  "timestamp": "2026-03-30T10:32:30Z",
  "details": {
    "retryAfterSeconds": 750,
    "blockedUntil": "2026-03-30T10:45:00Z"
  }
}
```

##### Trường hợp 4: Đăng nhập đúng trước khi bị khóa (TC-03)
Nếu người dùng nhập sai 1-4 lần rồi nhập đúng mật khẩu:
- Số lần thử sai được reset về 0.
- Trả về `200 OK` kèm access token và refresh token.

---

## 3. Admin Unlock User Account (Early Unlock)

Cho phép Quản trị viên (Admin) mở khóa tài khoản trước khi hết thời gian 15 phút.

- **URL**: `/api/v1/users/{id}/unlock`
- **Method**: `POST`
- **Authorization**: Bearer Token (Yêu cầu quyền `USER_UPDATE`)
- **Path Variables**:
  - `id` (UUID): ID của người dùng cần mở khóa.

### Success Response
- **HTTP Status**: `200 OK`
- **Response Body**:
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "username": "bacsi_an",
  "fullName": "Bác sĩ An",
  "email": "an@hospital.vn",
  "phone": "0901234567",
  "role": "DOCTOR",
  "active": true
}
```
*Ghi chú*: Hệ thống ghi Audit Log với:
- `userId`: Admin thực hiện mở khóa (lấy từ CurrentUserPort)
- `actionType`: `UNLOCK`
- `resourceType`: `USER`
- `resourceId`: `id` của user được mở khóa
- `detail`: `{"unlockedUsername": "bacsi_an"}`

### Error Responses
- **401 Unauthorized**: Chưa đăng nhập hoặc Token hết hạn.
- **403 Forbidden**: Người dùng không có quyền `USER_UPDATE`.
- **404 Not Found**: Không tìm thấy người dùng với `id` tương ứng (`USER_NOT_FOUND`).

---

## 4. Retrieve Login Audit Logs (TC-04)

Cho phép Quản trị viên tra cứu lịch sử đăng nhập/khóa/mở khóa của một tài khoản người dùng cụ thể.

- **URL**: `/api/v1/users/{id}/login-logs`
- **Method**: `GET`
- **Authorization**: Bearer Token (Yêu cầu quyền `USER_READ`)
- **Path Variables**:
  - `id` (UUID): ID của người dùng.
- **Query Parameters**:
  - `page` (int, default: 0): Số trang.
  - `size` (int, default: 20): Kích thước trang.

### Success Response
- **HTTP Status**: `200 OK`
- **Response Body** (Spring Data Page):
```json
{
  "content": [
    {
      "id": "7b8f9e0a-1234-5678-9abc-def012345678",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "actionType": "LOCK",
      "resourceType": "USER",
      "resourceId": "123e4567-e89b-12d3-a456-426614174000",
      "detail": "{\"username\":\"bacsi_an\",\"failedAttempts\":5,\"blockedUntil\":\"2026-03-30T10:45:00Z\"}",
      "ipAddress": null,
      "createdAt": "2026-03-30T10:30:00Z"
    },
    {
      "id": "8c9a0f1b-2345-6789-0bcd-ef0123456789",
      "userId": "11111111-1111-1111-1111-111111111111",
      "actionType": "UNLOCK",
      "resourceType": "USER",
      "resourceId": "123e4567-e89b-12d3-a456-426614174000",
      "detail": "{\"unlockedUsername\":\"bacsi_an\"}",
      "ipAddress": null,
      "createdAt": "2026-03-30T10:35:00Z"
    },
    {
      "id": "9da1b2c3-3456-7890-1cde-f01234567890",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "actionType": "LOGIN",
      "resourceType": "USER_SESSION",
      "resourceId": "550e8400-e29b-41d4-a716-446655440000",
      "detail": "{\"username\":\"bacsi_an\"}",
      "ipAddress": null,
      "createdAt": "2026-03-30T10:36:00Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 3,
  "totalPages": 1,
  "first": true,
  "last": true
}
```
