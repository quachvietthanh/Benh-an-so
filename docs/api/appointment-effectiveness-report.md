# Báo cáo hiệu quả lịch hẹn (NCL-08-CN-008)

Backend API contract for the appointment effectiveness report.

## Endpoint

```
GET /reports/appointment-effectiveness?from={yyyy-MM-dd}&to={yyyy-MM-dd}[&doctorId={uuid}][&bookingChannel={ONLINE_PORTAL|RECEPTION_COUNTER}]
```

- Method: `GET`
- Authorization: `@RequirePermission("REPORT_VIEW")`
  - `REPORT_VIEW` is seeded to the `MANAGER` role only (V2 seed). Therefore:
    - `MANAGER` → 200
    - `ADMIN` → 403 (ADMIN is intentionally not granted `REPORT_VIEW`)
    - `DOCTOR`, `PHARMACIST`, `RECEPTIONIST` → 403
    - unauthenticated → 401
- Period semantics: half-open interval `[from 00:00, to+1d 00:00)` in clinic timezone
  `Asia/Ho_Chi_Minh`, keyed on `appointments.start_time` (the scheduled occurrence time, **not**
  `created_at`). A rescheduled appointment is counted once, at its current `start_time`.

## Query parameters

| Param | Required | Meaning |
|---|---|---|
| `from` | yes | `yyyy-MM-dd`, inclusive start date |
| `to` | yes | `yyyy-MM-dd`, inclusive end date |
| `doctorId` | no | UUID; restrict the population to one doctor |
| `bookingChannel` | no | `ONLINE_PORTAL` (portal bookings) or `RECEPTION_COUNTER` (at-counter/legacy bookings, stored as `NULL` `booking_channel`). Any other value → 400. |

Validation: `from`/`to` required (`yyyy-MM-dd`), `from <= to`, inclusive range ≤ 366 days.
Violations return `400` (`VALIDATION_ERROR` / `MISSING_PARAMETER`).

## Response (200 OK)

```json
{
  "from": "2026-08-01",
  "to": "2026-08-31",
  "generatedAt": "2026-08-31T08:00:00Z",
  "total": 100,
  "items": [
    { "status": "COMPLETED", "count": 40, "percentage": 40.00 },
    { "status": "CANCELLED", "count": 30, "percentage": 30.00 },
    { "status": "NO_SHOW", "count": 30, "percentage": 30.00 }
  ]
}
```

- `total`: total appointments in the (filtered) period.
- `items`: one entry per `AppointmentStatus` that has `count > 0`, ordered by the
  `AppointmentStatus` enum declaration order
  (`SCHEDULED, CONFIRMED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW`).
- `percentage`: `count × 100 / total`, `BigDecimal` scale 2, `RoundingMode.HALF_UP`.
  Percentages across items are consistent with `total` (sum ≈ 100.00 modulo rounding).

## Status values (authoritative)

`SCHEDULED`, `CONFIRMED`, `CHECKED_IN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`
(enum `com.benhsoan.domain.appointment.enums.AppointmentStatus`).

## Empty period (NCL-08-CN-008-TC-03)

When there are no appointments in the period (or for the applied filters):

```json
{ "from": "2026-08-01", "to": "2026-08-31", "generatedAt": "...", "total": 0, "items": [] }
```

The frontend should render the "no data" state when `total == 0` (or `items` is empty).

## Filters

- `doctorId` and `bookingChannel` narrow the report population. Percentages are always
  relative to the filtered population's `total`.
- To "split by booking channel" (TC-02), call the endpoint once with
  `bookingChannel=ONLINE_PORTAL` and once with `bookingChannel=RECEPTION_COUNTER`
  (or omit the param for the overall view).

## Error responses

| Case | HTTP | code | message |
|---|---|---|---|
| Missing `from`/`to` | 400 | `MISSING_PARAMETER` | `from is required.` / `to is required.` |
| Invalid date format | 400 | `VALIDATION_ERROR` | `from must be in yyyy-MM-dd format.` |
| `from > to` | 400 | `VALIDATION_ERROR` | `from must be before or equal to to.` |
| Range > 366 days | 400 | `VALIDATION_ERROR` | `Date range must not exceed 366 days.` |
| Invalid `bookingChannel` | 400 | `VALIDATION_ERROR` | `bookingChannel must be one of: ONLINE_PORTAL, RECEPTION_COUNTER.` |
| Unauthenticated | 401 | `AUTHENTICATION_FAILED` | — |
| No `REPORT_VIEW` | 403 | `ACCESS_DENIED` | — |

## Example request

```
GET /reports/appointment-effectiveness?from=2026-08-01&to=2026-08-31
GET /reports/appointment-effectiveness?from=2026-08-01&to=2026-08-31&bookingChannel=ONLINE_PORTAL
GET /reports/appointment-effectiveness?from=2026-08-01&to=2026-08-31&doctorId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2
```
