# Kế hoạch triển khai & Khắc phục lỗi: Người liên hệ khẩn cấp của bệnh nhân (NCL-02-CN-007)

Tài liệu này tổng hợp phân tích kỹ thuật của Tech Lead và kế hoạch triển khai xử lý toàn bộ các phát hiện (findings) từ báo cáo review mã nguồn cho tính năng **Người liên hệ khẩn cấp của bệnh nhân** (`NCL-02-CN-007`) thuộc Epic `NCL-02`.

---

## 1. Tóm tắt quyết định kỹ thuật (Tech Lead Decision)

### 1.1. Bảng đối chiếu xử lý các Findings

| Mã Finding | Mức độ | Trạng thái | Hướng xử lý kỹ thuật |
| :--- | :--- | :--- | :--- |
| **P1.01** | P1 (Nghiêm trọng) | **Xử lý** | Mở rộng regex tại `UpdatePatientRequest.java` cho phép chuỗi mask `[0-9]{2}\*{6}[0-9]{2}` và chuỗi rỗng `""`. Tầng Use Case (`UpdatePatientService`) phục hồi giá trị unmask và validate logic. |
| **P1.02** | P1 (Nghiêm trọng) | **Xử lý** | Cập nhật regex `@Pattern` tại `RegisterPatientRequest.java` và `UpdatePatientRequest.java` sử dụng group `(?:...)?` để chấp nhận chuỗi rỗng `""` và `null`. |
| **P2.01** | P2 (Trung bình) | **Xử lý** | Bổ sung câu lệnh backfill dữ liệu `UPDATE patients SET emergency_relationship = 'Người thân' WHERE emergency_contact IS NOT NULL AND emergency_relationship IS NULL;` vào migration `V51`. Bổ sung fallback trong Use Case. |
| **P2.02** | P2 (Trung bình) | **Xử lý** | Bổ sung handler cho `ValidationException` trong `GlobalExceptionHandler.java`, phân tích chuỗi `fieldName: errorDescription` nạp vào `details.fields` theo đúng chuẩn `docs/exception-conventions.md`. |
| **P2.03** | P2 (Trung bình) | **Đã xử lý (FIXED)** | Đồng bộ tài liệu Workbook `project-workbook.xlsx` (row 85 & task 384), ban hành RFC-007 chuẩn hóa quy tắc Cohesive Triplet, và bổ sung test cases kiểm chứng lưu vết khi xóa trong `UpdatePatientServiceTest.java`. |
| **P3.01** | P3 (Nhẹ) | **Xử lý** | Thêm Bean Validation `@Size(max = 50, message = "Mối quan hệ không được vượt quá 50 ký tự.")` cho `emergencyRelationship` ở cả 2 Request DTOs. |
| **P3.02** | P3 (Nhẹ) | **Xử lý** | Bổ sung WebMvc test case `getMedicalRecordDetailReturnsEmergencyContactForDoctor` trong `MedicalRecordControllerTest.java` kiểm tra `GET /medical-records/visits/{visitId}` trả về đủ 3 trường người liên hệ khẩn cấp. |

### 1.2. Các giả định và xác minh (Verification Status)
- **Đã xác minh**: Regex `^(?:(0|\+84)(3|5|7|8|9)[0-9]{8}|[0-9]{2}\*{6}[0-9]{2})?$` tương thích hoàn toàn với chuẩn JSR-380, nhận diện đúng cả SĐT thật, SĐT mask (`09******78`), chuỗi rỗng `""` và `null`.
- **Đã xác minh**: Tầng Use Case (`UpdatePatientService:118-120`) đã có sẵn logic `PatientAnonymizer.isMaskedPhone(emergencyPhone)` để khôi phục dữ liệu gốc nếu giá trị gửi lên là chuỗi mask.
- **Đã xác minh**: Bảng `patients` trong migration `V4__seed_patients.sql` có 10 bản ghi mẫu có `emergency_contact` và `emergency_phone`, nhưng thiếu `emergency_relationship`.
- **Đã xác minh**: `GlobalExceptionHandler.java` trước đây bắt `ValidationException` qua `handleDomainException`, trả về `details: {}` rỗng khiến frontend không trích xuất được `details.fields`.
- **Đã xác minh (P2.03)**: Mâu thuẫn giữa câu chữ Postcondition trong Workbook và Cohesive Triplet đã được giải quyết triệt để: cập nhật trực tiếp `project-workbook.xlsx` (row 85 & task 384), ban hành `docs/rfc-ncl-02-cn-007-emergency-contact-postcondition.md`, và bổ sung bộ test kiểm chứng trong `UpdatePatientServiceTest.java`.

---

## 2. Thiết kế chi tiết cho từng Finding

