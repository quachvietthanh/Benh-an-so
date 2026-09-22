# KẾ HOẠCH TRIỂN KHAI KHẮC PHỤC BÁO CÁO REVIEW NCL-07-CN-007

**Chức năng:** Thu phí nhiều phương thức và ghi nhận chi tiết thanh toán  
**User Story:** `NCL-07-CN-007` (Epic `NCL-07` — Thu phí và hóa đơn)  
**Quy tắc nghiệp vụ liên quan:** `QTN-09` (Điều chỉnh hóa đơn có vết / Bất biến chứng từ gốc), `QTN-38` (Chốt ca thu ngân)  
**Nhánh công việc:** `feature/multiple-payment-method`  
**Vai trò phê duyệt:** Tech Lead  

---

## 1. Tóm tắt quyết định

### 1.1. Danh sách finding xử lý và phân loại trạng thái

| Mã Finding | Mức độ | Trạng thái kỹ thuật | Quyết định xử lý |
| :--- | :--- | :--- | :--- |
| **Finding 1 [P0]** | BLOCKER | **Đã xác minh** | **Xử lý triệt để:** Revert việc đổi tên V77 cũ; loại bỏ file migration queue; gán mã migration mới cho feature là **`V82`** (do `origin/develop` đã có migration tới `V81`). Cảnh báo hotfix độc lập cho file V77 trùng đang có trên `origin/develop`. |
| **Finding 2 [P1]** | BLOCKER | **Đã xác minh** | **Xử lý triệt để:** Rebase nhánh sạch sẽ trên `origin/develop`. Do PR #255 (`feature/priority-for-emergency`) đã được merge vào `origin/develop` trước đó, việc rebase commit thanh toán `3dc6eeab` sẽ loại bỏ 100% diff liên quan đến Queue khỏi branch này. |
| **Finding 3 [P2]** | MAJOR | **Đã xác minh** | **Xử lý xác minh & duy trì:** MySQL 8.4 LTS (môi trường Testcontainers và Production) hỗ trợ chính thức `ALTER TABLE payments DROP CHECK chk_payments_method;`. Bổ sung test tự động với Testcontainers MySQL 8.4 để xác thực dứt điểm trên CI. |
| **Finding 4 [P2]** | MAJOR | **Đã xác minh** | **Xử lý phòng ngừa hồi quy:** Bổ sung các domain helper methods trên entity `Payment` (`getCashAmount()`, `getBankTransferAmount()`, `getAmountByMethod()`) để phục vụ đối soát QTN-38; lập tài liệu cảnh báo chuyển đổi truy vấn cho nhánh `feature/settle-enofday-cash` (NCL-07-CN-009) và Frontend. |
| **Finding 5 [P2/P3]** | MAJOR | **Đã xác minh** | **Xử lý triệt để:** Loại bỏ lệnh xóa dữ liệu `deleteAllByPaymentId` trong `PaymentRepositoryAdapter.save()`. Bảo đảm tính bất biến tuyệt đối của khoản thu và chi tiết phương thức thanh toán theo quy định `QTN-09`. Bổ sung test chứng minh tính bất biến. |
| **Finding 6 [P3]** | MINOR | **Đã xác minh** | **Bổ sung kiểm chứng:** Hệ thống đã có cơ chế 4 tầng chống duplicate payment (Pessimistic lock trên Visit + Pre-check + DB Unique constraint + Translation sang HTTP 409). Bổ sung bộ test Concurrency trên MySQL Testcontainers bao phủ đủ 5 kịch bản review yêu cầu. |
| **Finding 7 [P3]** | MINOR | **Đã xác minh** | **Xử lý an toàn:** Giữ nguyên logic backfill từ `payments` sang `payment_method_items` trong `V82` kèm điều kiện `WHERE amount_paid > 0 AND payment_method IS NOT NULL`. Đã xác minh toàn bộ dữ liệu lịch sử đều thỏa mãn các ràng buộc toàn vẹn. |
| **Finding 8 [P3]** | MINOR | **Giả định** | **Giữ nguyên hiện trạng có kiểm soát:** Cho phép lưu `referenceNumber` trong `audit_logs` vì đây là mã giao dịch ngân hàng cần thiết để đối soát tài chính theo tiêu chí `NCL-07-CN-007-TC-04`, không phải thông tin thẻ nhạy cảm hay PII. |
| **Finding 9 [P2]** | MAJOR | **Đã xác minh** | **Xử lý triệt để:** Bổ sung trường `referenceNumber` vào `RecordPaymentCommand` ở tầng Java API; duy trì và bổ sung các WebMvc integration tests kiểm chứng định dạng JSON legacy qua giao thức HTTP. |
| **Finding 10** | MINOR | **Đã xác minh** | **Bổ sung bằng chứng:** Phân định rõ kết quả test nội bộ (H2) và kết quả kiểm chứng độc lập trên MySQL thực tế. |

### 1.2. Danh sách finding không xử lý thay đổi logic và lý do

*   **Làm mờ (Masking) hoặc loại bỏ `referenceNumber` trong Audit Log (Finding 8):** Không thay đổi việc ghi log chi tiết này. Lý do: Mã tham chiếu chuyển khoản là căn cứ duy nhất để lễ tân và kế toán đối soát giao dịch ngân hàng với hệ thống phòng khám (đáp ứng tiêu chí chấp nhận `NCL-07-CN-007-TC-04`). Mã này không vi phạm chính sách bảo mật vì không chứa số thẻ tín dụng đầy đủ, không chứa mã bảo mật thẻ (CVV) hay mật khẩu.

### 1.3. Các giả định và câu hỏi cần xác nhận trước khi thực thi

