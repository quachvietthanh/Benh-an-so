# Cashier Shift Settlement API Contract

> Module: **NCL-07 — Thu phí và hóa đơn**  
> User Story: **NCL-07-CN-009 — Chốt ca thu ngân cuối ngày**  
> Business Rules: **QTN-38 (Chốt ca thu ngân), QTN-09 (Điều chỉnh hóa đơn có vết)**  
> Base URL: `http://localhost:8080/api/v1`  
> Auth: Bearer Token (JWT)  
> Status: **Official Baseline**

---

## 1. Phạm Vi & Mục Tiêu Nghiệp Vụ

Tài liệu này quy định hợp đồng giao tiếp API cho chức năng **Chốt ca thu ngân cuối ngày** (`NCL-07-CN-009`):
1. **Xem tóm tắt ca hiện tại** (`GET /cashier-shifts/current-summary`): Giúp lễ tân thu ngân kiểm tra tổng hợp số tiền hệ thống ghi nhận theo từng phương thức thanh toán (`CASH`, `BANK_TRANSFER`, `CARD`, `QR_CODE`, `E_WALLET`) và số lượng giao dịch trước khi chốt ca.
2. **Chốt ca thu ngân** (`POST /cashier-shifts/close`): Lễ tân nhập số tiền mặt thực tế kiểm đếm (`actualCashAmount`) và ghi chú giải trình (nếu có chênh lệch). Hệ thống tính toán chênh lệch (`differenceAmount = actualCashAmount - systemCashAmount`), tạo phiếu chốt ca và đóng băng các khoản thu thuộc ca đó.
3. **Xác nhận phiếu chốt ca** (`POST /cashier-shifts/{id}/confirm`): Quản lý phòng khám duyệt và xác nhận phiếu chốt ca, đặc biệt trong các trường hợp có chênh lệch âm/dương (`differenceAmount != 0`).
4. **Tra cứu danh sách & chi tiết ca chốt** (`GET /cashier-shifts`, `GET /cashier-shifts/{id}`): Cho phép lễ tân, quản lý và quản trị viên tra cứu lịch sử bàn giao ca.
5. **Bảo vệ tính toàn vẹn của ca đã chốt (`QTN-38`, `QTN-09`, `AC-03`)**: Chặn mọi hành vi sửa đổi trực tiếp hoặc hoàn tiền các khoản thu (`Payment`) thuộc ca đã chốt; mọi điều chỉnh tài chính phải thông qua hóa đơn điều chỉnh.

---

## 2. Phân Quyền (Roles & Permissions)

| Endpoint | Method | Required Permission | Allowed Roles | Mô tả nghiệp vụ |
|---|---|---|---|---|
| `/cashier-shifts/current-summary` | GET | `CASHIER_SHIFT_READ` | `RECEPTIONIST`, `MANAGER`, `ADMIN` | Xem tổng hợp ca thu ngân hiện tại của người dùng đăng nhập |
| `/cashier-shifts/close` | POST | `CASHIER_SHIFT_CREATE` | `RECEPTIONIST`, `ADMIN` | Tạo phiếu chốt ca và đối chiếu tiền thực tế |
| `/cashier-shifts/{id}/confirm` | POST | `CASHIER_SHIFT_CONFIRM` | `MANAGER` | Quản lý phòng khám duyệt/xác nhận phiếu chốt ca |
| `/cashier-shifts` | GET | `CASHIER_SHIFT_READ` | `RECEPTIONIST`, `MANAGER`, `ADMIN` | Tra cứu danh sách các phiếu chốt ca (phân trang) |
| `/cashier-shifts/{id}` | GET | `CASHIER_SHIFT_READ` | `RECEPTIONIST`, `MANAGER`, `ADMIN` | Xem chi tiết một phiếu chốt ca |

*Ghi chú:* Các vai trò `DOCTOR`, `PHARMACIST`, `PATIENT` bị từ chối truy cập (`403 Forbidden`) và được ghi nhật ký kiểm toán `ACCESS_DENIED`.

---

## 3. Chi Tiết API Endpoints

### 3.1 Xem tóm tắt ca hiện tại

```http
GET /cashier-shifts/current-summary
```

#### Request Headers
- `Authorization: Bearer <token>`

#### Response (200 OK)
```json
{
  "cashierId": "44444444-4444-4444-4444-444444444444",
  "cashierName": "Le Tan Vien",
  "startTime": "2026-09-21T01:00:00Z",
  "endTime": "2026-09-21T09:30:00Z",
  "totalTransactions": 5,
  "systemCashAmount": 1500000.00,
  "systemTransferAmount": 800000.00,
  "systemCardAmount": 0.00,
  "systemOtherAmount": 0.00,
  "totalSystemAmount": 2300000.00,
  "unsettledPaymentIds": [
    "p1111111-1111-1111-1111-111111111111",
    "p2222222-2222-2222-2222-222222222222"
  ]
}
```

*Lưu ý:* Nếu ca hiện tại chưa có khoản thu nào (`totalTransactions == 0`), trả về `200 OK` với các số tiền bằng `0.00`, `totalTransactions = 0`, `unsettledPaymentIds = []`, `startTime = null`.

---

### 3.2 Chốt ca thu ngân (Close Shift)

```http
POST /cashier-shifts/close
```

#### Request Body
```json
{
  "actualCashAmount": 1450000.00,
  "notes": "Lệch âm 50.000đ do thối nhầm tiền lẻ cho bệnh nhân"
}
```