### Finding P1.01 & P1.02: Bean Validation chặn chuỗi Mask và chuỗi rỗng trên `emergencyPhone`
- **Root cause**: 
  - Regex cũ `^(0|\+84)(3|5|7|8|9)[0-9]{8}$` yêu cầu chuỗi tối thiểu 10 chữ số.
  - Khi bật chế độ ẩn danh trình diễn (QTN-43), client nhận `09******78` và submit lại form; Bean Validation ném `MethodArgumentNotValidException` (HTTP 400), làm dead code logic unmask ở Use Case.
  - Khi form web submit chuỗi rỗng `""` (thay vì null), Bean Validation đánh giá chuỗi rỗng không khớp regex -> trả về 400.
- **Giải pháp được chọn**:
  - Tại `RegisterPatientRequest.java`:
    ```java
    @Pattern(
            regexp = "^(?:(0|\\+84)(3|5|7|8|9)[0-9]{8})?$",
            message = "Số điện thoại không đúng định dạng."
    )
    String emergencyPhone
    ```
  - Tại `UpdatePatientRequest.java`:
    ```java
    @Pattern(
            regexp = "^(?:(0|\\+84)(3|5|7|8|9)[0-9]{8}|[0-9]{2}\\*{6}[0-9]{2})?$",
            message = "Số điện thoại không đúng định dạng."
    )
    String emergencyPhone
    ```
- **Lợi ích**:
  - Giữ nguyên error response contract chuẩn ở tầng REST (`400 Bad Request` kèm field `emergencyPhone`).
  - Cho phép chuỗi rỗng `""` và chuỗi mask `09******78` vượt qua filter Bean Validation an toàn để tầng Use Case xử lý.

### Finding P2.01: Deadlock dữ liệu cũ (Legacy Data Backfill)
- **Root cause**: Migration `V51` thêm cột `emergency_relationship` nhưng để giá trị `NULL` cho toàn bộ dữ liệu hiện hữu. Khi cập nhật hồ sơ cũ, Use Case kiểm tra Cohesive Triplet thấy có tên và SĐT nhưng thiếu mối quan hệ nên văng `ValidationException`.
- **Giải pháp được chọn**:
  - Cập nhật file migration `V51__add_emergency_relationship_to_patients.sql`:
    ```sql
    ALTER TABLE patients ADD COLUMN emergency_relationship VARCHAR(50) NULL;

    UPDATE patients
    SET emergency_relationship = 'Người thân'
    WHERE emergency_contact IS NOT NULL AND emergency_relationship IS NULL;
    ```
  - Bổ sung fallback phòng vệ trong `UpdatePatientService.java`: tự động giữ lại mối quan hệ đã có nếu client gửi null trên hồ sơ cũ có sẵn người liên hệ.

### Finding P2.02: Error Response của `ValidationException` thiếu `details.fields`
- **Root cause**: 
  - `docs/exception-conventions.md:31` yêu cầu validation field errors phải được đọc từ `details.fields`.
  - Khi Use Case ném `ValidationException("emergencyPhone: Số điện thoại không đúng định dạng.")`, `GlobalExceptionHandler` bắt qua `handleDomainException`, trả về `details: {}`.
- **Giải pháp được chọn**:
  - Bổ sung `@ExceptionHandler(ValidationException.class)` trong `GlobalExceptionHandler.java`:
    Trích xuất `fieldName` và `errorDescription` từ message dạng `fieldName: errorDescription` và đưa vào `details.fields`.
  - Nếu message không theo dạng này, `details` trả về rỗng, đảm bảo tương thích ngược 100%.

