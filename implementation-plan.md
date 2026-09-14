# Implementation Plan: Gọi lại và tạm hoãn bệnh nhân vắng trong hàng đợi (NCL-03-CN-009)

Kế hoạch kỹ thuật triển khai backend hoàn chỉnh cho User Story `NCL-03-CN-009`: **Gọi lại và tạm hoãn bệnh nhân vắng trong hàng đợi** thuộc Epic `NCL-03` (Lịch hẹn và hàng đợi khám), đảm bảo đáp ứng đầy đủ Acceptance Criteria (TC-01, TC-02, TC-03, TC-04), bảo toàn kiến trúc Hexagonal Architecture, không gây lỗi hồi quy và tuân thủ các quy tắc nghiệp vụ.

---

## 1. TÓM TẮT QUYẾT ĐỊNH VÀ GIẢI PHÁP KỸ THUẬT

### 1.1. Mục tiêu và Phạm vi
* **Mục tiêu**: Xây dựng hoàn chỉnh luồng nghiệp vụ backend khi bệnh nhân vắng mặt tại thời điểm gọi khám: cho phép tạm hoãn (defer/skip), tự động chuyển lượt sang người kế tiếp, ghi nhận số lần gọi (`callCount`), và cho phép Lễ tân đưa bệnh nhân trở lại hàng đợi (`WAITING`) khi có mặt; lưu vết lịch sử phục vụ kiểm toán.
* **Phạm vi**: Chỉ backend (Domain, Ports, Services, Persistence, REST API, Audit, Tests). Không thay đổi frontend.

### 1.2. Các quyết định kỹ thuật cốt lõi
1. **Khắc phục Bug huỷ dữ liệu của Skip hiện tại**:
   - Hiện tại: `SkipQueueItemService` gọi `visit.cancel()` và `appointment.cancel()`, biến `SKIPPED` thành terminal khiến bệnh nhân không thể khám tiếp khi quay lại.
   - Giải pháp: Khi tạm hoãn, `QueueItem` chuyển sang `SKIPPED`, `Visit` được hoàn trả về trạng thái `WAITING` (thông qua phương thức `hold()`/`revertToWaiting()`), và `Appointment` hoàn trả về trạng thái `CHECKED_IN` (thông qua phương thức `revertToCheckedIn()`). Tuyệt đối **KHÔNG huỷ (`cancel`)** Visit và Appointment khi tạm hoãn.
2. **Số lần gọi (`callCount`)**:
   - Thêm cột `call_count INT NOT NULL DEFAULT 0` vào bảng `queue_items` thông qua migration `V51__add_queue_item_call_count_and_defer_support.sql` (tiếp nối các migration chẩn đoán, dời lịch và xác nhận lịch hẹn).
   - `callCount` tăng lên 1 mỗi khi phương thức `call()` được gọi (tại `CallNextQueueItemService`).
3. **Cơ chế Tự chuyển người kế tiếp (TC-01)**:
   - Trong `SkipQueueItemService`: Sau khi chuyển ca hiện tại sang tạm hoãn, use case tự động tìm ca kế tiếp đang `WAITING` trong queue để gọi (`callNext`) nếu còn bệnh nhân chờ; nếu queue rỗng thì chỉ hoàn tất tạm hoãn ca hiện tại.
4. **Cơ chế Đưa lại vào hàng đợi (Re-queue - TC-02)**:
   - Thêm API `POST /queue-items/{itemId}/re-queue`.
   - Domain `QueueItem.reQueue(Instant)`: Cho phép chuyển đổi hợp lệ từ `SKIPPED -> WAITING`.
   - Sắp xếp vị trí: Giữ nguyên `queueNumber` gốc (vì các số nhỏ hơn đã khám xong nên tự nhiên ca này sẽ nằm ở đầu danh sách `WAITING` được ưu tiên gọi sớm), đồng thời tôn trọng mức độ ưu tiên của hàng đợi (`priorityLevel`: EMERGENCY > APPOINTMENT > REGULAR).
5. **Chặn trạng thái sai (TC-03)**:
   - Chặn và ném `QueueItemInvalidStatusException` nếu ca đã `COMPLETED` hoặc `CANCELLED` khi gọi tạm hoãn.
6. **Lịch sử hàng đợi (TC-04)**:
   - Ghi audit log chi tiết qua `QueueAuditService` với `action`: `DEFERRED` / `RE_QUEUED`, lưu `callCount`, `actorId`, `timestamp`, `reason`.
   - Cung cấp API `GET /queue-items/{itemId}/history` (hoặc `GET /queues/{queueId}/history`) trả về lịch sử thao tác hàng đợi.

---

## 2. KẾ HOẠCH TRIỂN KHAI THEO TỪNG GIAI ĐOẠN

### Giai đoạn 1: Database Migration V51 & Domain Foundation
* **Mục tiêu**: Bổ sung schema lưu trữ `call_count`, mở rộng Domain `QueueItem`, `Visit`, `Appointment` hỗ trợ đếm số lần gọi, tạm hoãn và hoàn trả trạng thái.
* **File tác động**:
  * [NEW] `backend/src/main/resources/db/migration/V51__add_queue_item_call_count_and_defer_support.sql`
  * [MODIFY] `backend/src/main/java/com/benhsoan/domain/queue/QueueItem.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/domain/visit/Visit.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/domain/appointment/Appointment.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/persistence/entity/queue/QueueItemEntity.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/persistence/mapper/queue/QueueStructurePersistenceMapper.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/persistence/jpaRepository/queue/QueueItemDetailsProjection.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/persistence/jpaRepository/queue/JpaQueueItemRepository.java`