#### Quy tắc xác thực & Nghiệp vụ
1. **Precondition:** Ca phải có ít nhất 1 khoản thu chưa chốt (`totalTransactions >= 1`). Nếu không có, trả về `400 Bad Request` với mã lỗi `NO_UNSETTLED_PAYMENTS`.
2. `actualCashAmount`: Bắt buộc, không được âm (`>= 0.00`).
3. `notes`: Tùy chọn nếu khớp quỹ (`difference == 0`). **Bắt buộc nhập** nếu có chênh lệch (`difference != 0`); nếu để trống sẽ trả về `400 Bad Request` với mã lỗi `SHIFT_NOTE_REQUIRED_FOR_DISCREPANCY`.
4. Trạng thái sau chốt:
   - Nếu `differenceAmount == 0`: `status = CONFIRMED` (Khớp quỹ theo `TC-01`).
   - Nếu `differenceAmount != 0`: `status = PENDING_CONFIRMATION` (Chờ quản lý duyệt theo `TC-02`).

#### Response (201 Created)
```json
{
  "id": "c0000000-0000-0000-0000-000000000001",
  "shiftCode": "CS-20260921-0001",
  "cashierId": "44444444-4444-4444-4444-444444444444",
  "cashierName": "Le Tan Vien",
  "startTime": "2026-09-21T01:00:00Z",
  "endTime": "2026-09-21T09:30:00Z",
  "totalTransactions": 5,
  "systemCashAmount": 1500000.00,
  "systemTransferAmount": 800000.00,
  "systemCardAmount": 0.00,
  "systemOtherAmount": 0.00,
  "totalSystemAmount": 2300000.00,
  "actualCashAmount": 1450000.00,
  "differenceAmount": -50000.00,
  "status": "PENDING_CONFIRMATION",
  "notes": "Lệch âm 50.000đ do thối nhầm tiền lẻ cho bệnh nhân",
  "confirmedBy": null,
  "confirmedByName": null,
  "confirmedAt": null,
  "confirmationNotes": null,
  "createdAt": "2026-09-21T09:30:00Z"
}
```

---

### 3.3 Quản lý xác nhận phiếu chốt ca (Confirm Shift)

```http
POST /cashier-shifts/{id}/confirm
```

#### Request Body
```json
{
  "confirmationNotes": "Đã đối chiếu sổ quỹ và đồng ý xử lý trừ công thu ngân"
}
```

#### Quy tắc xác thực & Nghiệp vụ
1. Chỉ người dùng có vai trò `MANAGER` (hoặc quyền `CASHIER_SHIFT_CONFIRM`) mới được phép thực hiện.
2. Phiếu chốt ca phải tồn tại, nếu không trả về `404 Not Found`.
3. Phiếu phải đang ở trạng thái `PENDING_CONFIRMATION`. Nếu phiếu đã được xác nhận trước đó, trả về `409 Conflict` với mã lỗi `SHIFT_ALREADY_CONFIRMED`.

#### Response (200 OK)
```json
{
  "id": "c0000000-0000-0000-0000-000000000001",
  "shiftCode": "CS-20260921-0001",
  "cashierId": "44444444-4444-4444-4444-444444444444",
  "cashierName": "Le Tan Vien",
  "startTime": "2026-09-21T01:00:00Z",
  "endTime": "2026-09-21T09:30:00Z",
  "totalTransactions": 5,
  "systemCashAmount": 1500000.00,
  "systemTransferAmount": 800000.00,
  "systemCardAmount": 0.00,
  "systemOtherAmount": 0.00,
  "totalSystemAmount": 2300000.00,
  "actualCashAmount": 1450000.00,
  "differenceAmount": -50000.00,
  "status": "CONFIRMED",
  "notes": "Lệch âm 50.000đ do thối nhầm tiền lẻ cho bệnh nhân",
  "confirmedBy": "66666666-6666-6666-6666-666666666666",
  "confirmedByName": "Quan Ly Phong Kham",
  "confirmedAt": "2026-09-21T10:00:00Z",
  "confirmationNotes": "Đã đối chiếu sổ quỹ và đồng ý xử lý trừ công thu ngân",
  "createdAt": "2026-09-21T09:30:00Z"
}
```

---

### 3.4 Tra cứu danh sách phiếu chốt ca (Search Shifts)

```http
GET /cashier-shifts?cashierId={uuid}&status={PENDING_CONFIRMATION|CONFIRMED|REJECTED}&from={date}&to={date}&page=0&size=20
```

#### Query Parameters
- `cashierId` (optional): UUID thu ngân chốt ca.
- `status` (optional): Trạng thái phiếu chốt.
- `from` (optional): `Instant` / `ISO-8601`.
- `to` (optional): `Instant` / `ISO-8601`.
- `page` (default: 0): Số trang.
- `size` (default: 20): Kích thước trang.

#### Response (200 OK)
Page object chứa danh sách `CashierShiftResponse`.

---

### 3.5 Xem chi tiết phiếu chốt ca

```http
GET /cashier-shifts/{id}
```

#### Response (200 OK)
Trả về đối tượng `CashierShiftResponse` kèm danh sách chi tiết các khoản thu đã chốt trong ca.

---

## 4. Bảo Vệ Khoản Thu Đã Chốt (`QTN-38`, `AC-03`)

Khi một `Payment` đã được gán vào một ca chốt (`payment.cashierShiftId != null`):
- Mọi lời gọi tới `POST /invoices/payments/{paymentId}/refund` sẽ bị từ chối với:
  - **HTTP Status:** `409 Conflict` (hoặc `400 Bad Request`).
  - **Error Code:** `PAYMENT_ALREADY_SETTLED`.
  - **Message:** `"Khoản thu đã được chốt ca thu ngân, không thể hoàn tiền trực tiếp; vui lòng thực hiện thông qua hóa đơn điều chỉnh theo quy tắc QTN-09 và QTN-38."`
