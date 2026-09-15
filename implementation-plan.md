# Kế hoạch Triển khai: Lịch tuần theo bác sĩ dạng bảng (NCL-03-CN-010)

Kế hoạch kỹ thuật triển khai backend hoàn chỉnh cho User Story `NCL-03-CN-010`: **Lịch tuần theo bác sĩ dạng bảng** thuộc Epic `NCL-03` (Lịch hẹn và hàng đợi khám), đảm bảo đáp ứng đầy đủ Acceptance Criteria (TC-01, TC-02, TC-03, TC-04), bảo toàn kiến trúc Hexagonal Architecture, không gây lỗi hồi quy và tuân thủ các quy tắc nghiệp vụ (QTN-04, QTN-30, QTN-01).

---

## 1. TÓM TẮT YÊU CẦU NGHIỆP VỤ VÀ GIẢI PHÁP KỸ THUẬT

### 1.1. Mục tiêu và Phạm vi
* **Mục tiêu**: Xây dựng endpoint backend cung cấp ma trận lưới thời gian (grid/table) lịch hẹn cả tuần của các bác sĩ cho Lễ tân và Quản lý phòng khám; hỗ trợ quan sát khung giờ trống, khung giờ đã có lịch hẹn (kèm trạng thái tô màu), khung giờ khoảng nghỉ của bác sĩ; cho phép tạo lịch hẹn trực tiếp từ ô trống đúng quy tắc chống trùng lịch (QTN-04) và trong lịch làm việc (QTN-30); chặn truy cập trái quyền và ghi log kiểm toán (TC-04).
* **Phạm vi**: Chỉ backend (Domain, Ports, DTOs, Application Service, REST API, Security & Audit, Tests). Không thay đổi frontend.

### 1.2. Phân tích Yêu cầu, Role và Business Rules
* **Vai trò được phép**: Lễ tân (`RECEPTIONIST`), Quản lý phòng khám (`CLINIC_MANAGER` / `ADMIN`).
* **Vai trò bị chặn**: Dược sĩ (`PHARMACIST`) và các tài khoản không có quyền `APPOINTMENT_READ`.
* **Business Rules áp dụng**:
  * `QTN-04: Không trùng lịch một bác sĩ`: Một bác sĩ không được có hai lịch hẹn trong cùng khung giờ.
  * `QTN-30: Đặt lịch trong lịch làm việc của bác sĩ`: Chỉ được đặt lịch hẹn vào khung giờ nằm trong lịch làm việc của bác sĩ và không trùng khoảng nghỉ đã đăng ký.
  * `QTN-01: Phân quyền truy cập theo vai trò`: Kiểm soát quyền truy cập chặt chẽ qua `@RequirePermission("APPOINTMENT_READ")`.

### 1.3. Acceptance Criteria (Tiêu chí chấp nhận)
* **`NCL-03-CN-010-TC-01 (Luồng thành công)`**:
  * *Given*: Đã có lịch làm việc và lịch hẹn của các bác sĩ.
  * *When*: Lễ tân mở lịch tuần.
  * *Then*: Bảng hiển thị đúng lịch hẹn theo cột bác sĩ và hàng khung giờ.
* **`NCL-03-CN-010-TC-02 (Luồng thành công)`**:
  * *Given*: Đang xem lịch tuần.
  * *When*: Lễ tân bấm vào một ô trống để tạo lịch.
  * *Then*: Lịch hẹn được tạo đúng bác sĩ và khung giờ của ô đó qua `POST /appointments`.
* **`NCL-03-CN-010-TC-03 (Sai trạng thái)`**:
  * *Given*: Ô thuộc khoảng nghỉ của bác sĩ (time-off / leave).
  * *When*: Lễ tân bấm tạo lịch trên ô đó.
  * *Then*: Hệ thống chặn theo `QTN-30` và báo bác sĩ không làm việc trong khung giờ đó (`DoctorNotWorkingException`).
* **`NCL-03-CN-010-TC-04 (Không có quyền)`**:
  * *Given*: Người đăng nhập là Dược sĩ (`PHARMACIST`).
  * *When*: Mở màn hình lịch tuần / gọi API lịch tuần.
  * *Then*: Hệ thống từ chối truy cập (HTTP 403) và ghi nhật ký kiểm toán (`ActionType.ACCESS_DENIED`, `ResourceType.PERMISSION`).

### 1.4. Thiết kế Kỹ thuật Backend
1. **API Endpoint**:
   * `GET /appointments/doctor-weekly-table`
   * Query params:
     * `date` (LocalDate, tùy chọn, mặc định là ngày hiện tại). Backend tính tuần từ Thứ Hai 00:00:00 đến Chủ Nhật 23:59:59.999 theo múi giờ `Asia/Ho_Chi_Minh`.
     * `doctorId` (UUID, tùy chọn, lọc riêng 1 bác sĩ nếu cần).