* **Quy tắc nghiệp vụ**:
  - `QueueItem.call()` tăng `callCount++`.
  - `QueueItem.reQueue()` chỉ hợp lệ từ `SKIPPED -> WAITING`.
  - `Visit.revertToWaiting()` từ `IN_PROGRESS -> WAITING`.
  - `Appointment.revertToCheckedIn()` từ `IN_PROGRESS -> CHECKED_IN`.
* **Tiêu chí verify/test**:
  - Unit tests cho `QueueItemTest`, `VisitTest`, `AppointmentTest` bao phủ tất cả transition mới và chặn transition sai.
  - Integration test `QueueItemReadModelJpaIntegrationTest` kiểm tra mapping và query cột `call_count`.

### Giai đoạn 2: Refactor Skip Service & Xây dựng Re-Queue Use Case (Core Application)
* **Mục tiêu**: Hoàn thiện logic nghiệp vụ tạm hoãn không phá huỷ Visit/Appointment; tự động chuyển lượt gọi sang người tiếp theo; xây dựng use case đưa lại vào hàng đợi.
* **File tác động**:
  * [NEW] `backend/src/main/java/com/benhsoan/port/inbound/queue/ReQueueItemUseCase.java`
  * [NEW] `backend/src/main/java/com/benhsoan/port/dto/command/queue/ReQueueItemCommand.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/port/dto/result/QueueItemResult.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/application/ucservice/queue/SkipQueueItemService.java`
  * [NEW] `backend/src/main/java/com/benhsoan/application/ucservice/queue/ReQueueItemService.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/application/ucservice/queue/CallNextQueueItemService.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/application/ucservice/queue/QueueOperationAuthorization.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/application/ucservice/queue/QueueAuditService.java`
* **Quy tắc nghiệp vụ**:
  - TC-01: Tạm hoãn -> lưu `callCount`, tự động gọi người tiếp theo (`callNext`) nếu có ca `WAITING`.
  - TC-02: Đưa lại vào hàng đợi -> item chuyển về `WAITING`, cập nhật ngay lập tức.
  - TC-03: Từ chối tạm hoãn nếu ca đã `COMPLETED` hoặc `CANCELLED`.
* **Tiêu chí verify/test**:
  - `SkipQueueItemServiceTest`: Cập nhật lại test case để verify `visit` và `appointment` không bị huỷ.
  - `ReQueueItemServiceTest` [NEW]: Kiểm tra luồng re-queue thành công, phân quyền và kiểm tra trạng thái không hợp lệ.

### Giai đoạn 3: REST API Adapters, Security & Audit Query (Web & Infrastructure)
* **Mục tiêu**: Cung cấp API endpoints cho lễ tân thao tác, trả về `callCount`, hỗ trợ tra cứu lịch sử hàng đợi (TC-04).
* **File tác động**:
  * [MODIFY] `backend/src/main/java/com/benhsoan/adapter/inbound/rest/controller/QueueController.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/adapter/inbound/rest/response/queue/QueueItemResponse.java`
  * [MODIFY] `backend/src/main/java/com/benhsoan/adapter/inbound/rest/mapper/QueueRestMapper.java`
  * [NEW] `backend/src/main/java/com/benhsoan/port/inbound/queue/GetQueueHistoryUseCase.java`
  * [NEW] `backend/src/main/java/com/benhsoan/application/ucservice/queue/GetQueueHistoryService.java`
  * [NEW] `backend/src/main/java/com/benhsoan/adapter/inbound/rest/response/queue/QueueHistoryResponse.java`
  * [MODIFY] `backend/docs/permission-matrix.md`
* **Quy tắc nghiệp vụ**:
  - TC-04: API lịch sử trả về `callCount`, `skippedAt` (thời điểm tạm hoãn), và người thao tác.
  - RBAC: `RECEPTIONIST`, `ADMIN` có quyền gọi `re-queue`.
* **Tiêu chí verify/test**:
  - MockMvc tests trong `QueueControllerTest`: kiểm tra `POST /queue-items/{id}/re-queue`, `POST /queue-items/{id}/skip`, `GET /queue-items/{id}/history`.
  - Security tests trong `QueueSecurityIntegrationTest`: kiểm tra 403 Forbidden khi thiếu role/permission.

### Giai đoạn 4: Kiểm thử tích hợp toàn diện & Hồi quy (Verification & Regression)
* **Mục tiêu**: Chạy toàn bộ test suites của backend, đảm bảo vòng đời hàng đợi hoạt động hoàn hảo và không gây hồi quy.
* **File tác động**:
  * [MODIFY] `backend/src/test/java/com/benhsoan/application/ucservice/queue/SkipQueueItemTransactionIntegrationTest.java`
  * [MODIFY] `backend/src/test/java/com/benhsoan/application/ucservice/queue/QueueFlowServiceTest.java`
* **Quy tắc nghiệp vụ**:
  - Kiểm tra toàn bộ vòng đời: Check-in -> Call-next (callCount=1) -> Defer (auto call next ca khác) -> Re-queue -> Call-next lại (callCount=2) -> Complete.
* **Tiêu chí verify/test**:
  - Chạy `mvn test` trên toàn bộ module backend, đảm bảo 100% test pass không có failure/error nào.