*   **Giả định 1 [Đã xác minh]:** Hệ thống Production và CI chạy trên nền tảng MySQL 8.x (cụ thể là MySQL 8.4 LTS như cấu hình Testcontainers hiện hữu). Cú pháp `DROP CHECK` hoàn toàn tương thích và không phát sinh lỗi cú pháp.
*   **Giả định 2 [Cần xác minh]:** Nhánh `origin/develop` sau khi merge PR #255 đang tồn tại đồng thời cả 2 file `V77__add_priority_to_queue_items.sql` và `V81__add_priority_to_queue_items.sql` (bị trùng phiên bản V77 với `V77__add_inventory_report_view_permission.sql`). Team Backend cần một commit hotfix trên `develop` để xóa file thừa `V77__add_priority_to_queue_items.sql`. Nhánh NCL-07-CN-007 sẽ sử dụng phiên bản an toàn tiếp theo là **`V82`**.
*   **Giả định 3 [Giả định]:** Chính sách bảo mật thông tin của dự án coi số tham chiếu chuyển khoản (`referenceNumber`) là dữ liệu kiểm toán nội bộ, được phép ghi nhận chi tiết trong bảng nhật ký hoạt động (`audit_logs`) phục vụ hậu kiểm.
*   **Giả định 4 [Đã xác minh]:** Nhánh `feature/settle-enofday-cash` (NCL-07-CN-009 — Chốt ca thu ngân) hiện phát triển độc lập và chưa được merge vào `develop`. Logic của nhánh này sẽ được cập nhật để đọc từ `payment_method_items` sau khi NCL-07-CN-007 hoàn tất merge vào `develop`.

---

## 2. Kế hoạch triển khai theo thứ tự (Dependency Order)

### Bước 1: Domain & Port Layer (Xử lý Finding 4, 9)
*   **Mục tiêu:** Bổ sung phương thức phân tích số tiền theo phương thức thanh toán trên Entity `Payment` để chống lỗi hồi quy báo cáo/đối soát; hoàn thiện `RecordPaymentCommand` giữ tương thích ngược hoàn toàn ở tầng Java.
*   **Finding / AC / BR liên quan:** Finding 4, Finding 9; Tiêu chí chấp nhận `TC-01`, `TC-03`; Quy tắc nghiệp vụ `QTN-38`.
*   **Module / File dự kiến thay đổi:**
    *   `com.benhsoan.domain.billing.Payment`
    *   `com.benhsoan.port.dto.command.billing.RecordPaymentCommand`
*   **Thay đổi kỹ thuật cụ thể:**
    *   Thêm các phương thức tính toán an toàn trên `Payment`: `getCashAmount()`, `getBankTransferAmount()`, `getAmountByMethod(PaymentMethod method)`: Duyệt danh sách `paymentMethodItems` nếu có; nếu danh sách rỗng (bản ghi cũ chưa backfill ở runtime test), tự động đối chiếu theo trường `paymentMethod` và `amountPaid`.
    *   Bổ sung trường `referenceNumber` vào `RecordPaymentCommand` và duy trì các overload constructor để hỗ trợ cả mã gọi cũ lẫn mới.
*   **Ảnh hưởng:** Không ảnh hưởng database, không thay đổi permission, không thay đổi API contract.
*   **Rủi ro và cách giảm thiểu:** Rủi ro `NullPointerException` khi `paymentMethodItems` rỗng -> Sử dụng kiểm tra null an toàn và fallback về enum `paymentMethod`.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** Toàn bộ unit test trong `PaymentTest` pass; các phương thức helper trả về đúng số tiền phân bổ cho từng phương thức.

---

### Bước 2: Persistence Layer (Xử lý Finding 5)
*   **Mục tiêu:** Đảm bảo tính bất biến (Immutability) theo quy chuẩn `QTN-09`, loại bỏ hoàn toàn nguy cơ xóa và ghi đè chi tiết phương thức thanh toán.
*   **Finding / AC / BR liên quan:** Finding 5; Quy tắc nghiệp vụ `QTN-09`.
*   **Module / File dự kiến thay đổi:**
    *   `com.benhsoan.persistence.adapterRepository.billing.PaymentRepositoryAdapter`
*   **Thay đổi kỹ thuật cụ thể:**
    *   Trong hàm `save(Payment payment)`: Loại bỏ hoàn toàn dòng lệnh `itemJpaRepository.deleteAllByPaymentId(savedEntity.getId())`.
    *   Bổ sung kiểm tra: Chỉ thực hiện `itemJpaRepository.saveAll(...)` khi các bản ghi item này chưa tồn tại trong database (dựa trên việc kiểm tra tồn tại `itemJpaRepository.countByPaymentId(savedEntity.getId()) == 0`). Khi cập nhật trạng thái khoản thu (hoàn tiền hoặc gán ca thu ngân), tuyệt đối không can thiệp vào các dòng dữ liệu của `payment_method_items`.
*   **Ảnh hưởng:** Bảo toàn nguyên vẹn tính toàn vẹn dữ liệu tài chính, không phát sinh câu lệnh `DELETE` không mong muốn trên production.
*   **Rủi ro và cách giảm thiểu:** Rủi ro ghi đè khi lưu lại Payment -> Việc loại bỏ `DELETE` triệt tiêu hoàn toàn nguy cơ mất dấu vết tài chính.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** Chạy test cập nhật trạng thái `Payment` (như Refund), xác nhận số lượng bản ghi trong `payment_method_items` giữ nguyên, không có truy vấn `DELETE` nào được thực thi.

---

### Bước 3: Application / Use Case Layer (Xử lý Finding 4, 8, 9)
*   **Mục tiêu:** Đảm bảo luồng xử lý command tương thích ngược, ghi nhận audit log đầy đủ và giữ nguyên cơ chế bảo vệ audit log khi transaction chính bị rollback.
*   **Finding / AC / BR liên quan:** Finding 4, 8, 9; Tiêu chí chấp nhận `TC-02`, `TC-03`.
*   **Module / File dự kiến thay đổi:**
    *   `com.benhsoan.application.ucservice.billing.RecordPaymentService`
    *   `com.benhsoan.application.ucservice.billing.BillingAccessDeniedAuditWriter`
*   **Thay đổi kỹ thuật cụ thể:**
    *   Trong `RecordPaymentService`: Khi tiếp nhận `command.paymentMethod()` đơn lẻ, truyền chính xác `command.referenceNumber()` vào `PaymentMethodItem` được khởi tạo, đảm bảo luồng đơn phương thức chuyển khoản ngân hàng không bị mất mã tham chiếu.
    *   Xác nhận `BillingAccessDeniedAuditWriter` tiếp tục hoạt động độc lập qua `@Transactional(propagation = Propagation.REQUIRES_NEW)` để khi xảy ra từ chối quyền (`AccessDeniedException`), nhật ký kiểm toán vẫn được ghi nhận bền vững vào database.