2. **DTO & Model**:
   * Enum `SlotAvailabilityStatus`: `AVAILABLE`, `BOOKED`, `ON_LEAVE`, `OFF_DUTY`, `PAST`.
   * Result & Response models chuẩn hóa: ngày trong tuần, bác sĩ, khung giờ slot 30 phút, thông tin tóm tắt lịch hẹn (`AppointmentSummary`), lý do khoảng nghỉ.
3. **Application Service (`GetDoctorWeeklyScheduleTableService`)**:
   * Batch query thông tin tuần để tránh N+1: Doctors, Weekly Schedules, Specific Date Schedules, Active Time-offs, Active Appointments, Patient Profiles.
   * Tính toán từng ô slot 30 phút cho mỗi bác sĩ theo từng ngày trong tuần.
4. **Tạo lịch trên ô trống & Kiểm tra vi phạm**:
   * Tái sử dụng `POST /appointments` (`CreateAppointmentService`), vốn đã có sẵn kiểm tra `DoctorScheduleValidator` (QTN-30) và `AppointmentRepository.existsActiveAppointmentConflict` (QTN-04).
5. **Phân quyền & Kiểm toán**:
   * Bảo vệ endpoint bằng `@RequirePermission("APPOINTMENT_READ")`.
   * `RequirePermissionAspect` tự động ném `AccessDeniedException` và ghi `AuditLog` `ACCESS_DENIED` khi Dược sĩ truy cập.

---

## 2. KẾ HOẠCH TRIỂN KHAI THEO TỪNG GIAI ĐOẠN

### Giai đoạn 1: Thiết kế API Contract, DTOs & Inbound Port
* **Mục tiêu**: Định nghĩa cấu trúc hợp đồng dữ liệu chuẩn hóa cho bảng lịch tuần theo bác sĩ.
* **Files / Layers tác động**:
  * `[NEW]` `backend/src/main/java/com/benhsoan/domain/appointment/enums/SlotAvailabilityStatus.java`
  * `[NEW]` `backend/src/main/java/com/benhsoan/port/inbound/appointment/GetDoctorWeeklyScheduleTableUseCase.java`
  * `[NEW]` `backend/src/main/java/com/benhsoan/port/dto/query/appointment/GetDoctorWeeklyScheduleTableQuery.java`
  * `[NEW]` `backend/src/main/java/com/benhsoan/port/dto/result/appointment/DoctorWeeklyTableResult.java`
  * `[NEW]` `backend/src/main/java/com/benhsoan/adapter/inbound/rest/response/appointment/DoctorWeeklyTableResponse.java`
* **Quy tắc**: Các model bất biến (records), phân tách rõ các trạng thái của ô lịch (`AVAILABLE`, `BOOKED`, `ON_LEAVE`, `OFF_DUTY`, `PAST`).
* **Tiêu chí verify/test**: `mvn test-compile` thành công.

### Giai đoạn 2: Xây dựng Application Service & Thuật toán Tính toán Slot
* **Mục tiêu**: Hiện thực hóa use case truy vấn lịch tuần bác sĩ với hiệu năng tối ưu, tính toán chính xác ca làm việc, khoảng nghỉ và lịch hẹn.
* **Files / Layers tác động**:
  * `[NEW]` `backend/src/main/java/com/benhsoan/application/ucservice/appointment/GetDoctorWeeklyScheduleTableService.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/port/outbound/repository/appointment/AppointmentRepository.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/persistence/adapterRepository/appointment/AppointmentRepositoryAdapter.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/persistence/jpaRepository/appointment/JpaAppointmentRepository.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/port/outbound/repository/appointment/DoctorTimeOffRepository.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/persistence/adapterRepository/appointment/DoctorTimeOffRepositoryAdapter.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/persistence/jpaRepository/appointment/JpaDoctorTimeOffRepository.java`
* **Quy tắc**:
  * Chuẩn hóa tuần Thứ 2 -> Chủ nhật theo `Asia/Ho_Chi_Minh`.
  * Tôn trọng thứ tự ưu tiên lịch làm việc (Weekly schedule làm gốc, ngày cụ thể ghi đè).
  * Tuân thủ `QTN-30`: Phản ánh đúng khoảng nghỉ active.
  * Tuân thủ `QTN-04`: Loại trừ lịch hẹn đã hủy (`CANCELLED`).
* **Tiêu chí verify/test**: Unit tests cho service bao phủ các kịch bản slot: trống, có lịch, nghỉ phép, ngoài giờ, quá khứ.

