# KẾ HOẠCH TRIỂN KHAI KHẮC PHỤC BÁO CÁO REVIEW NCL-07-CN-008

## Giảm giá và miễn phí có phê duyệt (Invoice Discount & Free Approval Workflow)

> **Phân hệ:** `NCL-07` — Thu ngân và Xuất hóa đơn  
> **User Story:** `NCL-07-CN-008` — Giảm giá và miễn phí có phê duyệt  
> **Quy tắc nghiệp vụ liên quan:** `QTN-37` — Kiểm soát giảm giá / miễn phí và Tách biệt trách nhiệm (Separation of Duties - SoD), `QTN-09` — Điều chỉnh hóa đơn có vết / Bất biến chứng từ  
> **Acceptance Criteria:** `NCL-07-CN-008-TC-01`, `TC-02`, `TC-03`, `TC-04`  
> **Nhánh thực hiện:** `feature/approve-discount-and-free`  
> **Vai trò phê chuẩn:** Tech Lead  

---

## 1. Tóm tắt quyết định

### 1.1. Danh sách Finding xử lý và phân loại trạng thái

| Mã Finding | Phân loại | Mức độ | Trạng thái kỹ thuật | Quyết định xử lý |
| :--- | :--- | :--- | :--- | :--- |
| **Finding P0** | Migration Collision | **BLOCKER** | **Đã xác minh** | **Xử lý triệt để:** Hủy merge dở dang, reset branch trên `origin/develop`. Đổi tên migration thành **`V84__create_discount_requests_and_adjust_billing_constraints.sql`**. |
| **Finding P1** | Scope Contamination | **BLOCKER** | **Đã xác minh** | **Xử lý triệt để (Tách scope):** Reset và cherry-pick commit `b7a812ce` trên đỉnh `origin/develop`. 28 file ngoài phạm vi (Queue Priority) tự động bị loại bỏ khỏi PR diff. |
| **Finding P1** | Concurrency Race Condition | **BLOCKER** | **Đã xác minh** | **Xử lý triệt để (Phòng vệ 2 tầng):** Dùng `findByIdForUpdate` trên Visit ở tầng use case + STORED Generated Column `active_status` và ràng buộc `UNIQUE (visit_id, active_status)` ở tầng MySQL DB. Bắt `DataIntegrityViolationException` map sang `DiscountAlreadyExistsException` (409 Conflict). |
| **Finding P2** | Malformed Audit JSON | **MAJOR** | **Đã xác minh** | **Xử lý triệt để:** Bỏ string interpolation `.formatted(...)` trong `CreateDiscountRequestService`, `ApproveDiscountRequestService`, `RejectDiscountRequestService`. Sử dụng `ObjectMapper` để serialize payload audit an toàn, chống injection và lỗi cú pháp JSON. |
| **Finding P3** | Financial Reporting Semantics | **MINOR** | **Đã xác minh** | **Rà soát & Bổ sung regression tests:** Rà soát query báo cáo doanh thu (`amount_paid`). Bổ sung regression tests kiểm chứng 3 trường hợp: không giảm giá, giảm một phần, và miễn phí 100%. |
| **Finding P3** | MySQL CHECK Constraint Verification | **MINOR** | **Đã xác minh** | **Bổ sung runtime test MySQL:** Viết test integration chạy với MySQL xác thực dòng hóa đơn âm (`unit_price < 0`, `amount < 0`) và hóa đơn gốc 0 đồng (`total_amount = 0`) thỏa mãn toàn bộ CHECK constraint của MySQL 8.0. |

### 1.2. Danh sách conflict đã hợp nhất khi đồng bộ `origin/develop`

