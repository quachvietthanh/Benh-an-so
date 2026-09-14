# Implementation Plan: Khắc phục Review Findings tính năng Xác nhận lịch hẹn (NCL-03-CN-008)

Kế hoạch giải quyết toàn bộ các findings và khiếm khuyết được chỉ ra trong Báo cáo Review Backend cho tính năng Xác nhận lịch hẹn (`NCL-03-CN-008`), tuân thủ Clean Architecture, bảo toàn dữ liệu, chống race condition và giữ backward compatibility.

## 1. TÓM TẮT QUYẾT ĐỊNH

### 1.1. Danh sách Finding tiếp nhận xử lý trong đợt này
| Mã Finding | Mức độ | Trạng thái kỹ thuật | Quyết định xử lý |
| :--- | :---: | :---: | :--- |
| **Finding 1** | **P1** | [Đã xác minh] | **Tiếp nhận xử lý**: Bổ sung phân giải họ tên người xác nhận (`confirmedByName`) trong `GetAppointmentByIdService` khi tra cứu chi tiết lịch hẹn qua API `GET /appointments/{id}`. |
| **Finding 2** | **P2** | [Đã xác minh] | **Tiếp nhận xử lý**: Tinh chỉnh logic query tại `GetUnconfirmedAppointmentsService` và `AppointmentBusinessSpecification` để khi tra cứu ngày hiện tại (`today`), chỉ lấy các lịch hẹn có `startTime > now` (chưa quá giờ khám). |
| **Finding 3** | **P2** | [Đã xác minh] | **Tiếp nhận xử lý**: Cập nhật ma trận phân quyền trong `docs/permission-matrix.md` và biên soạn hợp đồng API chuẩn tại `docs/api/appointment-confirm-contract.md`. |
| **Finding 4** | **P3** | [Đã xác minh] | **Tiếp nhận xử lý**: Bổ sung unit test cho `writeConfirmDenied` trong `AppointmentAccessDeniedAuditWriterTest` và test case nhánh rẽ `date == null` trong `GetUnconfirmedAppointmentsServiceTest`. |
| **Thiếu sót Mục VI** | **P2/P3** | [Đã xác minh] | **Tiếp nhận xử lý**: Bổ sung test Double Confirmation (ném `409 CONFLICT` khi đã `CONFIRMED`) cho cả luồng Lễ tân và Cổng bệnh nhân; bổ sung test hiển thị `confirmedByName` trong `GetAppointmentByIdServiceTest`. |

### 1.2. Danh mục KHÔNG xử lý trong đợt này và lý do
| Hạng mục / Lỗi | Nguồn gốc | Lý do từ chối xử lý trong phạm vi này |
| :--- | :---: | :--- |
| **2 failures tại `FullClinicalEncounterWorkflowE2EIntegrationTest`** | Báo cáo review mục V.2 | **Lỗi nhánh nền (`develop`)**: Do thiếu quyền `QUEUE_VIEW` khi gọi `GET /queues/me` từ tài khoản Bác sĩ trong script seed data nền của commit cũ, hoàn toàn không liên quan đến logic xác nhận lịch hẹn của `NCL-03-CN-008`. Xử lý riêng trong task bảo trì dữ liệu nền. |
| **Cột mới trong Database** | Schema DB | [Đã xác minh] Migration `V47__add_confirmation_fields_to_appointments.sql` đã đạt chuẩn: cột cho phép `NULL`, có khóa ngoại và chỉ mục đầy đủ, đã migrate thành công. Không phát sinh script Flyway mới. |

### 1.3. Các giả định và câu hỏi cần làm rõ trước khi code
1. **[Cần xác minh] Phạm vi hiển thị `confirmedByName` trong danh sách tìm kiếm (`SearchAppointmentsService`)**:
   - *Phân tích*: Finding 1 có trích dẫn file `SearchAppointmentsService.java:L24`. Tuy nhiên, API `GET /appointments` là API phân trang (Page). Nếu thực hiện resolve tên người xác nhận cho từng dòng trong vòng lặp bằng `userRepository.findById()` sẽ dẫn đến lỗi hiệu năng **N+1 Query**.
   - *Giả định lựa chọn*: Đợt này chỉ xử lý triệt để tại `GetAppointmentByIdService` (màn hình chi tiết lịch hẹn - nơi cần hiển thị thông tin người xác nhận theo `NCL-03-CN-008-TC-01`). Đối với `SearchAppointmentsService`, tiếp tục giữ nguyên `null`.
2. **[Giả định] Hành vi của API `GET /appointments/unconfirmed` khi người dùng truyền ngày trong quá khứ (`date < today`)**:
   - *Giả định lựa chọn*: User Story xác định đây là "danh sách chưa xác nhận trước giờ khám được liệt kê để gọi nhắc". Khi người dùng truyền ngày quá khứ, toàn bộ lịch của ngày đó đều đã quá giờ khám (`startTime < now`). Do đó, hệ thống sẽ trả về danh sách rỗng (Empty Page) thay vì ném lỗi, đảm bảo an toàn cho giao diện người dùng.

---

## 2. KẾ HOẠCH TRIỂN KHAI THEO THỨ TỰ DEPENDENCY

### Bước 1: Tầng Persistence (Database Query & Specifications)
- **File**: `backend/src/main/java/com/benhsoan/persistence/jpaRepository/appointment/AppointmentBusinessSpecification.java`
- **Thay đổi**: Cập nhật Specification `unconfirmedOnDate(Instant fromTime, Instant toTime)` sử dụng `cb.greaterThan(root.get("startTime"), fromTime)` và `cb.lessThanOrEqualTo(root.get("startTime"), toTime)`.

### Bước 2: Tầng Application / Use Case (Business Logic & Orchestration)
- **File**: `backend/src/main/java/com/benhsoan/application/ucservice/appointment/GetAppointmentByIdService.java`
  - Inject `UserRepository userRepository`.
  - Phân giải tên người xác nhận nếu `appointment.getConfirmedBy() != null`.
- **File**: `backend/src/main/java/com/benhsoan/application/ucservice/appointment/GetUnconfirmedAppointmentsService.java`
  - So sánh `targetDate` với `today` (theo múi giờ `Asia/Ho_Chi_Minh`).
  - Nếu `targetDate.isBefore(today)`: trả về `PageImpl<>(List.of(), pageable, 0)`.
  - Nếu `targetDate.isEqual(today)`: `fromTime = clockPort.now()`.
  - Nếu `targetDate.isAfter(today)`: `fromTime = startOfDay.minusMillis(1)`.
  - `toTime = endOfDay`.

### Bước 3: Tầng Testing (Kiểm thử đơn vị)
- Bổ sung test suites cho:
  - `GetAppointmentByIdServiceTest`: test case `confirmedByName`.
  - `GetUnconfirmedAppointmentsServiceTest`: test case `date == null`, `date` quá khứ.
  - `AppointmentAccessDeniedAuditWriterTest`: test case `writeConfirmDenied`.
  - `ConfirmAppointmentServiceTest`: test case Double Confirmation (409 Conflict).
  - `PatientConfirmAppointmentServiceTest`: test case Double Confirmation (409 Conflict).

### Bước 4: Tầng Tài liệu hóa (Documentation)
- Cập nhật `docs/permission-matrix.md`.
- Tạo mới `docs/api/appointment-confirm-contract.md`.