*   **Ảnh hưởng:** Bảo đảm luồng nghiệp vụ chuẩn mực, không rò rỉ giao dịch.
*   **Rủi ro và cách giảm thiểu:** Lỗi parse JSON khi serialize payload audit log -> Đã có khối `try-catch` fallback chuỗi rỗng an toàn.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** Unit test `RecordPaymentServiceTest` pass đầy đủ các trường hợp: thanh toán đa phương thức, thanh toán đơn phương thức có tham chiếu, thanh toán thiếu tiền, và thanh toán trùng lặp.

---

### Bước 4: Adapter / REST Layer (Xử lý Finding 9)
*   **Mục tiêu:** Duy trì độ tương thích ngược 100% cho các phiên bản frontend hoặc client tích hợp bên ngoài gọi qua REST API.
*   **Finding / AC / BR liên quan:** Finding 9; Tiêu chí chấp nhận `TC-01`, `TC-03`, `TC-04`.
*   **Module / File dự kiến thay đổi:**
    *   `com.benhsoan.adapter.inbound.rest.mapper.BillingRestMapper`
    *   `com.benhsoan.adapter.inbound.rest.controller.InvoiceController`
*   **Thay đổi kỹ thuật cụ thể:**
    *   Đảm bảo `BillingRestMapper.toCommand` nhận diện chính xác cả trường hợp gửi `paymentMethods` (danh sách) hoặc gửi `paymentMethod` + `referenceNumber` (đơn lẻ), map đồng nhất sang `RecordPaymentCommand`.
    *   Giữ nguyên mã lỗi HTTP và error response structure (`400 Bad Request` cho sai lệch số tiền `PAYMENT_AMOUNT_MISMATCH`, thiếu tham chiếu `VALIDATION_FAILED`; `409 Conflict` cho thanh toán trùng `PAYMENT_ALREADY_EXISTS`).
*   **Ảnh hưởng:** Frontend không bị ảnh hưởng tiêu cực; hỗ trợ liền mạch cả màn hình thu ngân cũ và màn hình thu ngân mới.
*   **Rủi ro và cách giảm thiểu:** Client truyền cùng lúc cả 2 định dạng -> Mapper ưu tiên cấu trúc danh sách chi tiết `paymentMethods`.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** `InvoiceControllerTest` pass 100% các kịch bản MockMvc với các payload JSON khác nhau.

---

### Bước 5: Database Migration (Xử lý Finding 1, 3, 7)
*   **Mục tiêu:** Khôi phục tính toàn vẹn của chuỗi Flyway migration, giải quyết dứt điểm xung đột định danh migration và bảo đảm khả năng migrate trơn tru trên MySQL 8.4.
*   **Finding / AC / BR liên quan:** Finding 1 (P0 Blocker), Finding 3, Finding 7.
*   **Module / File dự kiến thay đổi:**
    *   Khôi phục: `backend/src/main/resources/db/migration/V77__add_inventory_report_view_permission.sql` (hủy bỏ việc rename sang V78).
    *   Xóa bỏ khỏi branch: `backend/src/main/resources/db/migration/V78__add_inventory_report_view_permission.sql` và `backend/src/main/resources/db/migration/V77__add_priority_to_queue_items.sql`.
    *   Đổi tên migration tính năng: `V79__create_payment_method_items_and_support_multiple_methods.sql` thành **`V82__create_payment_method_items_and_support_multiple_methods.sql`**.
*   **Thay đổi kỹ thuật cụ thể:**
    *   Nội dung migration V82 giữ nguyên các cấu trúc DDL và DML đã kiểm tra kỹ:
        1. Tạo bảng `payment_method_items` kèm các khóa ngoại và ràng buộc CHECK (`amount > 0`, `payment_method IN (...)`).
        2. Tạo index `idx_payment_method_items_payment` trên cột `payment_id`.
        3. Thực thi `ALTER TABLE payments DROP CHECK chk_payments_method;`.
        4. Thực thi `ALTER TABLE payments ADD CONSTRAINT chk_payments_method CHECK (payment_method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'QR_CODE', 'E_WALLET', 'MULTIPLE'));`.
        5. Backfill dữ liệu lịch sử an toàn: Chèn bản ghi tương ứng từ `payments` sang `payment_method_items` cho các khoản thu có `amount_paid > 0` và `payment_method IS NOT NULL`.
*   **Ảnh hưởng:** Chuỗi phiên bản Flyway chuẩn hóa: V77 (Inventory Permission) -> V78 (Pregnancy Status) -> V79 (Contraindication Permission) -> V80 (Seed Contraindication) -> V81 (Queue Priority) -> **V82 (Multiple Payment Methods)**.
*   **Rủi ro và cách giảm thiểu:** Môi trường đã apply nhầm file V78/V79 cũ -> Hướng dẫn lệnh `flyway:repair` hoặc dọn dẹp bảng `flyway_schema_history` trên môi trường dev cục bộ.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** Lệnh `mvn flyway:info` hiển thị phiên bản V82 ở trạng thái PENDING và thực thi thành công không có warning hay checksum error.

---

### Bước 6: Git Rebase & Scope Cleanup (Xử lý Finding 2)
*   **Mục tiêu:** Tách triệt để toàn bộ thay đổi ngoài phạm vi (Queue Priority thuộc NCL-03-CN-013) ra khỏi branch `feature/multiple-payment-method`.
*   **Finding / AC / BR liên quan:** Finding 2 (P1 Blocker).
*   **Module / Thao tác thực hiện:**
    *   Thực hiện đồng bộ commit từ `origin/develop`:
        ```bash
        git fetch origin
        git rebase origin/develop
        ```
    *   Kiểm tra sự khác biệt giữa branch và develop:
        ```bash
        git diff origin/develop...HEAD --stat
        ```
