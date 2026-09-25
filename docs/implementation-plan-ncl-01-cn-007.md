# Kế hoạch triển khai khắc phục (Remediation Plan) - NCL-01-CN-007
**Tính năng:** Quản lý phiên làm việc và tự động đăng xuất  
**User Story:** `NCL-01-CN-007` (Epic `NCL-01` - Đăng nhập và phân quyền)  
**Quy tắc nghiệp vụ:** `QTN-45`, `QTN-01`  
**Vai trò phụ trách:** Tech Lead Backend  
**Trạng thái kế hoạch:** Sẵn sàng thực thi (Ready for Execution) - Chưa sửa code nghiệp vụ  

---

## 1. Tóm tắt quyết định xử lý (Decision Summary)

### 1.1. Ma trận xử lý các Finding từ Review
| Mã Finding | Mức độ | Tiêu đề Finding | Trạng thái xác minh | Quyết định | Lý do & Căn cứ nghiệp vụ/kỹ thuật |
| :--- | :---: | :--- | :---: | :---: | :--- |
| **finding-01** | **P0** | Bác sĩ truy cập `/admin/sessions` bị chặn ở tầng Filter Spring Security, không kích hoạt AOP Aspect và không ghi Nhật ký kiểm toán | **Đã xác minh** | **XỬ LÝ** | Vi phạm trực tiếp Acceptance Criterion **`AC-04`** (`NCL-01-CN-007-TC-04`) và quy tắc bảo mật **`QTN-01`**. Cần cho phép request đi qua Filter tới Controller để `@RequirePermission` chặn và ghi audit log. |
| **finding-02** | **P1** | `JwtAuthenticationFilter` chặn 401 trên `/auth/logout` và `/auth/refresh` khi phiên hết hạn/bị ngắt do thiếu endpoint trong `isPublicOrAuthPath` | **Đã xác minh** | **XỬ LÝ** | Phá vỡ luồng đăng xuất dọn dẹp phiên và xoay refresh token của client khi session idle timeout hoặc bị ngắt từ xa (`QTN-45`). |
| **finding-03** | **P1** | `UserSession.extend()` ném `IllegalStateException` không được map tại `GlobalExceptionHandler`, gây lỗi HTTP 500 ra client | **Đã xác minh** | **XỬ LÝ** | Vi phạm chuẩn API contract và exception convention. Khi phiên đã hết hạn/bị ngắt mà user bấm gia hạn, hệ thống phải trả về HTTP 400 (`VALIDATION_FAILED`) thay vì HTTP 500 (`INTERNAL_SERVER_ERROR`). |
| **finding-04** | **P2** | `PatientLoginService` không gán `ipAddress` và `userAgent` vào `UserSession`, làm mất dấu vết máy trạm bệnh nhân | **Đã xác minh** | **XỬ LÝ** | Dữ liệu phiên bệnh nhân bị `NULL` thông tin máy trạm, Admin không giám sát được thiết bị quầy/kiosk (`AC-03`, `NCL-01-CN-007-CV-02`). |
| **finding-05** | **P2** | Thiếu hụt 100% Test Suite kiểm chứng cho 4 Acceptance Criteria của NCL-01-CN-007 | **Đã xác minh** | **XỬ LÝ** | `SessionManagementController`, `GetActiveSessionsService`, `TerminateSessionService`, `ExtendSessionService`, `SessionAuditWriter` có 0% test coverage. Cần bổ sung Unit, Integration và Security test. |
| **finding-06** | **P3** | `UserSessionRepositoryAdapter` dùng `Instant.now()` thay vì `ClockPort`, và toàn bộ thay đổi tính năng chưa được commit | **Đã xác minh** | **XỬ LÝ** | Không đồng nhất với `ClockPort` làm sai lệch test thời gian; unstaged changes tiềm ẩn nguy cơ mất mã nguồn. |