### Finding P2.03: Mâu thuẫn nghiệp vụ Cohesive Triplet vs Postcondition Workbook
- **Root cause**: Workbook ban đầu ghi "Hồ sơ có ít nhất một người liên hệ khẩn cấp", mâu thuẫn với quy tắc Cohesive Triplet cho phép xóa sạch người liên hệ khi có nhu cầu chính đáng.
- **Giải pháp xử lý (Đã hoàn tất)**:
  1. Ban hành tài liệu [RFC-NCL-02-CN-007-01](file:///f:/Java/Benh-so-an/docs/rfc-ncl-02-cn-007-emergency-contact-postcondition.md) phân tích và chuẩn hóa quy tắc Cohesive Triplet.
  2. Cập nhật trực tiếp `project-workbook.xlsx` (Sheet `Product Backlog` row 85, Col 11 và Sheet `Tasks` row 384, Col 5) thành: *"Nếu ghi nhận người liên hệ khẩn cấp thì phải có đầy đủ bộ ba thông tin và mọi thay đổi đều lưu vết."*
  3. Bổ sung các unit test trong `UpdatePatientServiceTest.java` kiểm chứng: xóa trắng cả 3 trường thành công và lưu vết đầy đủ vào `patient_change_logs`, nhưng nếu chỉ xóa 1 hoặc 2 trường thì bị chặn lập tức bởi Cohesive Triplet.

### Finding P3.01: Thiếu Bean Validation `@Size(max = 50)` cho `emergencyRelationship`
- **Root cause**: Cột DB là `VARCHAR(50)`. Nếu client gửi chuỗi > 50 ký tự, lỗi ném ra từ tầng DB là `DataIntegrityViolationException` (500/409) thay vì `400 Bad Request`.
- **Giải pháp**: Bổ sung `@Size(max = 50, message = "Mối quan hệ không được vượt quá 50 ký tự.")` vào cả `RegisterPatientRequest` và `UpdatePatientRequest`.

### Finding P3.02: Thiếu Integration Test ở Controller cho Bác sĩ xem Bệnh án
- **Root cause**: Đã có unit test cho mapper nhưng thiếu test end-to-end tầng MockMvc Controller để đảm bảo endpoint `GET /medical-records/visits/{visitId}` serialize đầy đủ các trường người liên hệ khẩn cấp của bệnh nhân.
- **Giải pháp**: Thêm test case `getMedicalRecordDetailReturnsEmergencyContactForDoctor` trong `MedicalRecordControllerTest.java`.

---

## 3. Kế hoạch triển khai mã nguồn theo thứ tự

### Bước 1: Database Migration (P2.01)
- **File**: `backend/src/main/resources/db/migration/V51__add_emergency_relationship_to_patients.sql`
- **Nội dung**: Bổ sung lệnh `UPDATE patients SET emergency_relationship = 'Người thân' WHERE emergency_contact IS NOT NULL AND emergency_relationship IS NULL;`.

### Bước 2: Application Services & Normalization (P1.01, P2.01, P2.02)
- **File**: `backend/src/main/java/com/benhsoan/application/ucservice/patient/UpdatePatientService.java`
- **File**: `backend/src/main/java/com/benhsoan/application/ucservice/patient/RegisterPatientService.java`
- **Nội dung**:
  - Chuẩn hóa `normalizePhone` trả về `null` thay vì `""`.
  - Thêm fallback giữ `emergencyRelationship` cho bệnh nhân cũ hoặc chế độ ẩn danh.
  - Đảm bảo `ValidationException` có dạng `fieldName: errorDescription`.

### Bước 3: REST Request DTOs & Global Exception Handler (P1.01, P1.02, P2.02, P3.01)
- **File 1**: `backend/src/main/java/com/benhsoan/adapter/inbound/rest/request/patient/RegisterPatientRequest.java`
  - Thêm `@Size(max = 50)` cho `emergencyRelationship`.
  - Cập nhật `@Pattern` cho `emergencyPhone`.
- **File 2**: `backend/src/main/java/com/benhsoan/adapter/inbound/rest/request/patient/UpdatePatientRequest.java`
  - Thêm `@Size(max = 50)` cho `emergencyRelationship`.
  - Cập nhật `@Pattern` cho `emergencyPhone` (cho phép cả chuỗi mask và chuỗi rỗng).
- **File 3**: `backend/src/main/java/com/benhsoan/exception/GlobalExceptionHandler.java`
  - Thêm `@ExceptionHandler(ValidationException.class)` để parse `details.fields`.

### Bước 4: Kiểm thử Unit, Controller & Integration (P1.01, P1.02, P2.01, P2.02, P3.01, P3.02)
- **File 1**: `backend/src/test/java/com/benhsoan/exception/GlobalExceptionHandlerTest.java`
  - Test mapping `ValidationException` dạng `field: message` sang `details.fields`.
- **File 2**: `backend/src/test/java/com/benhsoan/adapter/inbound/rest/controller/PatientEmergencyContactIntegrationTest.java`
  - Test update với chuỗi mask `09******78` (P1.01).
  - Test chấp nhận chuỗi rỗng `""` trên `emergencyPhone` (P1.02).
  - Test từ chối `emergencyRelationship` dài hơn 50 ký tự (P3.01).
- **File 3**: `backend/src/test/java/com/benhsoan/adapter/inbound/rest/controller/MedicalRecordControllerTest.java`
  - Test `getMedicalRecordDetailReturnsEmergencyContactForDoctor` (P3.02).

### Bước 5: Chạy toàn bộ Test Suite & Hồi quy
- Chạy `mvn test "-Dtest=*Patient*Test,*MedicalRecord*Test,GlobalExceptionHandlerTest"`
- Đảm bảo 100% test cases pass.
