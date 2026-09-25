# Queue Number Display Contract (NCL-03-CN-014)

**User Story:** NCL-03-CN-014 — Màn hình hiển thị số thứ tự tại khu vực chờ  
**Epic:** NCL-03 — Lịch hẹn và hàng đợi khám  
**Base Path:** `/queues/display`  
**Security Level:** Public (Unauthenticated / Sảnh chờ Kiosk)  
**Related Business Rules:** QTN-43, QTN-03, QTN-40  

---

## 1. Mục Đích & Bối Cảnh Nghiệp Vụ

Màn hình công cộng tại khu vực chờ cho phép bệnh nhân và người nhà tự theo dõi số thứ tự (STT) đang khám và các số kế tiếp theo từng phòng khám, giảm tải việc hỏi đáp tại quầy lễ tân và tránh lộn xộn tại khu chờ.

### Các quy tắc cốt lõi:
1. **TC-01 (Cập nhật khi gọi khám):** Khi bác sĩ gọi bệnh nhân tiếp theo (`callNext`), màn hình khu vực chờ lập tức cập nhật số thứ tự đang khám theo từng phòng (`currentCalling`).
2. **TC-02 & QTN-43 (Che dữ liệu nhận dạng cá nhân):** Chỉ hiển thị số thứ tự và **tên viết tắt** (ví dụ: `N. V. A`). Tuyệt đối **không hiển thị họ tên đầy đủ, không hiển thị số điện thoại, địa chỉ, số CMND/CCCD, thẻ BHYT hay mã bệnh nhân**.
3. **TC-03 & QTN-40 (Ưu tiên theo phân loại):** Ca cấp cứu (`EMERGENCY`) và ca ưu tiên (`PRIORITY`) luôn được xếp lên đầu danh sách chờ kế tiếp (`waitingList`).

---

## 2. Chi Tiết API Endpoint

### `GET /queues/display`

Lấy toàn bộ dữ liệu bảng hiển thị khu chờ theo ngày và phòng khám.

#### Request Parameters (Query String):

| Tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `date` | `LocalDate` (YYYY-MM-DD) | Không | Ngày hiện tại (`today`) | Ngày khám của hàng đợi |
| `roomId` | `UUID` | Không | `null` (Tất cả phòng) | Lọc hiển thị cho riêng một phòng khám cụ thể |

#### Phân Quyền (Security):
- **Chỉ áp dụng HTTP GET (PermitAll):** Chỉ cho phép truy cập công khai không cần token JWT đối với phương thức `GET /queues/display` để hỗ trợ thiết bị Kiosk/Smart TV sảnh chờ hoạt động liên tục. Các phương thức ghi (POST, PUT, DELETE) yêu cầu xác thực và phân quyền nghiêm ngặt.
- Mọi trường dữ liệu trả về đều đã được ẩn danh tuyệt đối.
- **Trường hợp phòng chưa có bệnh nhân:** Khi truyền tham số `roomId` của một phòng khám đang hoạt động nhưng chưa có bệnh nhân nào check-in trong ngày, API vẫn trả về thông tin phòng khám (`roomId`, `roomNumber`, `roomName`) với `currentCalling: null` và `waitingList: []` để màn hình Kiosk hiển thị trạng thái sẵn sàng thay vì bị trống.

---

## 3. Cấu Trúc Dữ Liệu Response (`200 OK`)

```json
{
  "date": "2026-09-24",
  "updatedAt": "2026-09-24T08:30:00Z",
  "rooms": [
    {
      "roomId": "e3b0c442-98fc-1c14-9af0-2a3c4d5e6f01",
      "roomNumber": "P101",
      "roomName": "Phòng Khám Nội 1",
      "doctorId": "d1a2b3c4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
      "doctorName": "BS. Nguyễn Văn B",
      "currentCalling": {
        "id": "770e8400-e29b-41d4-a716-446655440222",
        "queueNumber": 12,
        "patientInitials": "N. V. A",
        "status": "IN_PROGRESS",
        "priority": "NORMAL",
        "calledAt": "2026-09-24T08:25:00Z"
      },
      "waitingList": [
        {
          "id": "880e8400-e29b-41d4-a716-446655440333",
          "queueNumber": 15,
          "patientInitials": "L. C. C",
          "status": "WAITING",
          "priority": "EMERGENCY",
          "calledAt": null
        },
        {
          "id": "990e8400-e29b-41d4-a716-446655440444",
          "queueNumber": 14,
          "patientInitials": "P. Ư. T",
          "status": "WAITING",
          "priority": "PRIORITY",
          "calledAt": null
        },
        {
          "id": "aa0e8400-e29b-41d4-a716-446655440555",
          "queueNumber": 13,
          "patientInitials": "T. T. B",
          "status": "WAITING",
          "priority": "NORMAL",
          "calledAt": null
        }
      ]
    }
  ]
}
```

### Chi tiết các trường:

| Trường | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `date` | `LocalDate` | Ngày hiển thị của bảng hàng đợi |
| `updatedAt` | `Instant` | Thời điểm tạo dữ liệu / truy vấn |
| `rooms` | `Array` | Danh sách thông tin theo từng phòng khám |
| `rooms[].roomId` | `UUID` | Khóa định danh phòng khám |
| `rooms[].roomNumber` | `String` | Mã phòng (ví dụ: `P101`) |
| `rooms[].roomName` | `String` | Tên phòng khám (ví dụ: `Phòng Khám Nội 1`) |
| `rooms[].doctorName` | `String` | Tên bác sĩ phụ trách phòng hôm nay |
| `rooms[].currentCalling` | `Object / null` | Bệnh nhân đang được gọi/khám tại phòng (`IN_PROGRESS`). `null` nếu phòng chưa gọi bệnh nhân nào. |
| `rooms[].waitingList` | `Array` | Danh sách tối đa 10 bệnh nhân đang chờ kế tiếp, đã sắp xếp ưu tiên. |
| `*.queueNumber` | `Integer` | Số thứ tự khám trong ngày |
| `*.patientInitials` | `String` | **Tên viết tắt** theo chuẩn QTN-43 (ví dụ: `Nguyễn Văn An` $\rightarrow$ `N. V. A`) |
| `*.status` | `String` | `IN_PROGRESS` (đang khám) hoặc `WAITING` (chờ khám) |
| `*.priority` | `String` | Mức ưu tiên: `EMERGENCY` (Cấp cứu), `PRIORITY` (Ưu tiên), `NORMAL` (Thường) |
| `*.calledAt` | `Instant / null` | Thời điểm bác sĩ gọi vào khám |

---

## 4. Quy Tắc Chuyển Đổi Tên Viết Tắt (QTN-43)

Hàm `PatientAnonymizer.abbreviateName(fullName)` thực hiện:
- Tách họ tên thành các từ.
- Lấy ký tự đầu tiên viết hoa của mỗi từ kèm dấu chấm và khoảng trắng phân cách.
- Ví dụ:
  - `Nguyễn Văn An` $\rightarrow$ `N. V. A`
  - `Lê Nam` $\rightarrow$ `L. N`
  - `Trần Thị Mai Phương` $\rightarrow$ `T. T. M. P`
- Đảm bảo **an toàn tuyệt đối**, không thể suy ngược ra thông tin cá nhân.