### 1.2. Danh sách Finding không xử lý
*Không có.* Toàn bộ 6 finding đều là lỗi đã được kiểm chứng bằng bằng chứng mã nguồn cụ thể, ảnh hưởng trực tiếp tới tính đúng đắn của tính năng NCL-01-CN-007.

### 1.3. Giả định kỹ thuật (Assumptions)
1. **Giả định 1 (Đã xác minh):** Hệ thống phân quyền hiện tại sử dụng `@RequirePermission` kết hợp `RequirePermissionAspect` để tự động ghi bản ghi `AuditLog` với `ActionType.ACCESS_DENIED`, `ResourceType.PERMISSION` khi người dùng không đủ quyền.
2. **Giả định 2 (Đã xác minh):** Endpoint `GET /admin/sessions` và `POST /admin/sessions/{id}/terminate` giữ nguyên URL path và contract đã công bố, chỉ điều chỉnh cấu hình khớp đường dẫn trong `SecurityConfig`.
3. **Giả định 3 (Đã xác minh):** Cơ chế Single-Session trên mỗi tài khoản (`revokeByUserId` khi đăng nhập thành công) tiếp tục được duy trì theo đúng chính sách an ninh phòng khám.

---

## 2. Kế hoạch triển khai theo thứ tự (Step-by-Step Implementation Plan)

Sắp xếp theo thứ tự phụ thuộc kiến trúc:  
`Domain / Port` ──► `Persistence` ──► `Application / Service` ──► `Adapter & Security` ──► `Testing Suite` ──► `Commit & Tài liệu`

```
┌────────────────────────────────────────────────────────┐
│ Bước 1: Domain & Port Layer (Sửa Exception contract)   │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ Bước 2: Persistence Layer (Chuẩn hóa ClockPort)        │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ Bước 3: Application Layer (Client context & Clock)     │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ Bước 4: Adapter & Security Layer (Filter & Phân quyền) │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ Bước 5: Testing Suite (Toàn bộ 4 ACs & Regression)     │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│ Bước 6: Đóng gói Commit & Cập nhật Tài liệu            │
└────────────────────────────────────────────────────────┘
```

---

### Bước 1: Chuẩn hóa Domain Exception & Outbound Port Contract
- **Mục tiêu:** Khắc phục lỗi HTTP 500 khi gia hạn phiên và chuẩn hóa tham số thời gian cho truy vấn phiên hoạt động.
- **Finding liên quan:** `finding-03` (P1), `finding-06` (P3).
- **Quy tắc / AC:** `AC-02` (`NCL-01-CN-007-TC-02`), `QTN-45`.
- **Files thay đổi:**
  - `backend/src/main/java/com/benhsoan/domain/auth/UserSession.java`
  - `backend/src/main/java/com/benhsoan/port/outbound/repository/auth/UserSessionRepository.java`
- **Thay đổi kỹ thuật cụ thể:**
  1. Trong `UserSession.java` tại method `extend(Instant now, Duration idleTimeout)`:
     - Thay thế việc ném `IllegalStateException` bằng `ValidationException` (ví dụ: `"Cannot extend a revoked session."`, `"Cannot extend an expired session."`, `"Cannot extend an idle timed-out session."`).
  2. Trong `UserSessionRepository.java`:
     - Cập nhật signature: `Page<UserSession> findActiveSessions(Instant now, Instant activeThreshold, Pageable pageable);` để nhận thời điểm hiện tại `now` rõ ràng từ tầng application.
- **Ảnh hưởng:**
  - API: Lỗi gia hạn khi phiên không hợp lệ sẽ trả về HTTP 400 Bad Request có format `VALIDATION_FAILED` chuẩn mực thay vì HTTP 500.
  - Database / Migration: Không ảnh hưởng.
  - Transaction / Audit: Không ảnh hưởng.
- **Rủi ro & Giảm thiểu:**
  - Rủi ro: Có class khác gọi `findActiveSessions` bị lệch tham số.
  - Giảm thiểu: Rà soát toàn bộ codebase, chỉ có duy nhất `GetActiveSessionsService` gọi port này.
