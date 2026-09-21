# Quy chuẩn phát triển giao diện (UI Guidelines)

Khi xây dựng hoặc chỉnh sửa giao diện người dùng (Frontend) trong toàn bộ dự án, luôn tuân thủ nghiêm ngặt 3 nguyên tắc sau:

---

### 1. Thuần 100% Tiếng Việt (Tuyệt đối không dùng nửa Anh nửa Việt)
* **Toàn bộ giao diện người dùng** bao gồm:
  - Tiêu đề trang, tiêu đề modal/drawer/card.
  - Nhãn form (`label`), nhãn cột bảng (`title`).
  - Nút bấm (`Button text`), các nút xác nhận / hủy bỏ.
  - Placeholder ô nhập liệu, ô chọn (`Select`, `Input`, `TextArea`).
  - Hướng dẫn phụ (`Tooltip`), chú thích, mô tả mẫu.
  - Thông báo hệ thống (`message.success`, `message.error`, `Alert`, `Notification`).
* **Bắt buộc phải sử dụng 100% Tiếng Việt chuẩn, tự nhiên, rõ ràng**.
* **Tuyệt đối không** dùng nửa Anh nửa Việt (ví dụ: placeholder tiếng Anh, button tiếng Anh, tooltip pha trộn). 
* *Ngoại lệ duy nhất*: Các mã định danh kỹ thuật chuẩn hóa quốc tế hoặc mã hệ thống (như mã phòng `P101`, mã ICD-10 `J00`, mã chuyên khoa `PEDIATRICS`).

---

### 2. Chuẩn hóa kích thước nút bấm to rõ ràng (Tất cả các nút trên giao diện)
* **Nút bấm trong bảng danh mục (Table Action Buttons)**:
  - Chuẩn kích thước tối thiểu **36px × 36px** (border-radius: 8px).
  - Icon bên trong nút phải có kích thước rõ ràng (**16px – 18px**), sử dụng nền màu nhạt (tint background) và viền tương ứng với loại hành động (Xem: xanh lam nhạt, Sửa: vàng cam nhạt, Nguy hiểm / Ngừng dùng: đỏ nhạt, Kích hoạt: xanh lá nhạt).
  - **Tuyệt đối không** sử dụng nút nhỏ xíu (`size="small"`, `type="text"` mờ nhạt không viền không nền) gây khó quan sát và khó click.
* **Nút bấm trong Modal / Form / Thanh công cụ**:
  - Nút xác nhận (`Lưu thay đổi`, `Tạo mới`, `Xác nhận`): `size="large"` hoặc chiều cao tối thiểu **40px – 42px**, độ rộng tối thiểu 120px, font chữ 15px đậm nét.
  - Nút đóng/hủy (`Hủy bỏ`, `Đóng`): chiều cao **40px – 42px**, độ rộng tối thiểu 100px.

---

### 3. Không thêm dòng mô tả phụ dưới tiêu đề trang
* Phần Header trang chỉ hiển thị Icon và Tiêu đề chính (`Title`), giữ giao diện tối giản, thoáng và sạch sẽ.
* **Tuyệt đối không** thêm dòng mô tả phụ dài dòng, không hiển thị mã User Story / Ticket (ví dụ: `NCL-09-CN-xxx`, chú thích nghiệp vụ, phân công trách nhiệm) bên dưới tiêu đề trang.

---

### 4. Tuyệt đối không tự ý sửa Backend (Chỉ sửa khi người dùng yêu cầu cụ thể)
* **Chỉ được phép chỉnh sửa mã nguồn Backend** (Java, Spring Boot, Flyway migration, SQL, cấu hình database, Controller, Service, Repository...) khi người dùng **yêu cầu rõ ràng và trực tiếp**.
* Khi gặp lỗi hoặc thiếu hụt từ phía API Backend:
  - Phân tích nguyên nhân và giải thích rõ ràng cho người dùng.
  - Xử lý linh hoạt, an toàn ở tầng Frontend (xử lý dữ liệu fallback, hiển thị thông báo thân thiện hoặc mock data dự phòng nếu cần).
  - Đề xuất hướng dẫn cho team Backend sửa nếu cần, tuyệt đối không tự ý can thiệp file backend khi chưa có chỉ thị.

