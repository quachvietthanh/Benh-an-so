# Medical Record Audit Log API Contract

> Module: **NCL-08 - Báo cáo vận hành và nhật ký truy cập**
>
> User Story: **NCL-08-CN-003 - Nhật ký truy cập bệnh án**
>
> Base URL: `http://localhost:8080/api/v1`
>
> Auth: Bearer Token (JWT)
>
> Date: **2026-08-12**
>
> Status: **Phase 1 - Contract Baseline**

---

## 1. Scope

Tài liệu này chốt contract Phase 1 cho API tra cứu nhật ký truy cập bệnh án dành cho quản trị viên.

Phạm vi của Phase 1:

1. Chốt endpoint tra cứu audit log tập trung cho admin.
2. Chốt bộ filter hỗ trợ acceptance criteria của story.
3. Chốt shape response phân trang cho frontend/admin screen.
4. Chốt validation và error contract ở mức API.
5. Chốt rule phân quyền để phase implement bám đúng `NCL-08-CN-003`.

---

## 2. Business Context

### 2.1 User Story Goal

Quản trị viên cần xem được nhật ký các lần truy cập bệnh án để giám sát:

- ai đã xem hoặc sửa dữ liệu y tế nhạy cảm,
- truy cập vào bệnh nhân nào,
- trong khoảng thời gian nào.

### 2.2 Acceptance Criteria Mapped To API

Story `NCL-08-CN-003` yêu cầu nhật ký phải tra cứu được theo:

- `người dùng`
- `bệnh nhân`
- `thời gian`

Vì vậy API Phase 1 bắt buộc hỗ trợ tối thiểu các filter:

- `accessedBy`
- `patientId`
- `from`
- `to`

Ngoài ra, để phục vụ drill-down từ màn hình hồ sơ bệnh án và reuse dữ liệu log hiện có, contract cũng chốt thêm:

- `medicalRecordId`
- `visitId`

---

## 3. Endpoint Contract

### GET `/medical-records/access-logs`

Tra cứu nhật ký truy cập bệnh án theo nhiều tiêu chí lọc.

#### Roles allowed

- `ADMIN`

> Phase 1 chốt endpoint này là API giám sát dành cho quản trị viên. Các role `DOCTOR` và `NURSE` không thuộc phạm vi truy cập endpoint này.

#### Query Parameters

| Param | Type | Required | Default | Description |
|---|---|---|---|---|
| `accessedBy` | UUID | No | - | ID tài khoản đã thực hiện truy cập/chỉnh sửa |
| `patientId` | UUID | No | - | ID bệnh nhân bị truy cập |
| `medicalRecordId` | UUID | No | - | ID bệnh án cụ thể |
| `visitId` | UUID | No | - | ID lượt khám liên quan |
| `from` | ISO-8601 instant | No | - | Mốc thời gian bắt đầu, inclusive |
| `to` | ISO-8601 instant | No | - | Mốc thời gian kết thúc, inclusive |
| `page` | int | No | `0` | Zero-based page index |
| `size` | int | No | `20` | Kích thước trang, tối đa `100` |

#### Sorting

Phase 1 chốt sort cố định:

- `accessedAt DESC`
- nếu trùng `accessedAt`, sort phụ `id DESC`

Frontend không truyền sort động ở Phase 1.

#### Filtering Semantics

- Các filter là **optional**.
- Nếu truyền nhiều filter, backend áp dụng theo logic **AND**.
- Nếu không truyền filter nghiệp vụ nào ngoài `page`/`size`, backend trả về toàn bộ log theo phân trang, mới nhất trước.

---

## 4. Sample Request

### 4.1 Search by Admin Use Case

```http
GET /api/v1/medical-records/access-logs?accessedBy=11111111-2222-3333-4444-555555555555&patientId=aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee&from=2026-08-01T00:00:00Z&to=2026-08-12T23:59:59Z&page=0&size=20
Authorization: Bearer <token>
```

### 4.2 Drill-down by Medical Record

```http
GET /api/v1/medical-records/access-logs?medicalRecordId=99999999-8888-7777-6666-555555555555&page=0&size=20
Authorization: Bearer <token>
```

---

## 5. Success Response Contract

### HTTP `200 OK`