- **Tiêu chí hoàn thành (Verify):**
  - Unit test `UserSessionTest` kiểm tra `extend()` ném đúng `ValidationException`. Mã nguồn biên dịch không lỗi (`mvn test-compile`).

---

### Bước 2: Chuẩn hóa Persistence Adapter với Clock Xác Định
- **Mục tiêu:** Loại bỏ hardcode `Instant.now()` trong adapter, đảm bảo kiểm thử thời gian chạy ổn định.
- **Finding liên quan:** `finding-06` (P3).
- **Files thay đổi:**
  - `backend/src/main/java/com/benhsoan/persistence/adapterRepository/auth/UserSessionRepositoryAdapter.java`
- **Thay đổi kỹ thuật cụ thể:**
  1. Cập nhật method `findActiveSessions`:
     ```java
     @Override
     public Page<UserSession> findActiveSessions(Instant now, Instant activeThreshold, Pageable pageable) {
         return jpaRepository.findActiveSessions(now, activeThreshold, pageable)
                 .map(mapper::toDomain);
     }
     ```
- **Ảnh hưởng:**
  - Tầng persistence không còn tự ý sinh thời gian hệ thống, hoàn toàn phụ thuộc vào tham số thời gian truyền từ Use Case.
- **Rủi ro & Giảm thiểu:**
  - Rủi ro không đáng kể.
- **Tiêu chí hoàn thành (Verify):**
  - Adapter chuyển tiếp chính xác tham số `now` vào `JpaUserSessionRepository`.

---

### Bước 3: Hoàn thiện Client Context trong Application Layer
- **Mục tiêu:** Ghi nhận đầy đủ IP và User-Agent cho phiên của Bệnh nhân; dọn dẹp mã chết audit.
- **Finding liên quan:** `finding-04` (P2), `finding-01` (P0), `finding-06` (P3).
- **Quy tắc / AC:** `AC-03`, `NCL-01-CN-007-CV-02`.
- **Files thay đổi:**
  - `backend/src/main/java/com/benhsoan/application/ucservice/auth/PatientLoginService.java`
  - `backend/src/main/java/com/benhsoan/application/ucservice/auth/GetActiveSessionsService.java`
  - `backend/src/main/java/com/benhsoan/application/ucservice/auth/SessionAuditWriter.java`
- **Thay đổi kỹ thuật cụ thể:**
  1. Trong `PatientLoginService.java:195-199`:
     - Gọi `UserSession.create(user.getId(), tokenHashPort.hash(refreshToken), now.plus(REFRESH_TOKEN_TIMEOUT), command.ipAddress(), command.userAgent());`.
  2. Trong `GetActiveSessionsService.java:53`:
     - Truyền `now` từ `clockPort.now()` vào `userSessionRepository.findActiveSessions(now, activeThreshold, pageable)`.
     - Thay thế đoạn `sessionsPage.map(s -> null)` bằng `Page.empty(pageable)` hoặc mapping trực tiếp để code tường minh.
  3. Trong `SessionAuditWriter.java`:
     - Đánh dấu docstring rõ ràng cho các method audit độc lập giao dịch qua `REQUIRES_NEW`. Loại bỏ hoặc giữ lại `writeAccessDeniedAudit` như một utility dùng chung nếu cần audit bổ sung từ service.
- **Ảnh hưởng:**
  - Database: Cột `ip_address` và `user_agent` của `user_sessions` có dữ liệu thực tế khi bệnh nhân đăng nhập.
  - Audit log: Đảm bảo audit ngắt phiên được lưu trữ an toàn.
- **Rủi ro & Giảm thiểu:**
  - Rủi ro chuỗi User-Agent quá dài (> 500 ký tự).
  - Giảm thiểu: `UserSessionEntity.userAgent` đã có `length = 500`, cắt ngắn nếu vượt quá 500 ký tự trước khi lưu.