### Giai đoạn 3: Tích hợp REST Controller, Mapper & Bảo mật
* **Mục tiêu**: Mở endpoint REST API `GET /appointments/doctor-weekly-table`, tích hợp mapping và kiểm soát quyền hạn.
* **Files / Layers tác động**:
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/adapter/inbound/rest/controller/AppointmentController.java`
  * `[MODIFY]` `backend/src/main/java/com/benhsoan/adapter/inbound/rest/mapper/AppointmentRestMapper.java`
* **Quy tắc**:
  * Endpoint được bảo vệ bởi `@RequirePermission("APPOINTMENT_READ")`.
  * Cho phép Lễ tân (`RECEPTIONIST`) và Quản lý phòng khám (`CLINIC_MANAGER` / `ADMIN`) truy cập.
  * Dược sĩ (`PHARMACIST`) bị chặn 403 Forbidden và `RequirePermissionAspect` ghi nhật ký `ACCESS_DENIED`.
* **Tiêu chí verify/test**: MockMvc tests kiểm tra phân quyền và ánh xạ response.

### Giai đoạn 4: Xây dựng Bộ Kiểm thử Tự động Toàn diện (Đảm bảo 4 ACs)
* **Mục tiêu**: Đảm bảo 100% các tiêu chí chấp nhận trong workbook đều được tự động hóa kiểm thử và bảo toàn không lỗi hồi quy.
* **Files / Layers tác động**:
  * `[NEW]` `backend/src/test/java/com/benhsoan/application/ucservice/appointment/GetDoctorWeeklyScheduleTableServiceTest.java`
  * `[NEW]` `backend/src/test/java/com/benhsoan/adapter/inbound/rest/controller/DoctorWeeklyTableIntegrationTest.java`
* **Quy tắc kiểm thử**:
  * TC-01: Bảng hiển thị đúng lịch hẹn theo cột bác sĩ và hàng khung giờ trong tuần.
  * TC-02: Bấm tạo lịch trên ô trống thành công đúng bác sĩ và khung giờ.
  * TC-03: Cố tình tạo lịch trên ô khoảng nghỉ bị chặn theo QTN-30.
  * TC-04: Dược sĩ truy cập bị từ chối và ghi nhật ký kiểm toán vi phạm.
* **Tiêu chí verify/test**: Toàn bộ test suite chạy pass 100% (`mvn test`).

---

## 3. KẾT QUẢ TRIỂN KHAI VÀ KHẮC PHỤC 5 FINDINGS CODE REVIEW

Sau đợt review chuyên sâu, toàn bộ 5 finding đã được xử lý triệt để:

| Finding | Nội dung lỗi | Giải pháp triển khai | Files thay đổi chính | Trạng thái |
| :--- | :--- | :--- | :--- | :---: |
| **[P1 - Blocker]** | Lỗi biên dịch `AppointmentController` do endpoint `/available-slots` bị sót/lạc scope. | Xóa bỏ hoàn toàn endpoint `GET /appointments/available-slots` và method test tương ứng, đưa controller về đúng scope NCL-03-CN-010. | `AppointmentController.java`, `AppointmentControllerTest.java` | **FIXED** |
| **[P3 - Rule]** | Xác nhận biểu diễn `COMPLETED` và `NO_SHOW` trên weekly table. | Giữ nguyên kiến trúc 2 tầng ổn định: Slot status = `BOOKED` (khóa ô theo QTN-04), chi tiết cuộc hẹn trong `appointment.status` (phục vụ tô màu UI). | `GetDoctorWeeklyScheduleTableService.java` | **VERIFIED** |
| **[P1 - Prior]** | Xung đột phân giải ca khi lịch tuần bị tắt (`active = false`). | Quy tắc: Lịch ngày cụ thể (`DoctorSchedule`) ghi đè lịch tuần (`DoctorWeeklySchedule`). | `DoctorScheduleValidator.java`, `DoctorWeeklyScheduleRepository.java`, `GetDoctorWeeklyScheduleTableService.java` | **FIXED** |
| **[P2-1]** | Lịch hẹn cũ bị ẩn khi slot rơi vào ngoài giờ (`OFF_DUTY`). | Đẩy kiểm tra `matchingAppt` lên đầu vòng lặp slot; hiển thị `BOOKED`, `isBookable = false` kèm thông tin cuộc hẹn. | `GetDoctorWeeklyScheduleTableService.java` | **FIXED** |
| **[P2-2]** | Thiếu kiểm tra role Bác sĩ khi truyền `doctorId`. | Bổ sung kiểm tra `!RoleConstants.DOCTOR.equals(doctor.getRoleId())` -> ném 404 `DoctorNotFoundException`. | `GetDoctorWeeklyScheduleTableService.java` | **FIXED** |
| **[P3-1]** | Chưa hỗ trợ chế độ ẩn danh (NCL-15-CN-003). | Bổ sung `patientCode`; tiêm `AnonymizationModeState` vào `AppointmentRestMapper`, áp dụng `PatientAnonymizer.maskFullName` và `maskPhone`. | `DoctorWeeklyTableResult.java`, `DoctorWeeklyTableResponse.java`, `AppointmentRestMapper.java`, `AppointmentRestMapperTest.java` | **FIXED** |
| **[P3-2]** | Trùng tên biến (Variable Shadowing). | Đổi tên tham số `buildSlotsForDay` thành `doctorAppointments`. | `GetDoctorWeeklyScheduleTableService.java` | **FIXED** |

**Xác nhận kiểm thử tự động thực tế sau khi fix:**
- Targeted Test Suites (6 suites): **47/47 tests PASS** (`BUILD SUCCESS`).
- Toàn bộ Backend Test Suite: **1639/1639 tests PASS** (26 skipped, 0 failures, 0 errors, `BUILD SUCCESS`).
