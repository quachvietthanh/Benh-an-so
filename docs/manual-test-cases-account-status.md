# Kịch bản kiểm thử thủ công — Quản lý khóa/mở khóa tài khoản

## Tiền điều kiện chung

| Hạng mục | Giá trị |
|---|---|
| Trình duyệt | Chrome / Edge (phiên bản mới nhất) |
| Backend | Đã chạy `localhost:8080` (đã deploy Spring Boot) |
| Frontend | Đã chạy `localhost:5173` (đã chạy `npm run dev`) |
| Tài khoản Admin | `admin` / `Admin@123` (hoặc theo dữ liệu có sẵn) |
| Tài khoản Bác sĩ | `doctor1` / `Doctor@123` (hoặc theo dữ liệu có sẵn) |

---

## 🔹 Test Case 1: Admin khóa tài khoản bác sĩ thành công

**Mục tiêu:** Kiểm tra Admin có thể khóa (lock) tài khoản của người dùng khác.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập bằng tài khoản **Admin** | Đăng nhập thành công, vào được Dashboard |
| 2 | Truy cập trang **Quản lý tài khoản người dùng** | Hiển thị danh sách người dùng (có cột "Trạng thái" + "Thao tác") |
| 3 | Tìm dòng của user `doctor1`. Cột "Trạng thái" hiển thị **"Hoạt động"** (Tag màu xanh) | Thấy đúng thông tin bác sĩ |
| 4 | Cột "Thao tác", click nút **"Khóa"** (màu đỏ) | Hiển thị hộp thoại xác nhận Popconfirm: *"Bạn có chắc chắn muốn khóa tài khoản 'doctor1' không?"* |
| 5 | Click **"Khóa"** trong Popconfirm | Hiển thị thông báo: *"Đã khóa tài khoản 'doctor1' thành công"*. Bảng tự động tải lại |
| 6 | Quan sát lại dòng `doctor1` | Cột "Trạng thái" chuyển thành **"Bị khóa"** (Tag màu đỏ). Cột "Thao tác" hiện nút **"Mở khóa"** (xanh) |

---

## 🔹 Test Case 2: Tài khoản bị khóa không thể đăng nhập

**Mục tiêu:** Kiểm tra một user sau khi bị Admin khóa thì không thể đăng nhập vào hệ thống.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng xuất khỏi tài khoản Admin (nếu đang đăng nhập) | Về trang Login |
| 2 | Nhập tên đăng nhập của tài khoản đã bị khóa (VD: `doctor1`) và mật khẩu đúng | Click nút Đăng nhập |
| 3 | Quan sát kết quả | **Đăng nhập thất bại.** Hiển thị thông báo lỗi: *"Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên."* hoặc lỗi 403 |
| 4 | Xác nhận rằng không thể truy cập Dashboard | Dù có token cũ (nếu lưu trong localStorage) cũng không thể sử dụng |

---

## 🔹 Test Case 3: Admin mở khóa tài khoản + user đăng nhập lại được

**Mục tiêu:** Kiểm tra Admin có thể mở khóa tài khoản và user đó đăng nhập lại bình thường.

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập bằng tài khoản **Admin** | Đăng nhập thành công |
| 2 | Truy cập trang **Quản lý tài khoản người dùng** | Danh sách hiển thị |
| 3 | Tìm dòng user đã bị khóa (VD: `doctor1`). Cột "Trạng thái" hiển thị **"Bị khóa"** (Tag đỏ) | Đúng user cần mở khóa |
| 4 | Cột "Thao tác", click nút **"Mở khóa"** (màu xanh) | Hiển thị Popconfirm: *"Bạn có chắc chắn muốn mở khóa tài khoản 'doctor1' không?"* |
| 5 | Click **"Mở khóa"** trong Popconfirm | Hiển thị thông báo: *"Đã mở khóa tài khoản 'doctor1' thành công"*. Bảng tự động tải lại |
| 6 | Quan sát cột "Trạng thái" của `doctor1` | Chuyển thành **"Hoạt động"** (Tag xanh) |
| 7 | Đăng xuất Admin | Về trang Login |
| 8 | Đăng nhập bằng tài khoản `doctor1` với mật khẩu đúng | **Đăng nhập thành công.** Vào được Dashboard với quyền Bác sĩ |

---

## 🔹 Test Case bổ sung: Admin không thể tự khóa chính mình

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập bằng tài khoản **Admin** | Thành công |
| 2 | Vào trang Quản lý tài khoản người dùng | Danh sách hiển thị |
| 3 | Tìm dòng của Admin. Thử click nút **"Khóa"** | Popconfirm hiện ra |
| 4 | Xác nhận **"Khóa"** | Hiển thị lỗi: *"Bạn không thể tự khóa tài khoản của chính mình"*. Trạng thái không thay đổi |

---

## 🔹 Test Case bổ sung: Người dùng thường (không phải Admin) không có quyền khóa

| Bước | Thao tác | Kết quả mong đợi |
|---|---|---|
| 1 | Đăng nhập bằng tài khoản **Bác sĩ** (VD: `doctor1`) | Thành công |
| 2 | Truy cập trực tiếp URL: `http://localhost:3000/quan-ly-tai-khoan` (nếu có route) hoặc gọi API bằng Postman: `PUT http://localhost:8080/api/v1/admin/users/{id}/status?locked=true` với token của bác sĩ | Trả về lỗi **403 Forbidden** — *"Bạn không có quyền truy cập tài nguyên này"* |