- **Tiêu chí hoàn thành (Verify):**
  - Test đăng nhập bệnh nhân tạo session có đầy đủ IP và User Agent.

---

### Bước 4: Khắc phục Lỗ hổng Phân quyền & Filter Interception (Security Layer)
- **Mục tiêu:** Cho phép `RequirePermissionAspect` chặn và ghi Audit Log khi vai trò không phải Admin truy cập `/admin/sessions`; không chặn 401 đối với `/auth/logout` và `/auth/refresh`.
- **Finding liên quan:** `finding-01` (P0), `finding-02` (P1).
- **Quy tắc / AC:** `AC-04` (`NCL-01-CN-007-TC-04`), `QTN-01`, `QTN-45`.
- **Files thay đổi:**
  - `backend/src/main/java/com/benhsoan/config/SecurityConfig.java`
  - `backend/src/main/java/com/benhsoan/infrastructure/authSecurity/JwtAuthenticationFilter.java`
- **Thay đổi kỹ thuật cụ thể:**
  1. Trong `SecurityConfig.java:78`:
     - Cấu hình mở đường dẫn `/admin/sessions/**` cho mọi người dùng đã xác thực:
       ```java
       .requestMatchers("/admin/sessions/**").authenticated()
       .requestMatchers("/admin/**").hasRole("ADMIN")
       ```
     - Nhờ đó, request của Bác sĩ (`ROLE_DOCTOR`) sẽ vượt qua được tầng `AuthorizationFilter` của Spring Security để đi tới `SessionManagementController`. Tại đây, `@RequirePermission("SESSION_READ")` sẽ kích hoạt `RequirePermissionAspect.java`, ném 403 Forbidden và **ghi bản ghi `ACCESS_DENIED` vào bảng `audit_logs`** theo đúng `AC-04` và `QTN-01`.
  2. Trong `JwtAuthenticationFilter.java:215-225`:
     - Cập nhật hàm `isPublicOrAuthPath(HttpServletRequest request)`:
       Bổ sung thêm 2 endpoint:
       ```java
       || normalized.equals("/auth/logout")
       || normalized.equals("/auth/refresh")
       ```
     - Nhờ đó, khi session đã hết hạn hoặc bị ngắt từ xa, client gửi request đăng xuất hoặc xoay token kèm header Bearer không bị filter chặn 401 trước khi đến Controller.
- **Ảnh hưởng:**
  - Security & Permission: Không làm suy yếu bảo mật. Endpoint `/admin/sessions` vẫn được bảo vệ 100% bằng `@RequirePermission("SESSION_READ")` và `@RequirePermission("SESSION_TERMINATE")` (chỉ cấp cho `ROLE_ADMIN`).
  - Audit log: Bác sĩ truy cập danh sách phiên sẽ tạo ra 1 bản ghi `AuditLog` với `action_type = 'ACCESS_DENIED'`, `resource_type = 'PERMISSION'`.
- **Rủi ro & Giảm thiểu:**
  - Rủi ro: Bác sĩ có thể thực thi được logic nếu quên khai báo `@RequirePermission`.
  - Giảm thiểu: `SessionManagementController` đã có `@RequirePermission` trên từng method; bổ sung unit test và security integration test kiểm tra chặt chẽ.
- **Tiêu chí hoàn thành (Verify):**
  - Chạy `MockMvc` giả lập Bác sĩ gọi `GET /admin/sessions` -> nhận HTTP 403 Forbidden, `auditLogRepository.save(...)` được gọi với `ActionType.ACCESS_DENIED`.

---