```json
{
  "content": [
    {
      "id": "4c1d22dd-31dd-4d85-8ef4-6cddf3c6c1e0",
      "patientId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
      "visitId": "d7c28591-5f3b-4383-b5d6-6b85dc3c2b34",
      "medicalRecordId": "99999999-8888-7777-6666-555555555555",
      "accessedBy": "11111111-2222-3333-4444-555555555555",
      "action": "VIEW",
      "detail": "Medical record viewed",
      "accessedAt": "2026-08-12T08:15:30Z"
    },
    {
      "id": "2b214cab-e7cb-4a3f-b0a5-a62ba678a11f",
      "patientId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
      "visitId": "d7c28591-5f3b-4383-b5d6-6b85dc3c2b34",
      "medicalRecordId": "99999999-8888-7777-6666-555555555555",
      "accessedBy": "11111111-2222-3333-4444-555555555555",
      "action": "UPDATE",
      "detail": "Medical record updated",
      "accessedAt": "2026-08-12T08:10:02Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

### Response Field Rules

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | UUID | No | ID dòng audit log |
| `patientId` | UUID | No | ID bệnh nhân |
| `visitId` | UUID | Yes | ID lượt khám liên quan |
| `medicalRecordId` | UUID | Yes | ID bệnh án liên quan |
| `accessedBy` | UUID | No | ID user thực hiện thao tác |
| `action` | enum | No | Hành động đã được ghi log |
| `detail` | string | Yes | Mô tả ngắn của thao tác |
| `accessedAt` | instant | No | Thời điểm ghi nhận log |

### Supported `action` Values In Phase 1

Theo enum hiện tại của backend, response có thể trả về:

- `VIEW`
- `VIEW_HISTORY`
- `CREATE`
- `UPDATE`
- `LOCK`
- `AMEND`
- `EXPORT`

> Phase 1 chỉ chốt contract đọc dữ liệu; không thay đổi semantics của các action hiện có.

---

## 6. Validation Rules

### 6.1 Time Range

- Nếu cùng có `from` và `to` thì `from <= to`.
- `from` và `to` dùng định dạng `ISO-8601 instant`, ví dụ `2026-08-12T08:15:30Z`.

### 6.2 Paging

- `page >= 0`
- `1 <= size <= 100`

### 6.3 Filter Rules

- Không bắt buộc phải truyền `accessedBy`, `patientId`, `medicalRecordId`, `visitId`.
- `medicalRecordId` và `visitId` được phép đi cùng `patientId` hoặc `accessedBy`.
- Nếu client truyền UUID không đúng định dạng, backend trả `400 Bad Request`.

---

## 7. Error Contract

### 7.1 `400 Bad Request`

Các trường hợp:

- `from > to`
- `page < 0`
- `size < 1` hoặc `size > 100`
- tham số UUID hoặc thời gian sai định dạng

Sample:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "From time must not be after to time.",
  "timestamp": "2026-08-12T09:00:00Z"
}
```

### 7.2 `401 Unauthorized`

- Thiếu token
- Token không hợp lệ hoặc hết hạn

### 7.3 `403 Forbidden`

- User đã đăng nhập nhưng không có role `ADMIN`

Sample:

```json
{
  "code": "ACCESS_DENIED",
  "message": "You do not have permission to access this resource.",
  "timestamp": "2026-08-12T09:00:00Z"
}
```

---

## 8. Design Decisions Chốt Ở Phase 1

1. Dùng một endpoint search tập trung: `GET /medical-records/access-logs`.
2. Giữ filter dạng query params, không dùng request body cho search.
3. Hỗ trợ filter đúng acceptance criteria của story: `accessedBy`, `patientId`, `from`, `to`.
4. Cho phép thêm `medicalRecordId`, `visitId` để hỗ trợ drill-down và reuse dữ liệu hiện có.
5. Sort cố định theo `accessedAt DESC`.
6. Phân quyền contract ở mức `ADMIN`.
7. Response trả `Page` thay vì mảng phẳng để phù hợp màn hình giám sát có phân trang.

---

## 9. Out Of Scope For Phase 1

Các nội dung sau chưa chốt trong Phase 1 này:

- enrich response với `userName`, `patientCode`, `patientName`
- export file audit log
- cảnh báo truy cập bất thường (`NCL-15-CN-002`)
- sort động theo nhiều cột
- filter theo IP address hoặc theo action list nhiều giá trị

Các phần trên sẽ được quyết định ở phase implement hoặc story phụ thuộc tiếp theo nếu cần.
