# Session Management & Automatic Logout — NCL-01-CN-007 (Backend)

## 1. Business rules directly supported by the Excel workbook

| Excel requirement | Backend behavior |
|---|---|
| Inactivity timeout (QTN-45) | A `UserSession` becomes inactive when `now > last_used_at + session_timeout_minutes`. Enforced on every authenticated request and on refresh. |
| Configurable by the clinic (TC-01: 30 minutes) | `clinic_configuration.session_timeout_minutes` (default `30`, bounds `1..1440`), editable via `PUT /system/clinic` (`CLINIC_CONFIGURATION_UPDATE`). |
| Warning before expiration (TC-02) | `clinic_configuration.session_warning_minutes` (default `5`); `GET /sessions/current` returns `expiresAt` + `warningAt` so the frontend can count down. |
| Session extension (TC-02) | `POST /sessions/current/extend` resets `last_used_at` to now; does NOT extend `refresh_expires_at`. |
| Admin views open sessions (TC-03) | `GET /sessions` (`SESSION_READ`) lists non-revoked sessions with non-secret metadata. |
| Admin terminates a session (TC-03) | `DELETE /sessions/{id}` (`SESSION_TERMINATE`) revokes the target session atomically; next request → 401. |
| Role-based access (TC-04, QTN-01) | `SESSION_READ`/`SESSION_TERMINATE` seeded to `ADMIN` only; denial is audited by `RequirePermissionAspect` (`ACCESS_DENIED`). |
| Audit for denied access (QTN-01) | Already handled by `RequirePermissionAspect`; termination is audited as `SESSION_TERMINATE`, extension as `SESSION_EXTEND`. |

## 2. Technical interpretation / implementation decisions

- **Session identity** = the existing `user_sessions` row (`UserSession`), already used by login/refresh/logout. No new table.
- **Two time boundaries**:
  - *Inactivity deadline* = `last_used_at + session_timeout_minutes` (configurable, resettable).
  - *Absolute lifetime* = `refresh_expires_at` (fixed 7 days, unchanged) — extension/activity never lengthens it.
- **Activity tracking** = any authenticated request updates `last_used_at` in `JwtAuthenticationFilter`, throttled to at most one write per `min(60s, timeout/2)` so the throttle can never make an actively-used session look idle (the staleness window is always strictly smaller than the timeout).
- **Extension** = `ExtendSessionService` verifies the current session (id + user id from the security context, `isActive`) then issues an atomic `touchLastUsed`. It rejects revoked/expired sessions.
- **Remote termination** = `TerminateSessionService` resolves the actor from the security context and performs an atomic conditional `UPDATE ... SET revoked_at = now WHERE id = :id AND revoked_at IS NULL`. Because `JwtAuthenticationFilter` re-reads the session and checks `isActive()` on every request, a terminated session stops authorizing protected operations immediately, without frontend cooperation.
- **Refresh interaction** = `RefreshTokenService` now applies the configurable inactivity timeout in its `isActive` check; refresh-token rotation (with reuse detection) is unchanged, and a revoked/expired session cannot be resurrected via refresh.
- **Logout** = unchanged (`LogoutService.revoke`). Session expiration, remote termination and logout all converge on the same `revoked_at` / `isActive` machinery.

## 3. Explicit assumptions (NOT defined by the Excel workbook)

1. **Single active session per user is preserved as existing system behavior.** Login already calls `userSessionRepository.revokeByUserId(...)` before creating a new session. This is *existing* behavior, not a new NCL-01-CN-007 business rule, and is left untouched.
2. **No IP / device metadata** is stored or exposed (the workbook does not require it; login does not currently record it for staff sessions).
3. **Warning duration default (5 minutes)** is an implementation default, not an Excel rule. It is configurable via `session_warning_minutes`.
4. **Timeout bounds (1..1440 minutes)** are technical safety constraints (prevent disabling authentication via 0/negative), not Excel rules.
5. **No retention scheduler** is added. Expiry is enforced lazily; the existing (currently unwired) `deleteExpiredSessions()` remains available if a cleanup job is later wanted.

## 4. API surface

| Method | Path | Permission | Notes |
|---|---|---|---|
| GET | `/sessions/current` | authenticated | Current session status (incl. `expiresAt`, `warningAt`, `inactivityTimeoutSeconds`). |
| POST | `/sessions/current/extend` | authenticated | Extends the current session (resets inactivity), returns updated status. |
| GET | `/sessions` | `SESSION_READ` | Paginated list of open sessions (no secrets). |
| DELETE | `/sessions/{id}?reason=` | `SESSION_TERMINATE` | Remote termination; `204 No Content`; optional `reason` audited. |

Never exposed: passwords, raw access/refresh tokens, OTPs, or session secrets.