| STT | File xung đột | Nguyên nhân xung đột | Phương án xử lý thống nhất |
| :---: | :--- | :--- | :--- |
| 1 | `BillingAccessDeniedAuditWriter.java` | HEAD có `recordAccessDenied` (generic). Develop có `writePaymentDenied` (dùng `ObjectMapper`). | **Hợp nhất:** Giữ cả 2 method, chuẩn hóa `recordAccessDenied` dùng `ObjectMapper` và `Propagation.REQUIRES_NEW` để audit SoD discount không bị rollback khi ném ngoại lệ. |
| 2 | `RecordPaymentService.java` | HEAD có chặn pending discount và nạp approved discount. Develop có hỗ trợ nhiều phương thức thanh toán (`PaymentMethodItem`). | **Hợp nhất:** Giữ nguyên hỗ trợ nhiều phương thức thanh toán của develop; đồng thời nạp `discountAmount`, `discountRequestId` từ discount đã duyệt vào `Payment.record(...)`. Hỗ trợ các constructor overload an toàn với fallback `NO_OP_DISCOUNT_REPO`. |
| 3 | `ResourceType.java` | HEAD thêm `DISCOUNT_REQUEST`. Develop thêm `PATIENT_IMPORT`. | **Hợp nhất:** Giữ cả 2 giá trị enum trong `ResourceType`. |
| 4 | `Payment.java` | HEAD thêm `discountAmount`, `discountRequestId`. Develop thêm `paymentMethodItems`. | **Hợp nhất:** Entity `Payment` hỗ trợ cả hai. `validateAmountPaid` kiểm tra `amountPaid = max(0, totalAmount - discountAmount)`. Cung cấp đầy đủ các constructor overload để giữ tương thích ngược. |
| 5 | `InvoiceResult.java` | HEAD thêm discount fields. Develop thêm `PaymentDetailResult payment`. | **Hợp nhất:** Record chứa đủ cả `discountAmount`, `discountRequestId` và `PaymentDetailResult payment`. |
| 6 | `PaymentResult.java` | HEAD thêm discount fields. Develop thêm `paymentMethods` list. | **Hợp nhất:** Record chứa đủ cả `discountAmount`, `discountRequestId` và `List<PaymentMethodItemResult> paymentMethods`. |

---

## 2. Kế hoạch triển khai theo thứ tự (Dependency Order)

### Bước 1: Tầng Domain & Port Layer (Xử lý Finding P1, P3)
*   **Mục tiêu:** Hợp nhất các thuộc tính giảm giá (`discountAmount`, `discountRequestId`) và thanh toán đa phương thức (`paymentMethodItems`) vào Entity `Payment` và các DTO Results; chuẩn hóa các ngoại lệ domain.
*   **Finding / AC / BR liên quan:** Finding P1, Finding P3; Tiêu chí chấp nhận `NCL-07-CN-008-TC-01`, `TC-02`; Quy tắc `QTN-37`.
*   **Module / File thay đổi:**
    *   `com.benhsoan.domain.auditlog.enums.ResourceType`: Giữ cả `DISCOUNT_REQUEST` và `PATIENT_IMPORT`.
    *   `com.benhsoan.domain.billing.Payment`: Hợp nhất `discountAmount`, `discountRequestId` và `paymentMethodItems`. Cung cấp các constructor overload cho `record(...)` và `restore(...)` hỗ trợ các bài test cũ lẫn mới.
    *   `com.benhsoan.port.dto.result.InvoiceResult`: Bổ sung `BigDecimal discountAmount`, `UUID discountRequestId`, `PaymentDetailResult payment`.
    *   `com.benhsoan.port.dto.result.PaymentResult`: Bổ sung `BigDecimal discountAmount`, `UUID discountRequestId`, `List<PaymentMethodItemResult> paymentMethods`.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** `PaymentTest` pass 100% các kịch bản.

---