### Bước 5: Xây dựng Toàn diện Test Suite cho NCL-01-CN-007
- **Mục tiêu:** Bù đắp 100% test suite còn thiếu cho cả 4 ACs và các quy tắc nghiệp vụ `QTN-45`, `QTN-01`.
- **Finding liên quan:** `finding-05` (P2).
- **Files tạo mới / cập nhật:**
  - `backend/src/test/java/com/benhsoan/domain/auth/UserSessionTest.java` (Cập nhật)
  - `backend/src/test/java/com/benhsoan/domain/clinic/ClinicConfigurationTest.java` (Cập nhật)
  - `backend/src/test/java/com/benhsoan/infrastructure/authSecurity/JwtAuthenticationFilterTest.java` (Cập nhật)
  - `backend/src/test/java/com/benhsoan/application/ucservice/auth/GetActiveSessionsServiceTest.java` (Tạo mới)
  - `backend/src/test/java/com/benhsoan/application/ucservice/auth/TerminateSessionServiceTest.java` (Tạo mới)
  - `backend/src/test/java/com/benhsoan/application/ucservice/auth/ExtendSessionServiceTest.java` (Tạo mới)
  - `backend/src/test/java/com/benhsoan/adapter/inbound/rest/controller/SessionManagementControllerSecurityTest.java` (Tạo mới)
- **Tiêu chí hoàn thành (Verify):**
  - Toàn bộ test suite liên quan đến tính năng pass 100%: `mvn test -Dtest="*Session*,*Auth*,*ClinicConfig*"`.

---

### Bước 6: Đóng gói Commit & Cập nhật Tài liệu
- **Mục tiêu:** Đảm bảo toàn bộ thay đổi được commit sạch sẽ vào Git theo convention và tài liệu được lưu vết.
- **Finding liên quan:** `finding-06` (P3).
- **Thao tác:**
  - Kiểm tra `git status` đảm bảo không còn file untracked bị bỏ quên.
  - Commit mã nguồn theo conventional commit: `feat(auth): implement session management and auto-logout (NCL-01-CN-007)`.

---

## 3. Thiết kế chi tiết cho từng Finding

### 3.1. Chi tiết Finding-01 (P0): Phân quyền & Ghi Nhật ký kiểm toán cho `GET /admin/sessions`
- **Root Cause Phân tích:**
  Spring Security Filter Chain chạy trước Spring MVC Handler Mapping và Spring AOP Aspect. Khi khai báo `.requestMatchers("/admin/**").hasRole("ADMIN")`, mọi request từ người dùng không mang `ROLE_ADMIN` đều bị chặn tại `AuthorizationFilter` và chuyển sang `accessDeniedHandler`. Vì `accessDeniedHandler` của hệ thống chỉ ghi JSON lỗi 403 mà không chứa logic ghi log kiểm toán, request bị chặn sớm khiến `@RequirePermission("SESSION_READ")` không bao giờ được chạm tới.
- **Phương án lựa chọn:**
  Khai báo `.requestMatchers("/admin/sessions/**").authenticated()` trong `SecurityConfig.java`.
- **Luồng xử lý sau khi sửa:**
  ```
  Doctor (ROLE_DOCTOR) ──► HTTP GET /admin/sessions
      │
      ▼
  JwtAuthenticationFilter: Xác thực token hợp lệ, nạp authorities (ROLE_DOCTOR, PERMISSION_...)
      │
      ▼
  SecurityConfig Filter Chain: Khớp "/admin/sessions/**" -> Kiểm tra authenticated -> HỢP LỆ
      │
      ▼
  DispatcherServlet ──► SessionManagementController.getActiveSessions()
      │
      ▼
  RequirePermissionAspect: Kiểm tra permission "SESSION_READ"
      │
      ├──► Không có quyền!
      ├──► Gọi auditDeniedAttempt(): Ghi AuditLog (ActionType.ACCESS_DENIED, ResourceType.PERMISSION)
      └──► Ném AccessDeniedException ("Bạn không có quyền thực hiện thao tác này")
      │
      ▼
  GlobalExceptionHandler: Bắt AccessDeniedException ──► Trả về HTTP 403 FORBIDDEN
  ```
