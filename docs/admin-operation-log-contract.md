# Nhật ký thao tác quản trị — Backend API Contract

> NCL-09-CN-006 (QTN-31) — Administrative Operation Log. Backend only.
> FE teammate consumes this contract to build the UI (CV-03).

## 1. Overview

The administrative operation log is a **read-only projection** over the existing
`audit_logs` table. No new table is introduced. Every relevant administrative
mutation records a single audit entry with `before`/`after` values, the
authenticated actor and a timestamp, all committed atomically with the mutation.

Administrative scope is a whitelist of (resource type, action) combinations:

| Resource type | Administrative actions |
|---|---|
| `USER` | `CREATE`, `UPDATE`, `ACTIVATE`, `DEACTIVATE`, `UNLOCK` |
| `ROLE` | `UPDATE` |
| `MEDICINE` | `CREATE`, `UPDATE`, `ACTIVATE`, `DEACTIVATE` |
| `SERVICE_CATALOG` | `CREATE`, `UPDATE`, `ACTIVATE`, `DEACTIVATE` |
| `SERVICE_PRICE` | `CREATE` |

> **SERVICE_PRICE is append-only.** Service prices are versioned by effective
> date: introducing a new price version always appends a new `SERVICE_PRICE`
> record rather than mutating an existing one. For that reason a price change is
> audited as `CREATE` (a new version), never as `UPDATE`.

Records that are **not** administrative configuration operations are excluded
from this view even when they share a resource type, e.g. `ACCESS_DENIED`
(`PERMISSION`), automatic login lockout (`LOCK` on `USER`), `CHANGE_PASSWORD`
and `RESET_PASSWORD` (`USER`). Login activity (`LOGIN`, `LOGOUT`,
`LOGIN_FAILED`) is also excluded.

## 2. Endpoint

| Item | Value |
|---|---|
| Method | `GET` |
| Path | `/admin-operation-logs` |
| Permission | `ADMIN_OPERATION_LOG_READ` (granted to `ADMIN`, `MANAGER`) |
| Auth | Bearer JWT (authenticated) |

## 3. Query parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `actorId` | UUID | no | Filter by the actor (user) who performed the operation. |
| `resourceType` | enum | no | Filter by object/resource type. One of `USER`, `ROLE`, `MEDICINE`, `SERVICE_CATALOG`, `SERVICE_PRICE`. |
| `from` | ISO-8601 instant | no | Inclusive lower bound on `createdAt`. |
| `to` | ISO-8601 instant | no | Exclusive upper bound on `createdAt`. |
| `page` | int | no (default `0`) | Page index (0-based). |
| `size` | int | no (default `20`) | Page size. |
| `sort` | string | no (default `createdAt,desc`) | Spring Data sort expression. |

> **Date-range semantics.** `from` is inclusive and `to` is exclusive. A request
> with `from == to` therefore matches nothing (an empty interval); `from` must
> not be after `to` (see §5).

Example:

```
GET /admin-operation-logs?actorId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1&resourceType=SERVICE_PRICE&page=0&size=20
```

## 4. Response

`200 OK` — a Spring Data `Page` of:

```json
{
  "content": [
    {
      "id": "…",
      "actorId": "…",
      "actorName": "System Administrator",
      "actionType": "CREATE",
      "resourceType": "SERVICE_PRICE",
      "resourceId": "…",
      "detail": "{\"before\":{\"price\":95000.00,\"effectiveFrom\":\"2026-01-01\"},\"after\":{\"price\":120000.00,\"effectiveFrom\":\"2026-09-01\"}}",
      "createdAt": "2026-09-17T03:30:00Z"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "size": 20,
  "number": 0
}
```

- `detail` is a JSON string of the form `{"before": {…}, "after": {…}}`.
  For CREATE operations `before` is `null`.
- `actorName` is the full name of the actor (may be `null` if the actor user no
  longer exists).

## 5. Errors

| Status | Code | Condition |
|---|---|---|
| `401` | `AUTHENTICATION_FAILED` | Missing/invalid token. |
| `403` | `ACCESS_DENIED` | Authenticated user lacks `ADMIN_OPERATION_LOG_READ` (e.g. DOCTOR). The denial is itself recorded as an `ACCESS_DENIED` audit entry. |
| `400` | `VALIDATION_ERROR` | Malformed query parameter (e.g. invalid UUID/enum/instant) or an inverted date range (`from` after `to` → `"from must be before or equal to to."`). |

## 6. Authorization behaviour

- `ADMIN` and `MANAGER` (clinic manager) can read the log.
- `DOCTOR` (and any role without `ADMIN_OPERATION_LOG_READ`) receives `403`.
- The denied access is logged by the existing `@RequirePermission` aspect
  (`actionType = ACCESS_DENIED`, `resourceType = PERMISSION`), with no recursion.

## 7. Data integrity / security

- Actor identity is taken from the authenticated security context
  (`CurrentUserPort`), never from the request body.
- There are no create/update/delete endpoints; the log is immutable (TC-04).
- Mutation + audit record share one transaction: if the audit record cannot be
  written, the administrative mutation is rolled back (QTN-31).
- Sensitive data (password hashes, tokens) are never stored in `detail`.

## 8. Permission seed

`V67__add_admin_operation_log_read_permission.sql` seeds
`ADMIN_OPERATION_LOG_READ` and grants it to `ADMIN` and `MANAGER`.