### Bước 2: Tầng Database Migration (Xử lý Finding P0, P1, P3)
*   **Mục tiêu:** Đổi số hiệu migration từ V78 sang **`V84`**; bổ sung cột ảo `active_status` (`STORED`) và ràng buộc duy nhất chống tạo trùng discount đang chờ/đã duyệt; nới lỏng CHECK constraints cho phép dòng discount âm và hóa đơn 0 đồng.
*   **Module / File thay đổi:**
    *   Xóa: `backend/src/main/resources/db/migration/V78__create_discount_requests_and_adjust_billing_constraints.sql`.
    *   Tạo mới: `backend/src/main/resources/db/migration/V84__create_discount_requests_and_adjust_billing_constraints.sql`.
*   **Nội dung DDL cụ thể:**
    1.  Tạo bảng `discount_requests` với cột `active_status VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'APPROVED') THEN 'ACTIVE' ELSE NULL END) STORED` và `CONSTRAINT uk_discount_requests_active_visit UNIQUE (visit_id, active_status)`.
    2.  Bảng `payments`: Thêm `discount_amount`, `discount_request_id`; sửa `chk_payments_amount_match` thành `CHECK (amount_paid = total_amount - discount_amount)`.
    3.  Bảng `invoices`: Thêm `discount_amount`, `discount_request_id`; sửa `chk_invoices_original_shape` thành `CHECK (total_amount >= 0 ...)`.
    4.  Bảng `invoice_lines`: Mở rộng `chk_invoice_lines_type` thêm `'DISCOUNT'`; sửa `chk_invoice_lines_amounts` cho phép dòng `DISCOUNT` có `unit_price <= 0` và `amount <= 0`.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** `mvn test-compile` thành công, kiểm thử Flyway MySQL pass.

---

### Bước 3: Tầng Persistence Layer (Xử lý Finding P1)
*   **Mục tiêu:** Bổ sung phương thức pessimistic lock `findByIdForUpdate`; bắt `DataIntegrityViolationException` khi vi phạm unique index và map sang `DiscountAlreadyExistsException` (409 Conflict).
*   **Module / File thay đổi:**
    *   `com.benhsoan.persistence.jpaRepository.billing.JpaDiscountRequestRepository`: Bổ sung `@Lock(LockModeType.PESSIMISTIC_WRITE) Optional<DiscountRequestEntity> findByIdForUpdate(@Param("id") UUID id);`.
    *   `com.benhsoan.persistence.adapterRepository.billing.DiscountRequestRepositoryAdapter`: Bọc `save()` trong try-catch `DataIntegrityViolationException` chuyển đổi thành `DiscountAlreadyExistsException(visitId)`.
    *   `com.benhsoan.persistence.mapper.billing.PaymentPersistenceMapper`: Ánh xạ đầy đủ trường dữ liệu.

---

### Bước 4: Tầng Application & Use Cases (Xử lý Finding P1, P2, SoD)
*   **Mục tiêu:** Khóa lượt khám chống race condition, chuẩn hóa ghi nhật ký kiểm toán bằng JSON với `ObjectMapper`, ngăn chặn vi phạm SoD tự duyệt và ghi nhận log `ACCESS_DENIED` độc lập qua `REQUIRES_NEW`.
*   **Module / File thay đổi:**
    *   `BillingAccessDeniedAuditWriter`: `@Transactional(propagation = Propagation.REQUIRES_NEW)` và tuần tự hóa JSON bằng `ObjectMapper`.
    *   `CreateDiscountRequestService`: Khóa `visitRepository.findByIdForUpdate(visitId)` trong transaction; dùng `ObjectMapper`.
    *   `ApproveDiscountRequestService`: Kiểm tra SoD tự duyệt; dùng `ObjectMapper`.
    *   `RejectDiscountRequestService`: Kiểm tra SoD tự từ chối; dùng `ObjectMapper`.
    *   `RecordPaymentService`: Chặn thanh toán khi pending discount; nạp approved discount vào payment; hỗ trợ thanh toán đa phương thức.
    *   `CreateInvoiceService`: Chặn hóa đơn khi pending discount; sinh dòng `InvoiceLine` loại `DISCOUNT` với giá trị âm.