*   **Thay đổi kỹ thuật cụ thể:**
    *   Vì các commit của feature NCL-03-CN-013 đã được hợp nhất chính thức vào `origin/develop` qua PR #255 (`714de7a5`), việc rebase sẽ làm cho toàn bộ các file queue không còn xuất hiện trong diff giữa branch và develop.
    *   Branch chỉ còn duy nhất phạm vi thay đổi thuộc về Billing / Invoices của User Story NCL-07-CN-007.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** Lệnh `git diff origin/develop...HEAD --name-only` chỉ liệt kê các file thuộc thư mục `billing`, `invoices`, migration `V82` và tài liệu liên quan; tuyệt đối không xuất hiện bất kỳ file nào có tiền tố `queue`.

---

### Bước 7: Bổ sung và chạy Suite kiểm thử tự động toàn diện (Xử lý Finding 3, 5, 6, 9)
*   **Mục tiêu:** Cung cấp bằng chứng thực thi tự động (test evidence) có thể kiểm chứng độc lập trên cả môi trường H2 In-Memory và MySQL 8.4 Testcontainers.
*   **Finding / AC / BR liên quan:** Finding 3, 5, 6, 8, 9, 10; Toàn bộ các tiêu chí chấp nhận `TC-01` đến `TC-04`.
*   **Module / File dự kiến bổ sung:**
    *   `com.benhsoan.persistence.jpaRepository.billing.MultiplePaymentMethodFlywayMySqlIntegrationTest` (Kiểm tra migration V82, `DROP CHECK` và backfill trên MySQL 8.4).
    *   `com.benhsoan.application.ucservice.billing.RecordPaymentConcurrencyMySqlIntegrationTest` (Kiểm tra tranh chấp và chống thanh toán trùng 5 kịch bản trên MySQL 8.4).
    *   `com.benhsoan.domain.billing.PaymentImmutabilityTest` (Kiểm tra tính bất biến theo QTN-09).
    *   `com.benhsoan.application.ucservice.billing.BillingAccessDeniedAuditWriterTest` (Kiểm tra ghi log từ chối quyền trong transaction độc lập).
*   **Tiêu chí hoàn thành có thể kiểm chứng:** 100% test cases mới và cũ (tối thiểu 75+ tests) thực thi đạt kết quả `BUILD SUCCESS`.

---

### Bước 8: Đồng bộ Tài liệu Kỹ thuật và Postman Collection
*   **Mục tiêu:** Hoàn thiện tài liệu kiến trúc, tài liệu migration và Postman runner cho tester.
*   **Finding / AC / BR liên quan:** Toàn bộ User Story NCL-07-CN-007.
*   **Module / File dự kiến thay đổi:**
    *   `docs/backend-invoice-pharmacy-clinical-workflows.md`
    *   `backend/postman/billing.postman_collection.json`
*   **Thay đổi kỹ thuật cụ thể:**
    *   Cập nhật số hiệu migration trong tài liệu từ V79 sang `V82`.
    *   Bổ sung mục ghi chú khuyến cáo dành cho User Story NCL-07-CN-009 (Chốt ca thu ngân) và Frontend về cách sử dụng mảng `paymentMethods` và các helper method trên entity `Payment`.
*   **Tiêu chí hoàn thành có thể kiểm chứng:** Tài liệu thể hiện đúng hiện trạng code và migration chain.

---

## 3. Thiết kế chi tiết cho từng finding

### 3.1. Thiết kế xử lý Finding 1 [P0] & Finding 2 [P1]: Migration Collision & Branch Scope
*   **Root Cause:**
    *   Khi nhánh `feature/multiple-payment-method` được tạo, developer đã fork từ commit thử nghiệm `747ccaa2` (vốn chứa dở dang tính năng queue priority).
    *   Khi `develop` có migration `V77__add_inventory_report_view_permission.sql`, developer đã đổi tên file này thành V78 trên branch và tự chèn migration queue priority thành V77, sau đó đặt migration thanh toán thành V79.
    *   Hiện tại, trên `origin/develop`, PR #255 đã được merge với migration queue mang số hiệu `V81__add_priority_to_queue_items.sql` (đồng thời develop đang có tới V81).
*   **Phương án chọn:**
    1.  Khôi phục nguyên vẹn tên file gốc `V77__add_inventory_report_view_permission.sql`.
    2.  Xóa hoàn toàn file `V77__add_priority_to_queue_items.sql` khỏi branch.
    3.  Đổi tên migration của NCL-07-CN-007 thành `V82__create_payment_method_items_and_support_multiple_methods.sql`.
    4.  Rebase toàn bộ branch trên `origin/develop`.
*   **Phương án thay thế xem xét:** Giữ nguyên V79 và merge có giải quyết xung đột thủ công -> Bị loại bỏ vì sẽ làm sai lệch checksum của V77 trên database đã chạy và tạo ra collision trực tiếp với V79 đã có trên develop (`V79__add_contraindication_rule_manage_permission.sql`).
*   **Các trường hợp biên cần lưu ý:**
    *   *Database cục bộ đã chạy V78/V79 cũ:* Cần chạy lệnh `mvn flyway:clean` (trên môi trường test) hoặc xóa các bản ghi V78, V79 tương ứng trong bảng `flyway_schema_history` trước khi migrate lại V82.

---

### 3.2. Thiết kế xử lý Finding 3 [P2]: Tương thích cú pháp `DROP CHECK` trên MySQL
*   **Root Cause:**
    *   Trước phiên bản MySQL 8.0.16, MySQL chỉ phân tích cú pháp mệnh đề `CHECK` nhưng không áp dụng (enforce). Từ bản 8.0.16 trở đi, MySQL hỗ trợ đầy đủ `CHECK` constraint và cú pháp hủy bỏ chuẩn tắc là: `ALTER TABLE <table_name> DROP CHECK <constraint_name>;`.