- **Xử lý các trường hợp biên:**
  - *Dữ liệu cũ / Rollback:* Bản ghi audit log được ghi vào bảng `audit_logs` ngay lập tức.
  - *Admin hợp lệ:* Admin có `ROLE_ADMIN` và `SESSION_READ` sẽ vượt qua cả 2 tầng và nhận danh sách phiên dạng Page 200 OK.

---

### 3.2. Chi tiết Finding-02 (P1): Bỏ chặn Filter trên `/auth/logout` và `/auth/refresh`
- **Root Cause Phân tích:**
  `JwtAuthenticationFilter` cố gắng thực thi cơ chế tự động đăng xuất bằng cách trả về 401 khi session hết hạn hoặc bị ngắt. Tuy nhiên, nó lại áp dụng cho toàn bộ các endpoint trừ một danh sách hardcode trong `isPublicOrAuthPath`. Khi session hết hạn, chính thao tác đăng xuất hoặc làm mới token lại bị chặn đứng.
- **Phương án lựa chọn:**
  Thêm `/auth/logout` và `/auth/refresh` vào danh sách ngoại lệ `isPublicOrAuthPath`.
- **Luồng xử lý sau khi sửa:**
  - User bị idle timeout (quá 30 phút) -> Giao diện gửi request `POST /auth/logout` kèm Bearer token -> Filter nhận diện là path `/auth/logout` nên không chặn 401, cho phép đi tiếp vào `AuthController.logout()` -> Token được thu hồi hoàn tất.
  - Client gửi `POST /auth/refresh` kèm Authorization header cũ và Body chứa `refreshToken` -> Filter cho phép đi tiếp vào `AuthController.refresh()` -> Xoay token thành công hoặc ném lỗi nghiệp vụ rõ ràng từ Service nếu refresh token đã hỏng.

---

### 3.3. Chi tiết Finding-03 (P1): Chuẩn hóa Ngoại lệ Gia hạn phiên (`UserSession.extend`)
- **Root Cause Phân tích:**
  Tầng Domain sử dụng ngoại lệ `IllegalStateException` (vốn là ngoại lệ kỹ thuật của JDK, thường ngụ ý trạng thái nội tại của JVM hoặc hệ thống bị lỗi). `GlobalExceptionHandler` bắt ngoại lệ này ở fallback `Exception.class` và trả về HTTP 500. Trong khi đó, việc gia hạn một phiên đã hết hạn là một lỗi nghiệp vụ do trạng thái dữ liệu (Business Rule), bắt buộc phải ném `ValidationException` để tầng Rest trả về HTTP 400.
- **Phương án lựa chọn:**
  Trong `UserSession.java`, sửa `throw new IllegalStateException(...)` thành `throw new ValidationException(...)`.
- **Luồng xử lý sau khi sửa:**
  ```
  User ──► POST /auth/sessions/current/extend
      │
      ▼
  ExtendSessionService: Gọi session.extend(now, idleTimeout)
      │
      ├──► Phiên đã bị idle timeout hoặc revoke trước đó
      └──► Ném ValidationException("Cannot extend an idle timed-out session.")
      │
      ▼
  GlobalExceptionHandler.handleValidationException():
      Trả về HTTP 400 BAD_REQUEST:
      {
          "status": 400,
          "code": "VALIDATION_FAILED",
          "message": "Cannot extend an idle timed-out session.",
          "path": "/auth/sessions/current/extend"
      }
  ```

---

### 3.4. Chi tiết Finding-04 (P2): Thu thập Client Context phiên Bệnh nhân
- **Root Cause Phân tích:**
  Sơ suất khi refactor constructor `UserSession.create`: Tác giả đã thêm overload nhận `ipAddress` và `userAgent` và cập nhật `LoginService`, nhưng quên cập nhật tại `PatientLoginService`.
- **Phương án lựa chọn:**
  Truyền `command.ipAddress()` và `command.userAgent()` vào `UserSession.create(...)` trong `PatientLoginService.java`.