---

### Bước 5: Tầng Adapter Inbound & REST Mappers (Xử lý Finding P1, P2)
*   **Mục tiêu:** Ánh xạ mã lỗi HTTP chuẩn: `403` cho SoD, `409` cho Pending/Duplicate discount, `400` cho vượt quá tổng tiền.
*   **Module / File thay đổi:**
    *   `DomainExceptionHttpStatusMapper`: Cập nhật ánh xạ cho các domain exception mới.
    *   `BillingRestMapper`: Hợp nhất mapper đầy đủ.

---

### Bước 6: Git Cherry-pick/Rebase & Scope Cleanup (Xử lý Finding P1 Blocker)
*   **Mục tiêu:** Loại bỏ hoàn toàn 28 file queue priority thuộc NCL-03-CN-013 khỏi diff so sánh với `origin/develop`.
*   **Thao tác thực hiện:** Đã reset và cherry-pick commit `b7a812ce` trên đỉnh `origin/develop`, giải quyết sạch 6 conflict. Diff hiện tại chỉ còn duy nhất 64 file thuộc phạm vi thanh toán và giảm giá.

---

### Bước 7: Bổ sung và chạy Suite Kiểm thử Tự động Toàn diện
*   **Mục tiêu:** Cung cấp đầy đủ bằng chứng kiểm thử tự động (test evidence) cho toàn bộ các finding và acceptance criteria.
*   **Kế hoạch chạy test:**
    ```bash
    mvn test -Dtest=DiscountRequest*Test,RecordPaymentServiceTest,CreateDiscountRequestServiceTest,ApproveDiscountRequestServiceTest,RejectDiscountRequestServiceTest,PaymentTest,InvoiceTest,BillingAccessDeniedAuditWriterTest
    ```

---

## 3. Thiết kế chi tiết cho từng finding

### 3.1. Thiết kế xử lý Finding P0: Flyway Migration Collision
- Nhánh `origin/develop` đã có migration từ `V78` đến `V83`.
- Đổi migration của tính năng thành **`V84__create_discount_requests_and_adjust_billing_constraints.sql`**.

### 3.2. Thiết kế xử lý Finding P1: Scope Contamination & Race Condition
- **Scope:** Cherry-pick trên `origin/develop` đã tự động làm sạch 28 file queue.
- **Race Condition:**
  - *Tầng 1 (Java):* `visitRepository.findByIdForUpdate(visitId)` khóa dòng lượt khám trong transaction.
  - *Tầng 2 (Database):* Cột `active_status VARCHAR(20) GENERATED ALWAYS AS (CASE WHEN status IN ('PENDING', 'APPROVED') THEN 'ACTIVE' ELSE NULL END) STORED` cùng ràng buộc `CONSTRAINT uk_discount_requests_active_visit UNIQUE (visit_id, active_status)`.
  - *Tầng 3 (Exception Translation):* Bắt `DataIntegrityViolationException` đổi thành `DiscountAlreadyExistsException` trả về HTTP `409 Conflict`.

### 3.3. Thiết kế xử lý Finding P2: Malformed Audit JSON
- Inject `ObjectMapper` vào tất cả các service liên quan đến discount và audit writer.
- Thay thế hoàn toàn chuỗi text block `.formatted(...)` bằng `objectMapper.writeValueAsString(...)`.

### 3.4. Thiết kế xử lý Finding P3: Financial Reporting Semantics & MySQL Constraints
- `amount_paid = total_amount - discount_amount` là chuẩn mực kế toán chính xác (phản ánh thực thu).
- Điều chỉnh CHECK constraints trong `V84` để MySQL 8.0 chấp thuận hóa đơn 0 đồng (`total_amount >= 0`) và dòng giảm giá âm (`unit_price <= 0`, `amount <= 0`).

---

## 4. Ma trận kiểm thử (Test Matrix)