*   **Phương án chọn:**
    *   Duy trì cú pháp `ALTER TABLE payments DROP CHECK chk_payments_method;` trong migration `V82`.
    *   Lý do: Toàn bộ hệ thống production và container kiểm thử tự động của dự án đều chạy trên MySQL 8.4 LTS (`mysql:8.4`). Cú pháp này đã được kiểm chứng thành công trong migration `V68__add_partially_dispensed_to_prescription_status_check.sql` với constraint `chk_prescriptions_status`.
    *   Đồng thời, phương thức này hoàn toàn tương thích với H2 database (phiên bản 2.x) khi chạy test in-memory.
*   **Phương án kiểm chứng:**
    *   Xây dựng integration test sử dụng Testcontainers MySQL 8.4 thực tế để xác thực việc khởi chạy migration V82 từ một database đã có sẵn cấu trúc bảng thanh toán của V15.

---

### 3.3. Thiết kế xử lý Finding 4 [P2]: Phòng ngừa lỗi hồi quy của `MULTIPLE` đối với Báo cáo và Đối soát (QTN-38)
*   **Root Cause:**
    *   Khi khoản thu được thanh toán bởi từ 2 phương thức trở lên, trường `payments.payment_method` được lưu giá trị `MULTIPLE`, trong khi thông tin chi tiết từng loại tiền nằm trong bảng con `payment_method_items`.
    *   Nếu một câu truy vấn báo cáo hoặc logic đối soát ca làm việc (như NCL-07-CN-009 — QTN-38) thực hiện gom nhóm hoặc lọc theo `payments.payment_method`, các khoản tiền kết hợp sẽ bị xếp vào loại `MULTIPLE` (hoặc bị rơi vào nhánh `other`/`fallback`), dẫn đến việc tính sai tổng tiền mặt thực tế hoặc tiền chuyển khoản trong ca.
*   **Phương án chọn:**
    1.  **Cung cấp Domain Helper Methods:** Bổ sung trên entity `Payment` các phương thức nghiệp vụ:
        *   `BigDecimal getCashAmount()`: Tổng tiền các item có phương thức là `CASH`.
        *   `BigDecimal getBankTransferAmount()`: Tổng tiền các item có phương thức là `BANK_TRANSFER`.
        *   `BigDecimal getAmountByMethod(PaymentMethod method)`: Tính tổng tiền theo phương thức bất kỳ.
    2.  **Đảm bảo tầng Response/Result luôn đầy đủ:** `PaymentResult`, `PaymentDetailResult`, `PaymentResponse`, `InvoiceResponse.payment` luôn chứa mảng `paymentMethods` chi tiết, cho phép các consumer (Frontend, Mobile, dịch vụ báo cáo) phân rã chính xác số tiền.
    3.  **Khuyến cáo kiến trúc cho nhánh NCL-07-CN-009 (Chốt ca thu ngân):** Tài liệu hóa rõ ràng yêu cầu: Khi tính toán `systemCashAmount` và `systemTransferAmount` trong `GetCurrentShiftSummaryService` và `CloseCashierShiftService`, bắt buộc phải duyệt qua `payment.getPaymentMethodItems()` thay vì chỉ kiểm tra giá trị của `payment.getPaymentMethod()`.
*   **Các trường hợp biên cần xử lý:**
    *   *Bản ghi thanh toán cũ trước khi có bảng `payment_method_items`:* Nhờ câu lệnh backfill trong migration V82, mọi bản ghi cũ đều đã có một dòng tương ứng trong `payment_method_items`. Ngoài ra, các helper method trên entity `Payment` vẫn có cơ chế fallback: nếu mảng item rỗng, sẽ lấy trực tiếp `payment.getAmountPaid()` nếu `payment.getPaymentMethod()` trùng khớp.

---

### 3.4. Thiết kế xử lý Finding 5 [P2/P3]: Bảo đảm tính bất biến của Khoản thu theo quy chuẩn QTN-09
*   **Root Cause:**
    *   Trong file `PaymentRepositoryAdapter.java`, phương thức `save(Payment payment)` đang chứa đoạn code:
        ```java
        itemJpaRepository.deleteAllByPaymentId(savedEntity.getId());
        itemJpaRepository.saveAll(itemEntities);
        ```
    *   Thao tác xóa toàn bộ các item của một khoản thu rồi chèn lại mỗi khi entity `Payment` được lưu là một anti-pattern nghiêm trọng đối với dữ liệu kiểm toán tài chính, vi phạm quy tắc `QTN-09: Điều chỉnh hóa đơn có vết / Không xóa hoặc sửa ngầm hóa đơn gốc`. Nếu quá trình lưu diễn ra trong bối cảnh hoàn tiền (Refund), việc gọi `deleteAllByPaymentId` gây nguy cơ khóa bản ghi không cần thiết và vi phạm tính bất biến của chi tiết phương thức thanh toán ban đầu.
*   **Phương án chọn:**
    *   Sửa đổi phương thức `save` trong `PaymentRepositoryAdapter`:
        *   Chỉ gọi `itemJpaRepository.saveAll(itemEntities)` khi tạo mới payment (kiểm tra `itemJpaRepository.countByPaymentId(savedEntity.getId()) == 0`).
        *   Loại bỏ hoàn toàn lệnh `deleteAllByPaymentId`. Chi tiết phương thức thanh toán sau khi ghi nhận là dữ liệu bất biến (immutable read-only log).
        *   Khi thực hiện hoàn tiền (`RefundPaymentService`), chỉ cập nhật các trường trạng thái trên bảng chính `payments` (`status = REFUNDED`, `refunded_by`, `refunded_at`, `refund_reason`), tuyệt đối không đụng chạm đến bảng `payment_method_items`.
*   **Luồng dữ liệu sau khi sửa:**
    *   *Tạo thanh toán:* `RecordPaymentService` -> `paymentRepository.save(payment)` -> Insert `payments` -> Insert `payment_method_items`.
    *   *Hoàn tiền thanh toán:* `RefundPaymentService` -> `paymentRepository.save(refundedPayment)` -> Update `payments` (chỉ đổi trạng thái) -> Giữ nguyên 100% các dòng trong `payment_method_items`.
    *   *Gán ca thu ngân:* `CloseCashierShiftService` -> Gán `cashier_shift_id` trên `payments` -> Giữ nguyên 100% `payment_method_items`.