- **Trường hợp biên:**
  Chuỗi User-Agent có thể dài hoặc chứa ký tự đặc biệt. Trường `user_agent` trong database là `VARCHAR(500)`. Cần đảm bảo nếu chuỗi vượt quá 500 ký tự sẽ được cắt gọn an toàn (`substring(0, 500)`).

---

## 4. Kế hoạch kiểm thử chi tiết (Test Plan Matrix)

### 4.1. Ma trận Kiểm thử theo Acceptance Criteria & Business Rules
| Mã AC / Quy tắc | Kịch bản kiểm thử | Tầng test | Input / Điều kiện ban đầu (Given) | Hành động (When) | Kết quả mong đợi (Then) |
| :--- | :--- | :---: | :--- | :--- | :--- |
| **AC-01 & QTN-45** | Tự động hết phiên khi idle quá 30m | Filter / Integration | Phiên làm việc có `lastUsedAt = now - 35m`, timeout cấu hình = 30m | Gửi request có Bearer token tới `/patients` | Nhận HTTP 401 Unauthorized, mã lỗi `SESSION_EXPIRED`, body yêu cầu đăng nhập lại. |
| **AC-01 & QTN-45** | Duy trì phiên khi thao tác liên tục | Filter / Integration | Phiên làm việc có `lastUsedAt = now - 2m`, timeout cấu hình = 30m | Gửi request có Bearer token tới `/patients` | Nhận HTTP 200 OK, phiên tiếp tục duy trì hoạt động. |
| **AC-01** | Throttling cập nhật `lastUsedAt` | Persistence / Filter | Phiên làm việc có `lastUsedAt = now - 30s` | Gửi request liên tục trong 30 giây | Cơ sở dữ liệu không bị gọi `touchLastUsed` liên tục (chỉ cập nhật khi khoảng cách >= 60s). |
| **AC-02** | Gia hạn phiên thành công bằng 1 thao tác | Controller / Service | Người dùng đang đăng nhập hợp lệ, phiên còn 3 phút nữa là hết hạn | Gọi `POST /auth/sessions/current/extend` | Nhận HTTP 200 OK, `lastUsedAt` được cập nhật mới, trả về mốc thời gian hết hạn mới. |
| **AC-02 & Finding-03** | Gia hạn phiên thất bại khi phiên đã hết hạn | Controller / Service | Người dùng gọi gia hạn khi phiên đã quá hạn idle timeout | Gọi `POST /auth/sessions/current/extend` | Nhận HTTP 400 Bad Request (`VALIDATION_FAILED`), không gây lỗi HTTP 500. |
| **AC-03** | Tra cứu danh sách phiên đang mở | Controller / Service | Quản trị viên (`ROLE_ADMIN` có `SESSION_READ`) | Gọi `GET /admin/sessions` | Nhận HTTP 200 OK, trả về danh sách phiên phân trang gồm username, IP, User Agent, trạng thái `isCurrentSession`. |
| **AC-03** | Kết thúc phiên từ xa | Controller / Service | Quản trị viên gọi ngắt phiên của người khác | Gọi `POST /admin/sessions/{id}/terminate` | Nhận HTTP 204 No Content, phiên được set `revoked_at = now`. Request tiếp theo của người dùng đó nhận 401 `SESSION_TERMINATED`. |
| **AC-03** | Audit log ngắt phiên độc lập transaction | Service / Integration | Quản trị viên ngắt phiên từ xa | Gọi `terminateSession` | Bản ghi `AuditLog` có `ActionType.DEACTIVATE`, `ResourceType.USER_SESSION` được lưu thành công qua `REQUIRES_NEW`. |
| **AC-04 & QTN-01 & Finding-01** | Bác sĩ truy cập danh sách phiên bị chặn & ghi log | Security Integration | Bác sĩ (`ROLE_DOCTOR`) có token hợp lệ | Gọi `GET /admin/sessions` | Nhận HTTP 403 Forbidden (`ACCESS_DENIED`). Bảng `audit_logs` có 1 bản ghi `ActionType.ACCESS_DENIED`, `ResourceType.PERMISSION`. |
| **Finding-02** | Đăng xuất khi phiên đã hết hạn | Filter / Integration | Phiên đã hết hạn idle timeout | Gọi `POST /auth/logout` kèm Bearer token | Không bị Filter chặn 401, request đi vào được LogoutService để thu hồi phiên thành công (204 No Content). |
| **Finding-02** | Làm mới token khi session idle | Filter / Integration | Session idle timeout, client gửi kèm Bearer cũ trong header | Gọi `POST /auth/refresh` với refresh token hợp lệ | Không bị Filter chặn 401, trả về cặp token mới thành công (200 OK). |
| **Finding-04** | Client context phiên Bệnh nhân | Service / Persistence | Bệnh nhân đăng nhập từ IP `192.168.1.100` và UserAgent `Mozilla/5.0` | Gọi `POST /auth/patient/login` | Bản ghi `user_sessions` có `ip_address = '192.168.1.100'` và `user_agent = 'Mozilla/5.0'`. |