| STT | Mã Test Case | Lớp kiểm thử | Given / When / Then | Finding / AC liên quan |
| :---: | :--- | :--- | :--- | :--- |
| **1** | `createDiscountRequest_withSpecialCharacters_serializesValidJson` | Unit Test (`CreateDiscountRequestServiceTest`) | **Given:** Request giảm giá có reason chứa ký tự `"`, `\`, `\n`.<br>**When:** Tạo đề xuất giảm giá.<br>**Then:** Audit log detail là chuỗi JSON hợp lệ, parse được bằng ObjectMapper. | Finding P2 |
| **2** | `approveDiscountRequest_whenApproverIsRequester_throws403AndAudits` | Unit Test (`ApproveDiscountRequestServiceTest`) | **Given:** Người phê duyệt trùng với người yêu cầu (`actorId.equals(requestedBy)`).<br>**When:** Thực hiện duyệt đề xuất.<br>**Then:** Ném `SelfApprovalNotAllowedException` (403), audit log ghi nhận `ACCESS_DENIED`. | QTN-37 / AC TC-03 |
| **3** | `rejectDiscountRequest_whenApproverIsRequester_throws403AndAudits` | Unit Test (`RejectDiscountRequestServiceTest`) | **Given:** Người từ chối trùng với người yêu cầu.<br>**When:** Thực hiện từ chối đề xuất.<br>**Then:** Ném `SelfApprovalNotAllowedException` (403), audit log ghi nhận `ACCESS_DENIED`. | QTN-37 / AC TC-03 |
| **4** | `recordPayment_whenPendingDiscountExists_throws409Conflict` | Unit Test (`RecordPaymentServiceTest`) | **Given:** Lượt khám đang có đề xuất giảm giá ở trạng thái `PENDING`.<br>**When:** Thu ngân cố tình ghi nhận thanh toán.<br>**Then:** Ném `PendingDiscountApprovalException` (HTTP 409 Conflict). | AC TC-01 |
| **5** | `recordPayment_withApprovedDiscount_calculatesAmountPaidCorrectly` | Unit Test (`RecordPaymentServiceTest`) | **Given:** Viện phí 500.000 VNĐ, có đề xuất giảm giá 100.000 VNĐ đã duyệt.<br>**When:** Thực hiện thanh toán với số tiền 400.000 VNĐ.<br>**Then:** Thành công, `payment.amountPaid == 400.000`, `payment.discountAmount == 100.000`. | AC TC-01 |
| **6** | `recordPayment_with100PercentFree_recordsZeroAmountPaid` | Unit Test (`PaymentTest`) | **Given:** Lượt khám được duyệt miễn phí 100% (`discountAmount == totalAmount`).<br>**When:** Ghi nhận thanh toán với `amountPaid = 0`.<br>**Then:** Thành công, `amountPaid == 0`, `isRecorded() == true`. | Finding P3 / AC TC-02 |
| **7** | `createInvoice_withApprovedDiscount_addsNegativeDiscountLine` | Unit Test (`CreateInvoiceServiceTest`) | **Given:** Đề xuất giảm 150.000 VNĐ đã được duyệt.<br>**When:** Xuất hóa đơn cho lượt khám.<br>**Then:** Hóa đơn có dòng `InvoiceLine` loại `DISCOUNT` với `amount == -150.000`, tổng tiền giảm trừ đúng 150.000. | AC TC-01 |
| **8** | `accessDeniedAuditLog_survivesTransactionRollback` | Integration Test (`BillingAccessDeniedAuditWriterTest`) | **Given:** Transaction nghiệp vụ chính bị rollback do ném ngoại lệ SoD.<br>**When:** `recordAccessDenied` chạy với `Propagation.REQUIRES_NEW`.<br>**Then:** Log kiểm toán `ACCESS_DENIED` vẫn được lưu thành công trong cơ sở dữ liệu. | QTN-37 |
| **9** | `concurrentCreateDiscountRequests_allowsOnlyOneToSucceed` | Concurrency Test (`CreateDiscountRequestConcurrencyMySqlIntegrationTest`) | **Given:** 1 lượt khám chưa có discount. Bắn đồng thời 2 luồng tạo discount cho cùng `visitId`.<br>**When:** 2 luồng chạy song song qua `CountDownLatch`.<br>**Then:** Đúng 1 luồng thành công (201), 1 luồng nhận lỗi `DiscountAlreadyExistsException` (409 Conflict). Chỉ có duy nhất 1 bản ghi DB. | Finding P1 |
| **10** | `mysqlFlywayMigrationV84_supportsNegativeDiscountAndZeroTotal` | MySQL Test (`InvoiceDiscountFlywayMySqlIntegrationTest`) | **Given:** Database MySQL 8.4 áp dụng migration `V84`.<br>**When:** Chèn hóa đơn có `total_amount = 0` và dòng `DISCOUNT` có số tiền `-200.000.00`.<br>**Then:** Thực thi thành công, toàn bộ CHECK constraints hợp lệ trên MySQL engine. | Finding P0, P3 |
| **11** | `revenueQueries_calculateAccuratelyWithDiscounts` | Regression Test (`JpaPaymentRepositoryTest`) | **Given:** Các khoản thu bình thường, có giảm giá, và miễn phí 100%.<br>**When:** Gọi `sumAmountPaidBetween(...)`.<br>**Then:** Doanh thu thực thu khớp chính xác tổng `amount_paid`. | Finding P3 |
| **12** | `securityIntegration_requiresRoleManagerOrAdminForApproval` | Security Test (`DiscountSecurityIntegrationTest`) | **Given:** User có role `RECEPTIONIST` hoặc `DOCTOR`.<br>**When:** Gọi POST `/invoices/discount-requests/{id}/approve`.<br>**Then:** Trả về HTTP 403 Forbidden. | AC TC-04 |

---

## 5. Rollout và Kế hoạch Kiểm chứng (Rollout & Verification)

### 5.1. Kế hoạch Triển khai (Deployment)
1. **Pre-deployment Check:** Kiểm tra `flyway_schema_history` trên môi trường đích, xác nhận version hiện tại là `V83`.
2. **Deploy Application:** Khởi động Spring Boot container, Flyway tự động áp dụng `V84__create_discount_requests_and_adjust_billing_constraints.sql`.
3. **Post-deployment Smoke Test:** Xác nhận `/actuator/health` đạt `UP`, kiểm tra số lượng bản ghi và thực hiện request mẫu.

### 5.2. Kế hoạch Rollback Dự phòng
Hủy các CHECK constraints và khôi phục constraints cũ; xóa các cột liên kết ngoại và xóa bảng `discount_requests`, cập nhật `flyway_schema_history`.

### 5.3. Tiêu chuẩn nghiệm thu đóng Finding (Definition of Done)
1. **P0 (Migration Collision):** Migration đổi thành `V84`, build và khởi động không có warning hay lỗi checksum.
2. **P1 (Scope Contamination):** `git diff origin/develop...HEAD --name-only` chỉ chứa đúng các file của tính năng, 0 file queue.
3. **P1 (Race Condition):** Cơ chế 2 tầng hoạt động hoàn hảo; concurrency test chứng minh không thể tạo 2 discount active đồng thời.
4. **P2 (Malformed Audit JSON):** 100% audit log trong discount flow được serialize bằng `ObjectMapper`.
5. **P3 (Payment & Reporting Semantics):** Toàn bộ regression test về thanh toán và báo cáo doanh thu pass 100%.
6. **P3 (MySQL Constraints):** Test MySQL Testcontainers xác nhận nới lỏng CHECK constraint cho dòng discount âm và hóa đơn 0 đồng thành công.