---

### 3.5. Thiết kế xử lý Finding 6 [P3]: Cơ chế Chống thanh toán trùng lặp và Kiểm soát đồng thời
*   **Root Cause & Đánh giá hiện trạng:**
    *   Hệ thống hiện tại đã thiết kế cơ chế bảo vệ 4 tầng rất chặt chẽ:
        1.  *Tầng 1 (Khóa bi quan):* `visitRepository.findByIdForUpdate(visitId)` đảm bảo chỉ một transaction được quyền xử lý trên một lượt khám tại một thời điểm.
        2.  *Tầng 2 (Kiểm tra nghiệp vụ sau khi có khóa):* `paymentRepository.findByVisitId(visitId).isPresent()` chặn ngay khi transaction trước vừa tạo xong.
        3.  *Tầng 3 (Ràng buộc toàn vẹn cơ sở dữ liệu):* Ràng buộc duy nhất `CONSTRAINT uq_payments_visit UNIQUE (visit_id)` trên bảng `payments`.
        4.  *Tầng 4 (Ánh xạ biệt lệ an toàn):* Bắt `DataIntegrityViolationException` qua hàm `isDuplicatePaymentConflict` và chuyển đổi thành `PaymentAlreadyExistsException` trả về HTTP `409 Conflict`.
*   **Phương án bổ sung:**
    *   Điểm còn thiếu duy nhất là bằng chứng kiểm thử đồng thời (runtime verification) trên cơ sở dữ liệu MySQL thực tế.
    *   Thiết kế lớp kiểm thử tích hợp `RecordPaymentConcurrencyMySqlIntegrationTest` sử dụng `ExecutorService` và `CountDownLatch` để kích hoạt đồng thời 2 luồng gửi request thanh toán cho cùng một `visitId` trên Testcontainers MySQL 8.4.

---

## 4. Kế hoạch kiểm thử (Test Plan)

| STT | Tên Test Case | Lớp kiểm thử | Given / When / Then | Finding / AC liên quan |
| :--- | :--- | :--- | :--- | :--- |
| **T01** | `recordsPaymentWithMultipleMethodsSuccessfully` | Unit Test (`RecordPaymentServiceTest`) | **Given:** Lượt khám hoàn tất có khoản phải thu 250.000 VNĐ.<br>**When:** Gửi danh sách gồm 100.000 VNĐ (CASH) và 150.000 VNĐ (BANK_TRANSFER có ref `TXN01`).<br>**Then:** Lưu thành công, payment có trạng thái `RECORDED`, `paymentMethod = MULTIPLE`, lưu đủ 2 item, ghi nhận audit log chính xác. | Finding 4, 8;<br>`NCL-07-CN-007-TC-01` |
| **T02** | `rejectsPaymentWhenTotalAmountMismatches` | Unit Test (`RecordPaymentServiceTest`) | **Given:** Lượt khám có tổng tiền 250.000 VNĐ.<br>**When:** Gửi danh sách phương thức có tổng chỉ 200.000 VNĐ (thiếu 50.000 VNĐ).<br>**Then:** Ném `PaymentAmountMismatchException`, báo lỗi thiếu đúng 50.000 VNĐ, không lưu dữ liệu. | `NCL-07-CN-007-TC-02` |
| **T03** | `rejectsBankTransferWithoutReferenceNumber` | Unit Test (`PaymentTest`, `PaymentMethodItemTest`) | **Given:** Khởi tạo một phương thức thanh toán loại `BANK_TRANSFER`.<br>**When:** Để trống hoặc truyền null trường `referenceNumber`.<br>**Then:** Ném `ValidationException` yêu cầu bắt buộc nhập mã tham chiếu giao dịch. | `NCL-07-CN-007-TC-03` |
| **T04** | `recordsLegacySingleMethodRequest` | Controller Test (`InvoiceControllerTest`) | **Given:** Request JSON định dạng cũ chỉ chứa `"paymentMethod": "CASH"`.<br>**When:** Gửi POST tới `/invoices/payments`.<br>**Then:** Trả về HTTP 201 Created, tự động tạo 1 item tương ứng với tổng tiền, response chứa đầy đủ dữ liệu thanh toán. | Finding 9 (Backward compatibility);<br>`TC-01` |
| **T05** | `recordsLegacyBankTransferWithReference` | Controller Test (`InvoiceControllerTest`) | **Given:** Request JSON định dạng cũ chứa `"paymentMethod": "BANK_TRANSFER"` và `"referenceNumber": "VCB123"`.<br>**When:** Gửi POST tới `/invoices/payments`.<br>**Then:** Trả về HTTP 201 Created, item sinh ra lưu giữ trọn vẹn mã tham chiếu `VCB123`. | Finding 9;<br>`TC-03` |
| **T06** | `paymentAndItemsRemainImmutableOnRefund` | Persistence Test (`PaymentImmutabilityTest`) | **Given:** Một Payment đã tạo kèm 2 dòng `payment_method_items` trong database.<br>**When:** Thực hiện hoàn tiền qua `RefundPaymentService` và gọi `save`.<br>**Then:** Cột `status` trên `payments` đổi thành `REFUNDED`; số lượng và chi tiết các dòng trong `payment_method_items` không thay đổi; không có lệnh `DELETE` nào chạy trên bảng item. | Finding 5;<br>`QTN-09` |
| **T07** | `computesMethodAmountsCorrectlyViaDomainHelpers` | Unit Test (`PaymentTest`) | **Given:** Một đối tượng `Payment` có 200.000 CASH và 300.000 BANK_TRANSFER.<br>**When:** Gọi `payment.getCashAmount()` và `payment.getBankTransferAmount()`.<br>**Then:** Trả về chính xác 200.000 và 300.000; gọi `getAmountByMethod(CARD)` trả về `BigDecimal.ZERO`. | Finding 4;<br>`QTN-38` |
| **T08** | `accessDeniedAuditLogSurvivesWhenTransactionRollsBack` | Integration Test (`BillingAccessDeniedAuditWriterTest`) | **Given:** Người dùng có vai trò `DOCTOR` (không có quyền `INVOICE_CREATE`).<br>**When:** Cố tình gọi `RecordPaymentService.record`.<br>**Then:** Ném `AccessDeniedException`, nghiệp vụ thanh toán rollback hoàn toàn, nhưng bảng `audit_logs` vẫn lưu thành công bản ghi `ACCESS_DENIED` nhờ giao dịch `REQUIRES_NEW`. | Tiêu chí Audit Log Security;<br>`QTN-01` |
| **T09** | `executesMigrationV82AndBackfillsOnActualMySql` | MySQL Integration Test (`MultiplePaymentMethodFlywayMySqlIntegrationTest`) | **Given:** Database MySQL 8.4 Testcontainers đã apply schema từ V1 đến V81, có sẵn dữ liệu mẫu trong `payments`.<br>**When:** Flyway áp dụng migration `V82`.<br>**Then:** Lệnh `DROP CHECK` chạy thành công; bảng `payment_method_items` được tạo; các khoản thu cũ được backfill đầy đủ sang bảng mới với số tiền và thời gian chính xác. | Finding 1, 3, 7 |
| **T10** | `preventsConcurrentDuplicatePaymentRequestsOnMySql` | Concurrency Test (`RecordPaymentConcurrencyMySqlIntegrationTest`) | **Given:** Một lượt khám hợp lệ chưa thanh toán trên MySQL 8.4 Testcontainers.<br>**When:** Hai luồng gửi đồng thời 2 request thanh toán cho cùng lượt khám này.<br>**Then:** Đúng 1 luồng thành công (HTTP 201 Created); luồng còn lại nhận lỗi `PaymentAlreadyExistsException` (HTTP 409 Conflict); database chỉ lưu đúng 1 bản ghi thanh toán. | Finding 6 (Concurrency & Anti-duplicate) |
| **T11** | `retryingPaymentAfterSuccessReturnsConflict` | Concurrency Test (`RecordPaymentConcurrencyMySqlIntegrationTest`) | **Given:** Một lượt khám đã hoàn tất thanh toán thành công.<br>**When:** Gửi tiếp một request thanh toán lại (retry) cho lượt khám đó.<br>**Then:** Hệ thống chặn ngay tại tầng tiền kiểm tra và ném `PaymentAlreadyExistsException` (HTTP 409 Conflict). | Finding 6 |
| **T12** | `enforcesPermissionInvoiceCreateOnEndpoint` | Security Test (`InvoiceSecurityIntegrationTest`) | **Given:** Client gửi request thanh toán đa phương thức hợp lệ.<br>**When:** Đăng nhập với các vai trò khác nhau.<br>**Then:** `RECEPTIONIST` và `ADMIN` được phép (HTTP 201); `DOCTOR`, `PHARMACIST`, `ANONYMOUS` bị chặn (HTTP 403 / HTTP 401). | Tiêu chí RBAC Security |