---

## 5. Kế hoạch Rollout, Kiểm chứng & Tiêu chuẩn Nghiệm thu

### 5.1. Thứ tự Deploy An toàn (Safe Rollout Sequence)
1. **Kiểm tra Migration Flyway:** Chạy migration `V97__session_management_and_idle_timeout.sql` trên môi trường Staging/Test DB.
   - Xác nhận bảng `clinic_configuration` có cột `session_idle_timeout_minutes` với giá trị mặc định 30.
   - Xác nhận bảng `user_sessions` có cột `ip_address`, `user_agent` và index `idx_user_sessions_active`.
   - Xác nhận bảng `permissions` có 2 quyền `SESSION_READ`, `SESSION_TERMINATE` và đã gán cho `ROLE_ADMIN`.
2. **Deploy Backend Build:** Triển khai bản build Backend mới chứa các lớp sửa đổi.
3. **Smoke Test sau Deploy:**
   - Dùng tài khoản Bác sĩ gọi `GET /admin/sessions` -> xác nhận nhận 403 Forbidden và bảng `audit_logs` có bản ghi từ chối.
   - Dùng tài khoản Admin gọi `GET /admin/sessions` -> xác nhận xem được danh sách phiên.
   - Đăng nhập thử bằng tài khoản bệnh nhân -> kiểm tra DB có IP và User Agent.

### 5.2. Lệnh Kiểm thử Cần chạy
```bash
# 1. Biên dịch và kiểm tra cú pháp
mvn test-compile

# 2. Chạy toàn bộ test liên quan đến Auth, Session và ClinicConfig
mvn test -Dtest="*Session*,*Auth*,*ClinicConfig*"

# 3. Chạy test riêng cho SessionManagementController và Security
mvn test -Dtest="SessionManagementControllerSecurityTest,JwtAuthenticationFilterTest,UserSessionTest,ClinicConfigurationTest"
```

### 5.3. Tiêu chí chuyển trạng thái Finding sang “FIXED”
- **Finding-01:** Test case `doctorAccessingSessionsIsDeniedAndAudited` pass 100%, có bản ghi trong bảng `audit_logs`.
- **Finding-02:** Test case `expiredSessionCanCallLogoutAndRefreshWithoutFilterRejection` pass 100%.
- **Finding-03:** Test case `extendExpiredSessionReturnsBadRequest` trả về status 400 (`VALIDATION_FAILED`), pass 100%.
- **Finding-04:** Test case `patientLoginSavesIpAndUserAgent` pass 100%, trường IP/UserAgent trong DB khác null.
- **Finding-05:** Toàn bộ các test suite mới được viết và đạt tỷ lệ pass 100%.
- **Finding-06:** Signature của repository dùng `ClockPort` / `now`, mã nguồn được commit sạch sẽ.