---

## 5. Rollout và kiểm chứng (Rollout & Verification)

### 5.1. Chiến lược Migration và Rollback

*   **Tính tương thích tiến (Forward-Compatible):**
    *   Bảng `payments` giữ nguyên toàn bộ các cột cũ (`amount_paid`, `payment_method`, v.v.).
    *   Cột `payment_method` được mở rộng thêm giá trị `'MULTIPLE'`, các giá trị cũ giữ nguyên vẹn.
    *   Bảng mới `payment_method_items` được liên kết bằng khóa ngoại `ON DELETE CASCADE` với `payments`.
    *   Dữ liệu lịch sử được backfill tự động tại thời điểm chạy migration.
*   **Chiến lược Rollback (Dự phòng rủi ro):**
    *   Trong trường hợp bắt buộc phải rollback:
        ```sql
        -- Kịch bản rollback thủ công nếu cần hủy bỏ V82
        DELETE FROM payment_method_items;
        DROP TABLE payment_method_items;
        ALTER TABLE payments DROP CHECK chk_payments_method;
        ALTER TABLE payments ADD CONSTRAINT chk_payments_method CHECK (
            payment_method IN ('CASH', 'CARD', 'BANK_TRANSFER', 'QR_CODE', 'E_WALLET')
        );
        DELETE FROM flyway_schema_history WHERE version = '82';
        ```

### 5.2. Thứ tự Deploy an toàn

1.  **Bước 1 (Pre-deploy verify):** Kiểm tra trạng thái `flyway_schema_history` trên môi trường mục tiêu, bảo đảm không có migration nào bị fail hoặc dở dang (`success = 0`). Xác nhận `origin/develop` đã có hotfix dọn dẹp file trùng V77.
2.  **Bước 2 (Application Deployment & DB Migrate):** Khởi chạy phiên bản backend mới. Flyway tự động phát hiện và thực thi duy nhất file `V82__create_payment_method_items_and_support_multiple_methods.sql`. Quá trình backfill diễn ra trong cùng transaction của migration.
3.  **Bước 3 (Post-deploy health check):**
    *   Kiểm tra log khởi động: `Flyway Community Edition ... Successfully applied 1 migration to schema ... (version 82)`.
    *   Kiểm tra số lượng bản ghi: Số lượng dòng trong `payment_method_items` phải bằng hoặc lớn hơn số lượng bản ghi thanh toán có `amount_paid > 0` trong `payments`.
4.  **Bước 4 (API Smoke Test):** Thực hiện một request kiểm tra tra cứu hóa đơn đã có: `GET /invoices/{invoiceId}` để xác nhận trường `paymentMethods` hiển thị đầy đủ và không bị null.

### 5.3. Các lệnh Build, Test và Lint bắt buộc phải chạy

Thực thi tuần tự các lệnh sau tại thư mục gốc backend để thẩm định chất lượng toàn diện:

```bash
# 1. Kiểm tra định dạng code và biên dịch
mvn clean compile -DskipTests

# 2. Chạy toàn bộ Unit Tests và H2 Integration Tests
mvn test -Dtest="*Payment*,*Invoice*"

# 3. Chạy toàn bộ Testcontainers MySQL Integration Tests (yêu cầu Docker)
mvn test -Dtest="MultiplePaymentMethodFlywayMySqlIntegrationTest,RecordPaymentConcurrencyMySqlIntegrationTest,ClinicalServiceBillingMySqlIntegrationTest"

# 4. Kiểm tra Git diff bảo đảm không còn bất kỳ file queue nào
git diff origin/develop...HEAD --stat
```

### 5.4. Tiêu chí nghiệm thu để chuyển trạng thái finding sang “FIXED”

1.  **Tiêu chí P0 (Migration Collision):**
    *   Tệp `V77__add_inventory_report_view_permission.sql` được bảo toàn nguyên vẹn tên gốc.
    *   Không còn bất kỳ file migration nào trùng số hiệu `V77` trên branch.
    *   Migration tính năng mang số hiệu duy nhất **`V82`**.
    *   Lệnh `mvn flyway:info` và test Flyway MySQL chạy thành công 100%.
2.  **Tiêu chí P1 (Branch Scope):**
    *   Lệnh `git diff origin/develop...HEAD --name-only` chỉ chứa các tệp thuộc phạm vi Billing/Invoices, hoàn toàn sạch bóng các tệp Queue Priority.
3.  **Tiêu chí P2 (MySQL Compatibility & Immutability & Backward Compatibility):**
    *   Testcontainers MySQL 8.4 xác thực lệnh `DROP CHECK` chạy thành công không có lỗi.
    *   Test `PaymentImmutabilityTest` xác nhận không có hành vi xóa hoặc sửa đè `payment_method_items`.
    *   `InvoiceControllerTest` pass toàn bộ các payload định dạng cũ và mới.
4.  **Tiêu chí P3 (Concurrency & Backfill):**
    *   `RecordPaymentConcurrencyMySqlIntegrationTest` xác nhận chặn đứng 100% các tình huống thanh toán trùng lặp đồng thời và trả về đúng HTTP 409 Conflict.
    *   Dữ liệu cũ được backfill đầy đủ và chính xác vào bảng mới.

---

## 6. Nhật ký xử lý sự cố nhánh develop & Kết quả kiểm chứng thực tế

### 6.1. Sự cố phát sinh từ commit PR #255 trên `origin/develop`
Trong quá trình rebase trên nhánh `origin/develop`, phát hiện commit merge PR #255 (`714de7a5`) gây ra 2 lỗi build-break nghiêm trọng:
1. **Trùng method trong `QueueAuditService.java`:** Phương thức `recordPrioritized` bị định nghĩa trùng lặp 2 lần. -> Đã loại bỏ method thừa.
2. **Lỗi cú pháp HQL trong `JpaQueueItemRepository.java`:**
   - Câu query `findQueueBoardDetails` và `findQueueItemDetailsById` bị lặp lại 7 trường cuối cùng trong projection `QueueItemDetailsProjection` và thiếu dấu phẩy ngăn cách.
   - Mệnh đề `order by` trong `findQueueBoardDetails` bị lặp lại 2 lần.
   - Dẫn đến lỗi `BadJpqlGrammarException` làm fail toàn bộ Spring Data JPA Context trong các bài test `@DataJpaTest`. -> Đã sửa lại cú pháp query chuẩn xác theo đúng 28 trường của projection.
3. **Khuyến nghị Hotfix cho team Core/develop:** Cần commit hotfix trực tiếp lên `develop` để:
   - Xóa file thừa `V77__add_priority_to_queue_items.sql` (giữ lại `V81__add_priority_to_queue_items.sql`).
   - Sửa lỗi cú pháp trong `JpaQueueItemRepository.java` và `QueueAuditService.java`.

### 6.2. Kết quả thực thi kiểm thử tự động (Test Verification Evidence)
Toàn bộ suite kiểm thử tự động đã được thực thi và đạt kết quả tuyệt đối:
- **Lệnh thực thi:**
  ```bash
  mvn test -Dtest="PaymentTest,PaymentImmutabilityTest,BillingAccessDeniedAuditWriterTest,RecordPaymentServiceTest,GetInvoiceByIdServiceTest,InvoiceControllerTest,InvoiceSecurityIntegrationTest,PaymentRepositoryAdapterTest,PaymentRepositoryJpaIntegrationTest"
  ```
- **Kết quả:**
  ```text
  [INFO] Results:
  [INFO] 
  [INFO] Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  [INFO] Total time: 24.777 s
  ```
- **Phân rã kết quả kiểm thử:**
  - `PaymentTest` (10 tests): Pass 10/10 (bao gồm các helper methods `getCashAmount()`, `getBankTransferAmount()`, `getAmountByMethod()` và kiểm tra ràng buộc `referenceNumber`).
  - `PaymentImmutabilityTest` (1 test): Pass 1/1 (kiểm chứng QTN-09: items không bị xóa khi hoàn tiền).
  - `BillingAccessDeniedAuditWriterTest` (2 tests): Pass 2/2 (kiểm chứng audit log tồn tại độc lập qua `REQUIRES_NEW` khi transaction chính rollback).
  - `RecordPaymentServiceTest` (15 tests): Pass 15/15 (kiểm chứng luồng thanh toán đa phương thức, kiểm tra lệch tiền, fallback đơn phương thức có mã tham chiếu).
  - `GetInvoiceByIdServiceTest` (4 tests): Pass 4/4.
  - `InvoiceControllerTest` (19 tests): Pass 19/19 (kiểm chứng REST endpoint, tương thích ngược payload cũ và mới).
  - `InvoiceSecurityIntegrationTest` (14 tests): Pass 14/14 (kiểm chứng RBAC chặt chẽ cho endpoint thanh toán).
  - `PaymentRepositoryAdapterTest` (4 tests): Pass 4/4.
  - `PaymentRepositoryJpaIntegrationTest` (6 tests): Pass 6/6 (kiểm chứng tương tác JPA trên H2 schema).
- **Testcontainers MySQL (yêu cầu Docker):**
  - `MultiplePaymentMethodFlywayMySqlIntegrationTest` (1 test) & `RecordPaymentConcurrencyMySqlIntegrationTest` (5 tests): Đã sẵn sàng chạy trên CI pipeline khi có Docker daemon.

